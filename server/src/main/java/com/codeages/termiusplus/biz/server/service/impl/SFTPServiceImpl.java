package com.codeages.termiusplus.biz.server.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.lang.func.VoidFunc1;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.server.annotation.SftpActive;
import com.codeages.termiusplus.biz.server.context.ServerContext;
import com.codeages.termiusplus.biz.server.dto.SFTPBean;
import com.codeages.termiusplus.biz.server.dto.SFTPServerUploadServerParams;
import com.codeages.termiusplus.biz.server.service.SFTPService;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.util.FileSizeFormatter;
import com.codeages.termiusplus.exception.AppException;
import com.codeages.termiusplus.ws.ssh.AuthKeyBoardHandler;
import com.codeages.termiusplus.ws.ssh.EventType;
import com.codeages.termiusplus.ws.ssh.MessageDto;
import com.codeages.termiusplus.ws.ssh.SftpServerUploadServerProgressDto;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.codeages.termiusplus.biz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.sftp.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
public class SFTPServiceImpl implements SFTPService {

    private final ServerService serverService;

    private final HttpServletResponse response;

    public SFTPServiceImpl(ServerService serverService, HttpServletResponse response) {
        this.serverService = serverService;
        this.response = response;
    }

    @SneakyThrows
    @Override
    public String init(String sessionId, Long serverId) {
        StatefulSFTPClient sftp = createSftp(sessionId, serverId);
        String id = UUID.fastUUID() + "-" + serverId + "-" + sessionId;
        ServerContext.SFTP_POOL.put(id, new SFTPBean(sftp, System.currentTimeMillis(), false));

        return id;
    }

    @SneakyThrows
    private StatefulSFTPClient createSftp(String sessionId, Long serverId) {
        SSHClient sshClient = serverService.createSSHClient(serverId, sessionId);

        sshClient.useCompression();
        return sshClient.newStatefulSFTPClient();
    }

    @Override
    public SFTPBean getSftpBean(String id) {
        SFTPBean sftp = ServerContext.SFTP_POOL.get(id);
        if (sftp == null) {
            log.info("SFTP连接已失效：{}", id);
            String[] split = id.split("-");
            sftp = new SFTPBean(
                    createSftp(split[split.length - 1], Long.valueOf(split[split.length - 2])),
                    System.currentTimeMillis(),
                    false
            );

            ServerContext.SFTP_POOL.put(id, sftp);
        }

        sftp.setTime(System.currentTimeMillis());

        return sftp;
    }

    private StatefulSFTPClient getSftp(String id) {
        return getSftpBean(id).getSftp();
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
    @SftpActive("#id")
    public void upload(String id, MultipartFile file, String remotePath) {
        File tmpFile = createTmpFile();
        file.transferTo(tmpFile);
        getSftp(id).put(tmpFile.getPath(), remotePath + File.separator + file.getOriginalFilename());
    }

    @Override
    @SneakyThrows
    @SftpActive("#id")
    public void uploadFile(String id, MultipartFile file, String remoteFilePath) {
        File tmpFile = createTmpFile();
        file.transferTo(tmpFile);
        getSftp(id).put(tmpFile.getPath(), remoteFilePath);
    }

    @Override
    @SftpActive("#id")
    public void download(String id, String remotePath) throws IOException {
        log.info("下载文件：{}", remotePath);
        String filename = remotePath.substring(remotePath.lastIndexOf("/") + 1);
        ServletOutputStream outputStream = response.getOutputStream();

        SFTPClient sftp = getSftp(id);
        RemoteFile readFile;
        try {
            readFile = sftp.open(remotePath);
        } catch (ConnectionException e) {
            String[] split = id.split("-");
            SFTPBean s = new SFTPBean(
                    createSftp(split[split.length - 1], Long.valueOf(split[split.length - 2])),
                    System.currentTimeMillis(),
                    false
            );

            ServerContext.SFTP_POOL.put(id, s);

            download(id, remotePath);
            return;
        }

        com.codeages.termiusplus.biz.storage.utils.FileUtil.initResponse(
                readFile.length(),
                filename,
                response,
                false
        );

        RemoteFile.ReadAheadRemoteFileInputStream readAheadRemoteFileInputStream = readFile.new ReadAheadRemoteFileInputStream(
                15);

        BufferedInputStream inputStream = new BufferedInputStream(
                readAheadRemoteFileInputStream,
                FileSizeFormatter.ONE_MB
        );
        inputStream.transferTo(outputStream);

        IoUtil.close(inputStream);
        IoUtil.close(outputStream);
    }

    @Override
    @SneakyThrows
    public long size(String id, String remotePath) {
        SFTPClient sftp = getSftp(id);
        @Cleanup RemoteFile readFile = sftp.open(remotePath);
        return readFile.length();
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
        SFTPBean sftp = ServerContext.SFTP_POOL.get(id);
        if (sftp == null) {
            return;
        }

        sftp.getSftp().close();
        ServerContext.SFTP_POOL.remove(id);
    }

    public void asyncServerUploadServer(SFTPServerUploadServerParams params) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> serverUploadServer(params));

