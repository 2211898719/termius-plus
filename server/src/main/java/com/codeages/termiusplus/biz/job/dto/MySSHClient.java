package com.codeages.termiusplus.biz.job.dto;

import cn.hutool.core.io.FileUtil;
import com.codeages.termiusplus.biz.sshj.SSHClient;
import lombok.AllArgsConstructor;

import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class MySSHClient {
    private final SSHClient sshClient ;

    public String executeCommand(String command) throws Exception {
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

    public static void main(String[] args) {
        String s = "              total        used        free      shared  buff/cache   available\n" +
                "Mem:            62G         29G         22G        214M         11G         32G\n" +
                "Swap:            0B          0B          0B\n";

        parseFreeOutput(s);

        String p ="%Cpu(s):  1.3 us,  0.5 sy,  0.0 ni, 98.0 id,  0.1 wa,  0.0 hi,  0.1 si,  0.0 st";

        parseCpuUsage(p);

        String d = "Filesystem      Size  Used Avail Use% Mounted on\n" +
                "udev             32G     0   32G   0% /dev\n" +
                "tmpfs           6.3G  4.8M  6.3G   1% /run\n" +
                "/dev/sda2       439G  300G  117G  72% /\n" +
                "tmpfs            32G     0   32G   0% /dev/shm\n" +
                "tmpfs           5.0M  4.0K  5.0M   1% /run/lock\n" +
                "tmpfs            32G     0   32G   0% /sys/fs/cgroup\n" +
                "/dev/loop2      128K  128K     0 100% /snap/bare/5\n" +
                "/dev/loop0      2.5M  2.5M     0 100% /snap/gnome-system-monitor/163\n" +
                "/dev/loop4      2.3M  2.3M     0 100% /snap/gnome-calculator/955\n" +
                "/dev/loop3       64M   64M     0 100% /snap/core20/2318\n" +
                "/dev/loop5      896K  896K     0 100% /snap/gnome-logs/123\n" +
                "/dev/loop6      506M  506M     0 100% /snap/gnome-42-2204/176\n" +
                "/dev/sda1       511M  5.3M  506M   2% /boot/efi\n" +
                "/dev/loop7      1.7M  1.7M     0 100% /snap/gnome-system-monitor/186\n" +
                "/dev/loop8       92M   92M     0 100% /snap/gtk-common-themes/1535\n" +
                "/dev/loop9       62M   62M     0 100% /snap/core20/1081\n" +
                "/dev/loop10     350M  350M     0 100% /snap/gnome-3-38-2004/143\n" +
                "/dev/loop11     242M  242M     0 100% /snap/gnome-3-38-2004/70\n" +
                "/dev/loop12     640K  640K     0 100% /snap/gnome-logs/106\n" +
                "/dev/loop13     219M  219M     0 100% /snap/gnome-3-34-1804/93\n" +
                "/dev/loop14      56M   56M     0 100% /snap/core18/2829\n" +
                "/dev/loop16     768K  768K     0 100% /snap/gnome-characters/726\n" +
                "/dev/loop15     219M  219M     0 100% /snap/gnome-3-34-1804/72\n" +
                "/dev/loop17      56M   56M     0 100% /snap/core18/2128\n" +
                "/dev/loop18      39M   39M     0 100% /snap/snapd/21759\n" +
                "/dev/loop19     2.5M  2.5M     0 100% /snap/gnome-calculator/884\n" +
                "/dev/loop20     640K  640K     0 100% /snap/gnome-characters/797\n" +
                "/dev/loop21      66M   66M     0 100% /snap/gtk-common-themes/1515\n" +
                "tmpfs           6.3G   16K  6.3G   1% /run/user/121\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/43136827a8ebfe6a4b3fb963c29cb237822faaff29f2914c232b0b1c742f8ce4/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/2b57399e18f735a468a6db78ab788da83131739d82843253acbde8179a4e55d1/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/2395f8830b681ecd441cd141901a51483544ffe558a0aacb8126aa83daadc932/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/c69e1f5319eccaeb252b1ac73b74f2d6bc28414ded5d14369e09da24464dcfe4/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/d55c6aa8208b578bfbce2a150961bb7a465fc81162f6f16a13c7e811b146da11/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/50a08420a6b7a38f823e2d9b69b7dc7be57f3126482b65966600cc47e043b9c7/merged\n" +
                "tmpfs           6.3G     0  6.3G   0% /run/user/1000\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/426c36186e64cf9e21099ed69b216924ab684abd9d3ba4c098cf6de7c96394ae/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/d537c9cecc9003c9a46a8f84757f234f3a3c35f9a3943f15f2adca7516611aae/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/5174014ba508f3db4d58cdb1ac4b43e6e19c2c967c60917c9d2a7008bfcb5a3d/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/f5093de1940d5b1ee956eeed9f9e397bc53956477d7a1fce0b4108992c86d317/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/e1663c569bd2ccf53e35bf2b481e757e6c72858ab2dafb2e39a0a977bed7eca2/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/3bbf9703bb8f5c6b94a7e409ffba6b75326ad33cd6ba4e6f5fac28a7ca39cadb/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/8664e4a8a0e77229a503cb2698ee4205826597c31945a07567c6ef5dda4fdd6d/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/b423f93cfd27b21a1b3ed7d623824375a8b4bb1ac40cbab726ddeef2a9d58ec4/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/feaff2bd36354de6b2e27c542ae737350fde20a5c454f933c36770edbc94fa79/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/6bcb84389be0fe063fd11748b9ebcc74cf623d6f1a58f64c5747ecb9941575dd/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/01c2e49af05543e6c0dada5e883ca944da7d840f288f0f863d5979b4230aec4a/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/2027170281ab28c3f89ed3da1290a72d822541fb492229bdeb2c8cfc18f19126/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/aa290d6e45a6e79eeb6699601496144b55e28654e30fc94c6b8557f6b921940b/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/8e07a941e1db398baf6cb2e1fdd531a3a1440c9641935a042d082dbbc5550ddb/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/cfbce0909ee27ca224ec92f38561f05c2e0a9f436c70190c2c64497edef73c67/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/2111cb4814ffa6bf4a4d18943bee3a40b74597b3660ab8d875af73b4f41723b7/merged\n" +
                "/dev/loop1       75M   75M     0 100% /snap/core22/1586\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/8bfed49de3c6b355eea34fcbc33cc223cd0ea9ec6d829249c3e9f32b544e4a00/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/8fff93c41777cc362ab5646e801855066ddf9997402ef25cd0ecc0d353ec6d24/merged\n" +
                "tmpfs           6.3G     0  6.3G   0% /run/user/0\n" +
                "/dev/loop22     4.3M  4.3M     0 100% /snap/asciiquarium/42\n" +
                "/dev/loop24      75M   75M     0 100% /snap/core22/1612\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/0083f2eb2a3c5452a21d3514d8387475051ee02bad6a95a8442a2db033e320d3/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/297c556c580e9f95821325c72f0ba2914d15d62075b253931051275240d2b9cc/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/f58881dce506b6479ad84bd32f86d2a20d2c088d93bb42a059a37d4588db5ebf/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/f0f32f5b7584d7bbc6e059e4035dd499318da841ae1d541e0d1d8685b9938b20/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/89df9a37fd140884218dbcf99220e29ca3bd2c97d7ceb5b7086d5c4f2f813042/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/ddca8ce7dc2d5c945562c2582f42e8b84bc0f61fc9d4f449af0d720f26e93a00/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/8384a71e516b93307163909f9800d1f3851bdf4d5daec0966a38ee8cd0b550c9/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/9318de39afeff3a594aa7baebdf12403bccdd719f586c9db7e9ddc67ad5c4288/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/690c26768eef71d3dc45445b6694be4e50759a609b80693f4a63a7686703b843/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/f48ccbcbc141b68b2efcd0b906497dcf0af50f52d05c9242c82f72095b5e14b1/merged\n" +
                "overlay         439G  300G  117G  72% /var/lib/docker/overlay2/f2ff613647bb5978bf3b90840ab871eab07f6c3f82be4363d45d3a5221174f9e/merged\n";

        parseDiskUsage(d);

        String network = "Average:        IFACE   rxpck/s   txpck/s    rxkB/s    txkB/s   rxcmp/s   txcmp/s  rxmcst/s   %ifutil\n" +
                "Average:    veth69d48ab      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    br-794a32146c5b      1.60      1.60      0.08      0.10      0.00      0.00      0.00      0.00\n" +
                "Average:    veth2bc492e      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    vethbb06485      1.80      1.80      0.37      0.12      0.00      0.00      0.00      0.00\n" +
                "Average:      docker0      1.60      0.80      0.20      0.29      0.00      0.00      0.00      0.00\n" +
                "Average:    vethd792e37      1.20      1.20      0.08      0.43      0.00      0.00      0.00      0.00\n" +
                "Average:    veth6b54ecf      0.20      0.20      0.01      0.01      0.00      0.00      0.00      0.00\n" +
                "Average:    veth8e0674a      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    br-e256f46186ca      0.60      0.60      0.03      0.04      0.00      0.00      0.00      0.00\n" +
                "Average:    br-00494c7c2bcd      4.60      6.40      0.80      0.64      0.00      0.00      0.00      0.00\n" +
                "Average:    veth835ea9f      1.20      1.00      0.17      0.06      0.00      0.00      0.00      0.00\n" +
                "Average:    vethce50941      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    veth6afa69a      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    veth95a9b5d      6.00      5.20      0.82      1.15      0.00      0.00      0.00      0.00\n" +
                "Average:    veth5b7506d      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    br-92537a38c3e2      1.20      0.60      0.07      0.04      0.00      0.00      0.00      0.00\n" +
                "Average:    veth8632be8      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    veth567ed7a      0.60      0.60      0.04      0.04      0.00      0.00      0.00      0.00\n" +
                "Average:    br-a00191a653ce      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    vethcc2ab65      1.80      3.60      0.69      0.46      0.00      0.00      0.00      0.00\n" +
                "Average:    br-c5a6a67a5a44      1.80      1.80      0.34      0.12      0.00      0.00      0.00      0.00\n" +
                "Average:    veth40d7fa3      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    veth0ad1ffa      0.60      0.60      0.04      0.04      0.00      0.00      0.00      0.00\n" +
                "Average:    br-f4b76945e070      1.20      1.00      0.16      0.06      0.00      0.00      0.00      0.00\n" +
                "Average:    vethd9c2723      3.60      1.80      0.46      0.69      0.00      0.00      0.00      0.00\n" +
                "Average:    br-aec60a59cd7c      6.80      7.60      1.16      0.92      0.00      0.00      0.00      0.00\n" +
                "Average:    br-826f22479a22      0.40      0.40      0.02      0.03      0.00      0.00      0.00      0.00\n" +
                "Average:    br-d549377b69f3      3.60      1.80      0.41      0.69      0.00      0.00      0.00      0.00\n" +
                "Average:    vethaead4fc      1.00      1.00      0.06      0.06      0.00      0.00      0.00      0.00\n" +
                "Average:    veth0ddba0b      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    veth348f9ec      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:       enp2s0     19.60     25.40      1.61     10.98      0.00      0.00      0.00      0.09\n" +
                "Average:    br-3cdb6ba1931c      1.20      1.20      0.06      0.43      0.00      0.00      0.00      0.00\n" +
                "Average:    veth6d28add      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    veth9e3ab5f      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    vethad7a15b      2.40      2.40      0.15      0.15      0.00      0.00      0.00      0.00\n" +
                "Average:    vethdc4ec70      0.80      0.80      0.05      0.05      0.00      0.00      0.00      0.00\n" +
                "Average:    veth2adc7fd      6.60      7.40      1.24      0.91      0.00      0.00      0.00      0.00\n" +
                "Average:    veth8d4a67e      0.40      0.40      0.03      0.03      0.00      0.00      0.00      0.00\n" +
                "Average:    vethb929bde      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    veth7e1172b      1.60      0.80      0.22      0.29      0.00      0.00      0.00      0.00\n" +
                "Average:           lo      2.00      2.00      0.10      0.10      0.00      0.00      0.00      0.00\n" +
                "Average:    vethc1cdacf      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    br-cbef70623b8a      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    br-fe62d718bb06      0.80      0.80      0.04      0.05      0.00      0.00      0.00      0.00\n" +
                "Average:    br-e00d881148aa      6.00      5.20      0.74      1.15      0.00      0.00      0.00      0.00\n" +
                "Average:    veth828f63d      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    veth67b9e5d      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    br-8d24e07062b0      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    veth9b45b13      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    br-e7f61f5ecb94      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    vethc77a046      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00\n" +
                "Average:    veth24d0163      0.40      0.40      0.03      0.03      0.00      0.00      0.00      0.00\n" +
                "Average:    veth32413d5      1.20      0.60      0.08      0.04      0.00      0.00      0.00      0.00\n";

        parseNetworkUsage(network);
    }

    public static void parseFreeOutput(String output) {
        String[] lines = output.split("\n");
        String[] memoryLine = lines[1].trim().split("\\s+");

        System.out.println("总内存 Memory: " + memoryLine[1]);
        System.out.println("使用内存 Memory: " + memoryLine[2]);
        System.out.println("可用内存 Memory: " + memoryLine[3]);
        System.out.println("可回收内存 Memory: " + memoryLine[6]);
    }

    public static void parseCpuUsage(String cpuOutput) {
        // 解析 CPU 使用情况
        String[] parts = cpuOutput.trim().split(",");
        for (String part : parts) {
            if (part.contains("us")) {
                System.out.println("用户 CPU Usage: " + part.trim());
            } else if (part.contains("sy")) {
                System.out.println("系统 CPU Usage: " + part.trim());
            } else if (part.contains("id")) {
                System.out.println("空闲 CPU Usage: " + part.trim());
            }
        }
    }



    public static void parseDiskUsage(String diskOutput) {
        String[] lines = diskOutput.split("\n");
        List<String> ignoredFileSystems = Arrays.asList("overlay", "tmpfs", "devtmpfs", "squashfs");

        // 跳过表头
        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].trim().split("\\s+");
            if (parts.length >= 6) { // 确保有足够的列
                String fileSystem = parts[0];
                String mountPoint = parts[5];

                // 只处理根目录或挂载在根目录下的文件系统，并且不在忽略列表中
                if (("/".equals(mountPoint))) {
                    System.out.println("Filesystem: " + parts[0]);
                    System.out.println("Size: " + parts[1]);
                    System.out.println("Used: " + parts[2]);
                    System.out.println("Available: " + parts[3]);
                    System.out.println("Use%: " + parts[4]);
                    System.out.println("Mounted on: " + parts[5]);
                    System.out.println("-------------------------");
                }
            }
        }
    }


    public static void parseNetworkUsage(String networkOutput) {
        String[] lines = networkOutput.split("\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();

            // 忽略包含特定关键词的行（虚拟网卡）
            if (!line.contains("docker") && !line.contains("veth") && !line.contains("br-")
                    && !line.contains("tap") && !line.contains("tun") && !line.contains("lo")
                    && !line.contains("ppp") && !line.contains("virbr") && !line.contains("vlan")) {

                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 5) { // 确保有足够的列
                    // 假设 RX 是下行，TX 是上行
                    long rxBytes = (long) (Double.parseDouble(parts[2]) * 1024); // 转换为字节
                    long txBytes = (long) (Double.parseDouble(parts[3]) * 1024); // 转换为字节

                    String rxReadable = FileUtil.readableFileSize(rxBytes);
                    String txReadable = FileUtil.readableFileSize(txBytes);

                    System.out.println("IFACE: " + parts[1] + "，上行: " + txReadable + ", 下行: " + rxReadable);
                }
            }
        }
    }


}
