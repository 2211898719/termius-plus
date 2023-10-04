package com.codeages.javaskeletonserver.biz.server.context;

import cn.hutool.core.lang.Pair;
import cn.hutool.extra.ssh.Sftp;
import com.codeages.javaskeletonserver.biz.server.dto.SFTPBean;

import java.util.concurrent.ConcurrentHashMap;

public class SFTPContext {

    public static final ConcurrentHashMap<String, SFTPBean> SFTP_POOL = new ConcurrentHashMap<>();

}