        future.exceptionally(ex -> {
            //todo 使用websocket通知前端
            return null; // 返回一个默认值
        });
    }

    @Override
    @SneakyThrows
    public byte[] readFile(String id, String remotePath) {
        SFTPClient sftp = getSftp(id);
        try (RemoteFile readFile = sftp.open(remotePath)) {
            byte[] bytes = new byte[(int) readFile.length()];
            readFile.read(0, bytes, 0, bytes.length);
            return bytes;
        }
    }

    @Override
    @SneakyThrows
    public void writeFile(String id, String remotePath, String content) {
        SFTPClient sftp = getSftp(id);
        try (RemoteFile remoteFile = sftp.open(remotePath, EnumSet.of(OpenMode.WRITE))) {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            remoteFile.write(0, bytes, 0, bytes.length);
        } catch (ConnectionException e) {
            String[] split = id.split("-");
            SFTPBean s = new SFTPBean(
                    createSftp(split[split.length - 1], Long.valueOf(split[split.length - 2])),
                    System.currentTimeMillis(),
                    false
            );

            ServerContext.SFTP_POOL.put(id, s);

            writeFile(id, remotePath, content);
        }

    }

    @Override
    public void serverUploadServer(SFTPServerUploadServerParams params) {
        try {
            SFTPClient sourceSftp = getSftp(params.getSourceId());
            String source = params.getSourcePath() + File.separator + params.getFileName();
            RemoteFile sourceFile = sourceSftp.open(source);

            SFTPClient targetSftp = getSftp(params.getTargetId());


            String target = params.getTargetPath() + File.separator + params.getFileName();

            try {
                targetSftp.lstat(target);
                throw new AppException(ErrorCode.INTERNAL_ERROR, "文件已存在");
            } catch (IOException e) {
                File tmpFile = createTmpFile();
                targetSftp.put(tmpFile.getPath(), target);
            }

            RemoteFile remoteFile = targetSftp.open(target, EnumSet.of(OpenMode.WRITE));
            RemoteFile.RemoteFileOutputStream remoteFileOutputStream = remoteFile.new RemoteFileOutputStream();
            RemoteFile.ReadAheadRemoteFileInputStream readAheadRemoteFileInputStream = sourceFile.new ReadAheadRemoteFileInputStream(
                    15);

            transferWithProgress(readAheadRemoteFileInputStream, remoteFileOutputStream, progress -> {
                MessageDto messageDto = new MessageDto(EventType.SFTP_SERVER_UPLOAD_SERVER_PROGRESS, JSONUtil.toJsonStr(new SftpServerUploadServerProgressDto(
                        params.getSourceServerName(),
                        source,
                        params.getTargetServerName(),
                        target,
                        progress,
                        params.getSourceId(),
                        params.getTargetId()
                )));
                AuthKeyBoardHandler.sendMessage(params.getClientSessionId(), JSONUtil.toJsonStr(messageDto));
            });

            IoUtil.close(readAheadRemoteFileInputStream);
            IoUtil.close(remoteFileOutputStream);
        } catch (Exception e) {
            log.error("服务器对服务器传递文件错误", e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @SneakyThrows
    public static void transferWithProgress(InputStream inputStream,
                                            OutputStream outputStream,
                                            VoidFunc1<Long> progressCallback) {
        Objects.requireNonNull(outputStream, "out");
        long transferred = 0;
        byte[] buffer = new byte[FileSizeFormatter.ONE_MB];
        int read;
        while ((read = inputStream.read(buffer, 0, FileSizeFormatter.ONE_MB)) >= 0) {
            outputStream.write(buffer, 0, read);
            transferred += read;
            if (progressCallback != null) {
                progressCallback.call(transferred);
            }
        }
    }

}
