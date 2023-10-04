package com.codeages.javaskeletonserver.biz.util;


import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

/**
 * @author: void
 * @date: 2021-10-15 16:08
 * @description: sftp服务器文件操作
 * @version: 1.0
 *
 */
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Vector;

@Component
public class SFTPUtil {
    @SneakyThrows
    public static void main(String[] args) {
        JSch jSch = new JSch();
        Session ubuntu = jSch.getSession("ubuntu", "111.229.24.132");
        ubuntu.setConfig("StrictHostKeyChecking", "no");
        ubuntu.setPassword("z13867013469[");
        ubuntu.connect();
        ubuntu.setTimeout(1000 * 60);
        Sftp sftp = new Sftp(ubuntu);
        System.out.println(sftp.home());
        System.out.println(sftp.pwd());
        Thread.sleep(1000*61);
        sftp.close();
        System.out.println(sftp.lsFiles("/home/ubuntu"));
        System.out.println(sftp.lsDirs("/home/ubuntu"));
    }

    private static final Logger logger = LoggerFactory.getLogger(SFTPUtil.class);
    private Session session = null;
    private ChannelSftp channel = null;
    private int timeout = 60000;

    /**
     * 连接sftp服务器
     */
    public boolean connect(String ftpUsername, String ftpAddress, int ftpPort, String ftpPassword) {
        boolean isSuccess = false;
        if (channel != null) {
            System.out.println("通道不为空");
            return false;
        }
        //创建JSch对象
        JSch jSch = new JSch();
        try {
            // 根据用户名，主机ip和端口获取一个Session对象
            session = jSch.getSession(ftpUsername, ftpAddress, ftpPort);

            //设置密码
            session.setPassword(ftpPassword);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            //为Session对象设置properties
            session.setConfig(config);
            //设置超时
            session.setTimeout(timeout);
            //通过Session建立连接
            session.connect();
            System.out.println("Session连接成功");
            // 打开SFTP通道
            channel = (ChannelSftp) session.openChannel("sftp");
            // 建立SFTP通道的连接
            channel.connect();
            System.out.println("通道连接成功");
            isSuccess = true;
        } catch (JSchException e) {
            logger.error("连接服务器异常", e);
        }
        return isSuccess;
    }

    /**
     * 关闭连接
     */
    public void close() {
        //操作完毕后，关闭通道并退出本次会话
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * 文件上传
     * 采用默认的传输模式：OVERWRITE
     *
     * @param src      输入流
     * @param dst      上传路径
     * @param fileName 上传文件名
     * @throws SftpException
     */
    public boolean upLoadFile(InputStream src, String dst, String fileName) throws SftpException {
        boolean isSuccess = false;
        try {
            if (createDir(dst)) {
                channel.put(src, fileName);
                isSuccess = true;
            }
        } catch (SftpException e) {
            logger.error(fileName + "文件上传异常", e);
        }
        return isSuccess;
    }

    /**
     * 创建一个文件目录
     *
     * @param createpath 路径
     * @return
     */
    public boolean createDir(String createpath) {
        boolean isSuccess = false;
        try {
            if (isDirExist(createpath)) {
                channel.cd(createpath);
                return true;
            }
            String pathArry[] = createpath.split("/");
            StringBuffer filePath = new StringBuffer("/");
            for (String path : pathArry) {
                if (path.equals("")) {
                    continue;
                }
                filePath.append(path + "/");
                if (isDirExist(filePath.toString())) {
                    channel.cd(filePath.toString());
                } else {
                    // 建立目录
                    channel.mkdir(filePath.toString());
                    // 进入并设置为当前目录
                    channel.cd(filePath.toString());
                }
            }
            channel.cd(createpath);
            isSuccess = true;
        } catch (SftpException e) {
            logger.error("目录创建异常！", e);
        }
        return isSuccess;
    }

    /**
     * 判断目录是否存在
     *
     * @param directory 路径
     * @return
     */
    public boolean isDirExist(String directory) {
        boolean isSuccess = false;
        try {
            SftpATTRS sftpATTRS = channel.lstat(directory);
            isSuccess = true;
            return sftpATTRS.isDir();
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().equals("no such file")) {
                isSuccess = false;
            }
        }
        return isSuccess;
    }

    /**
     * 重命名指定文件或目录
     */
    public boolean rename(String oldPath, String newPath) {
        boolean isSuccess = false;
        try {
            channel.rename(oldPath, newPath);
            isSuccess = true;
        } catch (SftpException e) {
            logger.error("重命名指定文件或目录异常", e);
        }
        return isSuccess;
    }

    /**
     * 列出指定目录下的所有文件和子目录。
     */
    public Vector ls(String path) {
        try {
            Vector vector = channel.ls(path);
            return vector;
        } catch (SftpException e) {
            logger.error("列出指定目录下的所有文件和子目录。", e);
        }
        return null;
    }

    /**
     * 删除文件
     *
     * @param directory  linux服务器文件地址
     * @param deleteFile 文件名称
     */
    public boolean deleteFile(String directory, String deleteFile) {
        boolean isSuccess = false;
        try {
            channel.cd(directory);
            channel.rm(deleteFile);
            isSuccess = true;
        } catch (SftpException e) {
            logger.error("删除文件失败", e);
        }
        return isSuccess;
    }

    /**
     * 下载文件
     *
     * @param directory    下载目录
     * @param downloadFile 下载的文件
     * @param saveFile     下载到本地路径
     */
    public boolean download(String directory, String downloadFile, String saveFile) {
        boolean isSuccess = false;
        try {
            channel.cd(directory);
            File file = new File(saveFile);
            channel.get(downloadFile, new FileOutputStream(file));
            isSuccess = true;
        } catch (SftpException e) {
            logger.error("下载文件失败", e);
        } catch (FileNotFoundException e) {
            logger.error("下载文件失败", e);
        }
        return isSuccess;
    }
}
