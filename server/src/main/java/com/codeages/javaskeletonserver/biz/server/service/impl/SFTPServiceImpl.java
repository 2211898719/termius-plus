package com.codeages.javaskeletonserver.biz.server.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.extra.ssh.Sftp;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.server.context.SFTPContext;
import com.codeages.javaskeletonserver.biz.server.dto.LsFileDto;
import com.codeages.javaskeletonserver.biz.server.dto.ProxyDto;
import com.codeages.javaskeletonserver.biz.server.dto.SFTPBean;
import com.codeages.javaskeletonserver.biz.server.dto.ServerDto;
import com.codeages.javaskeletonserver.biz.server.enums.LsFileTypeEnum;
import com.codeages.javaskeletonserver.biz.server.service.SFTPService;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.exception.AppException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class SFTPServiceImpl implements SFTPService {

    private final ServerService serverService;


    public SFTPServiceImpl(ServerService serverService) {
        this.serverService = serverService;
    }

    @SneakyThrows
    @Override
    public String init(Long serverId) {
        ServerDto server = serverService.findById(serverId);

        Sftp sftp = createSftp(server);
        String id = UUID.fastUUID().toString();
        SFTPContext.SFTP_POOL.put(id, new SFTPBean(sftp, System.currentTimeMillis()));

        return id;
    }

    private Sftp createSftp(ServerDto server) throws Throwable {
        JSch jSch = new JSch();
        Session session = jSch.getSession(server.getUsername(), server.getIp(), server.getPort().intValue());
        session.setConfig("StrictHostKeyChecking", "no");
        session.setPassword(server.getPassword());
        if (server.getProxy() != null) {
            Proxy proxy = createProxy(server);

            session.setProxy(proxy);
        }
        session.connect();
        session.setTimeout(1000 * 60 * 60);
        return new Sftp(session);
    }

    private Proxy createProxy(ServerDto server) throws Throwable {
        ProxyDto proxyDto = server.getProxy();

        Proxy proxy = proxyDto.getType().getConstructor().newInstance(proxyDto.getIp(), proxyDto.getPort().intValue());

        proxyDto.getType().getSetUserPasswd().invoke(proxy, proxyDto.getUsername(), proxyDto.getPassword());

        return proxy;
    }

    private Sftp getSftp(String id) {
        SFTPBean sftp = SFTPContext.SFTP_POOL.get(id);
        if (sftp == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "SFTP连接已失效");
        }

        sftp.setTime(System.currentTimeMillis());

        return sftp.getSftp();
    }

    @Override
    public String pwd(String id) {
        return getSftp(id).pwd();
    }

    @Override
    public List<LsFileDto> ls(String id, String path) {
        Sftp sftp = getSftp(id);
        List<String> files = sftp.lsFiles(path);
        List<String> dirs = sftp.lsDirs(path);
        return Stream.concat(
                dirs.stream().map(dir -> new LsFileDto(dir, LsFileTypeEnum.DIR)),
                files.stream().map(file -> new LsFileDto(file, LsFileTypeEnum.FILE))
        ).collect(Collectors.toList());
    }

    @Override
    public void mkdir(String id, String path) {
        getSftp(id).mkdir(path);
    }

    @Override
    public void rm(String id, String path) {
        getSftp(id).delFile(path);
    }

    @Override
    public void rmDir(String id, String path) {
        getSftp(id).delDir(path);
    }

    @Override
    @SneakyThrows
    public void rename(String id, String oldPath, String newPath) {
        try {
            getSftp(id).getClient().rename(oldPath, newPath);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "文件名已存在");
        }
    }

    @Override
    @SneakyThrows
    public void upload(String id, MultipartFile file, String remotePath) {
        String filename = file.getOriginalFilename();
        boolean upload = getSftp(id).upload(remotePath, filename, file.getInputStream());
        if (!upload) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "上传失败");
        }
    }

    @Override
    public void download(String id, String remotePath, OutputStream outputStream) {
        getSftp(id).download(remotePath, outputStream);
    }

    @Override
    public void close(String id) {
        SFTPBean sftp = SFTPContext.SFTP_POOL.get(id);
        if (sftp == null) {
            return;
        }

        sftp.getSftp().close();
        SFTPContext.SFTP_POOL.remove(id);
    }
}
