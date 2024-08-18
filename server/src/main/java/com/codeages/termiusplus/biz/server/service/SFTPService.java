package com.codeages.termiusplus.biz.server.service;

import com.codeages.termiusplus.biz.server.dto.SFTPBean;
import com.codeages.termiusplus.biz.server.dto.SFTPServerUploadServerParams;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface SFTPService {

    /**
     * 初始化sftp服务器,返回连接id，用于后续操作
     *
     * @return
     */
    String init(String sessionId, Long serverId);

    SFTPBean getSftpBean(String id);

    /**
     * 获取当前目录
     *
     * @param id
     * @return
     */
    String pwd(String id);

    /**
     * 获取目录下的文件
     *
     * @param id
     * @param path
     * @return
     */
    List<RemoteResourceInfo> ls(String id, String path);

    /**
     * 创建目录
     *
     * @param id
     * @param path
     */
    void mkdir(String id, String path);

    /**
     * 删除文件
     *
     * @param id
     * @param path
     */
    void rm(String id, String path);


    /**
     * 删除目录
     *
     * @param id
     * @param path
     */
    void rmDir(String id, String path);

    /**
     * 重命名文件
     *
     * @param id
     * @param oldPath
     * @param newPath
     */
    void rename(String id, String oldPath, String newPath);

    /**
     * 上传文件
     *
     * @param id
     * @param file
     * @param remotePath
     */
    void upload(String id, MultipartFile file, String remotePath);

    /**
     * 下载文件
     *
     * @param id
     * @param remotePath
     */
    void download(String id, String remotePath, HttpServletResponse response) throws IOException;

    long size(String id, String remotePath);

    /**
     * 关闭连接
     *
     * @param id
     */
    void close(String id);

    void serverUploadServer(SFTPServerUploadServerParams params);

    void asyncServerUploadServer(SFTPServerUploadServerParams params);

    byte[] readFile(String id, String remotePath);

    void writeFile(String id, String remotePath, String content);
}


