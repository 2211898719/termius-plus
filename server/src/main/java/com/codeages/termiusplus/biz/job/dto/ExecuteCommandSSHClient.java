package com.codeages.termiusplus.biz.job.dto;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.codeages.termiusplus.biz.util.command.CpuUsage;
import com.codeages.termiusplus.biz.util.command.DiskUsage;
import com.codeages.termiusplus.biz.util.command.NetworkUsage;

import lombok.SneakyThrows;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ExecuteCommandSSHClient {
    private final SSHClient sshClient;
    private final Session session;

    @SneakyThrows
    public ExecuteCommandSSHClient(SSHClient sshClient) {
        this.sshClient = sshClient;
        session = sshClient.startSession();
    }

    @SneakyThrows
    public String executeCommand(String command) {
        Session.Command cmd = sshClient.startSession().exec(command);

        InputStream in = cmd.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString().trim();
    }

    @SneakyThrows
    public List<NetworkUsage> calculateNetworkSpeed(int intervalSeconds) {
        Map<String, long[]> initialData = getNetworkBytes();
        ThreadUtil.sleep(intervalSeconds, TimeUnit.SECONDS);
        Map<String, long[]> finalData = getNetworkBytes();
        Set<String> ignoredIface = Set.of("lo", "br", "veth", "docker", "ztnv", "utun");
        List<NetworkUsage> networkUsages = new ArrayList<>();

        for (String iface : initialData.keySet()) {
            if (ignoredIface.stream().anyMatch(iface::startsWith)) {
                continue;
            }

            long initialRx = initialData.get(iface)[0];
            long initialTx = initialData.get(iface)[1];
            long finalRx = finalData.get(iface)[0];
            long finalTx = finalData.get(iface)[1];

            long rxBytes = finalRx - initialRx;
            long txBytes = finalTx - initialTx;

            double rxSpeed = (double) rxBytes / intervalSeconds; // bytes/s
            double txSpeed = (double) txBytes / intervalSeconds; // bytes/s

            String rxReadable = FileUtil.readableFileSize((long) rxSpeed);
            String txReadable = FileUtil.readableFileSize((long) txSpeed);
            System.out.println(iface + ": rx: " + rxReadable + "/s, tx: " + txReadable + "/s");

            networkUsages.add(new NetworkUsage(iface, rxSpeed, txSpeed));
        }

        return networkUsages;
    }

    public CpuUsage getCpuUsage() {
        return parseCpuUsage(executeCommand("top -b -n1 | grep 'Cpu(s)'"));
    }

    public List<DiskUsage> getDiskUsage() {
        return parseDiskUsage(executeCommand("df -h"));
    }

    private Map<String, long[]> getNetworkBytes() {
        return parseNetworkBytes(executeCommand("ip -s link"));
    }

    private Map<String, long[]> parseNetworkBytes(String output) {
        Map<String, long[]> networkStats = new HashMap<>();
        String currentInterface = null;
        long rxBytes = 0;
        long txBytes = 0;

        String[] lines = output.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // 检查接口行
            if (line.matches("^\\d+: \\S+:.*")) {
                if (currentInterface != null) {
                    // 存储上一个接口的数据
                    networkStats.put(currentInterface, new long[]{rxBytes, txBytes});
                }
                currentInterface = line.split(":")[1].trim(); // 获取当前接口名称
                rxBytes = 0; // 重置接收字节数
                txBytes = 0; // 重置发送字节数
            }

            // 检查接收字节行
            if (line.startsWith("RX: bytes")) {
                if (i + 1 < lines.length) {
                    String[] parts = lines[i + 1].trim().split("\\s+");
                    rxBytes = Long.parseLong(parts[0]); // 获取接收字节数
                }
            }

            // 检查发送字节行
            if (line.startsWith("TX: bytes")) {
                if (i + 1 < lines.length) {
                    String[] parts = lines[i + 1].trim().split("\\s+");
                    txBytes = Long.parseLong(parts[0]); // 获取发送字节数
                }
            }
        }

        // 存储最后一个接口的数据
        if (currentInterface != null) {
            networkStats.put(currentInterface, new long[]{rxBytes, txBytes});
        }

        return networkStats;
    }


    private static CpuUsage parseCpuUsage(String cpuOutput) {
        // 解析 CPU 使用情况
        String[] parts = cpuOutput.trim().split(":")[1].split(",");
        CpuUsage cpuUsage = new CpuUsage();
        //正则匹配 空格 +.*+空格us
        for (String part : parts) {
            part = part.trim();
            String[] split = part.split(" ");
            switch (split[1]) {
                case "us":
                    cpuUsage.setUs(Double.parseDouble(split[0].trim()));
                    break;
                case "sy":
                    cpuUsage.setSy(Double.parseDouble(split[0].trim()));
                    break;
                case "id":
                    cpuUsage.setId(Double.parseDouble(split[0].trim()));
                    break;
                default:
                    break;
            }
        }

        return cpuUsage;
    }


    private static List<DiskUsage> parseDiskUsage(String diskOutput) {
        String[] lines = diskOutput.split("\n");
        Set<String> ignoredFileSystems = Set.of("overlay", "tmpfs", "devtmpfs", "squashfs", "/dev/loop", "udev", "shm", "none");
        Set<String> ignoredMountedOns = Set.of("/boot");
        List<DiskUsage> diskUsages = new ArrayList<>();
        // 跳过表头
        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].trim().split("\\s+");
            if (parts.length >= 6) { // 确保有足够的列
                String fileSystem = parts[0];
                String mountPoint = parts[5];
                // 只处理根目录或挂载在根目录下的文件系统，并且不在忽略列表中
                if (ignoredFileSystems.stream().anyMatch(fileSystem::startsWith)) {
                    continue;
                }

                if (ignoredMountedOns.stream().anyMatch(mountPoint::startsWith)) {
                    continue;
                }

                DiskUsage diskUsage = new DiskUsage();
                diskUsage.setFilesystem(fileSystem);
                diskUsage.setSize(parts[1]);
                diskUsage.setUsed(parts[2]);
                diskUsage.setAvailable(parts[3]);
                diskUsage.setUse(parts[4]);
                diskUsage.setMountedOn(mountPoint);
                diskUsages.add(diskUsage);
            }
        }

        return diskUsages;
    }

}