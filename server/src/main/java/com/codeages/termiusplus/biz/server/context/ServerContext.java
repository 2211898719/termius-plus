package com.codeages.termiusplus.biz.server.context;

import com.codeages.termiusplus.biz.server.dto.SFTPBean;
import com.codeages.termiusplus.biz.server.ws.ssh.SshHandler;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ServerContext {

    public static final ConcurrentMap<String, SshHandler.HandlerItem> SSH_POOL = new ConcurrentHashMap<>();

    public static final ConcurrentMap<String, SFTPBean> SFTP_POOL = new ConcurrentHashMap<>();

    public static final Map<String, BlockingQueue<String>> AUTH_KEYBOARD_INTERACTIVE_POOL = new ConcurrentHashMap<>();


}
