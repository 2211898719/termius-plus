package com.codeages.javaskeletonserver.biz.server.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.UUID;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.server.context.SFTPContext;
import com.codeages.javaskeletonserver.biz.server.dto.SFTPBean;
import com.codeages.javaskeletonserver.biz.server.service.SFTPService;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.exception.AppException;
import com.github.jaemon.dinger.DingerSender;
import com.github.jaemon.dinger.core.entity.DingerRequest;
import com.github.jaemon.dinger.core.entity.enums.MessageSubType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.StatefulSFTPClient;
import org.mvel2.MVEL;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class SFTPServiceImpl implements SFTPService {

    private final ServerService serverService;

    private final DingerSender dingerSender;

    public SFTPServiceImpl(ServerService serverService, DingerSender dingerSender) {
        this.serverService = serverService;
        this.dingerSender = dingerSender;
    }

    @SneakyThrows
    @Override
    public String init(Long serverId) {
        StatefulSFTPClient sftp = createSftp(serverId);
        String id = UUID.fastUUID() + "-" + serverId;
        SFTPContext.SFTP_POOL.put(id, new SFTPBean(sftp, System.currentTimeMillis()));

        return id;
    }

    @SneakyThrows
    private StatefulSFTPClient createSftp(Long serverId) {
        SSHClient sshClient = serverService.createSSHClient(serverId);

        sshClient.useCompression();
        return sshClient.newStatefulSFTPClient();
    }


    private StatefulSFTPClient getSftp(String id) {
        SFTPBean sftp = SFTPContext.SFTP_POOL.get(id);
        if (sftp == null) {
            log.info("SFTP连接已失效：{}", id);
            String[] split = id.split("-");
            sftp = new SFTPBean(createSftp(Long.valueOf(split[split.length - 1])), System.currentTimeMillis());
        }

        sftp.setTime(System.currentTimeMillis());

        return sftp.getSftp();
    }

    @Override
    @SneakyThrows
    public String pwd(String id) {
        return getSftp(id).pwd();
    }

    @Override
    @SneakyThrows
    public List<RemoteResourceInfo> ls(String id, String path) {
        try {
            return getSftp(id).ls(path);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @SneakyThrows
    @Override
    public void mkdir(String id, String path) {
        getSftp(id).mkdir(path);
    }

    @Override
    @SneakyThrows
    public void rm(String id, String path) {
        getSftp(id).rm(path);
    }

    @Override
    @SneakyThrows
    public void rmDir(String id, String path) {
        getSftp(id).rmdir(path);
    }

    @Override
    @SneakyThrows
    public void rename(String id, String oldPath, String newPath) {
        try {
            getSftp(id).rename(oldPath, newPath);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "文件名已存在");
        }
    }

    @Override
    @SneakyThrows
    public void upload(String id, MultipartFile file, String remotePath) {
        File tmpFile = createTmpFile();
        file.transferTo(tmpFile);
        getSftp(id).put(tmpFile.getPath(), remotePath + File.separator + file.getOriginalFilename());
    }

    @Override
    public void download(String id, String remotePath, HttpServletResponse response) throws IOException {
        String filename = remotePath.substring(remotePath.lastIndexOf("/") + 1);
        ServletOutputStream outputStream = response.getOutputStream();

        SFTPClient sftp = getSftp(id);
        RemoteFile readFile = sftp.open(remotePath);
        com.codeages.javaskeletonserver.biz.storage.utils.FileUtil.initResponse(
                readFile.length(),
                filename,
                response
        );

        RemoteFile.ReadAheadRemoteFileInputStream readAheadRemoteFileInputStream = readFile.new ReadAheadRemoteFileInputStream(
                15);

        BufferedInputStream inputStream = new BufferedInputStream(readAheadRemoteFileInputStream, 1024 * 1024);
        inputStream.transferTo(outputStream);

        IoUtil.close(inputStream);
        IoUtil.close(outputStream);
    }

    @Override
    @SneakyThrows
    public long size(String id, String remotePath) {
        try (SFTPClient sftp = getSftp(id)) {
            try (RemoteFile readFile = sftp.open(remotePath)) {
                return readFile.length();
            }
        }
    }

    private static File createTmpFile() {
        File tmpDir = FileUtil.getTmpDir();
        String tmpFilePath = tmpDir.getPath() + File.separator + UUID.fastUUID();
        if (FileUtil.exist(tmpFilePath)) {
            FileUtil.del(tmpFilePath);
        }

        File tmpFile = FileUtil.touch(tmpFilePath);
        return tmpFile;
    }

    @Override
    @SneakyThrows
    public void close(String id) {
        SFTPBean sftp = SFTPContext.SFTP_POOL.get(id);
        if (sftp == null) {
            return;
        }

        sftp.getSftp().close();
        SFTPContext.SFTP_POOL.remove(id);
    }
}
