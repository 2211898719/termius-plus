package com.codeages.javaskeletonserver.biz.server.context;

import com.codeages.javaskeletonserver.biz.server.dto.SFTPBean;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class ServerContext {

    public static final ConcurrentHashMap<String, SFTPBean> SFTP_POOL = new ConcurrentHashMap<>();

    public static final Map<String, BlockingQueue<String>> AUTH_KEYBOARD_INTERACTIVE_POOL = new ConcurrentHashMap<>();

}
