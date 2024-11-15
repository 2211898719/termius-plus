package com.codeages.termiusplus.biz.server.service;

import cn.hutool.core.lang.tree.Tree;
import com.codeages.termiusplus.biz.server.dto.*;
import net.schmizz.sshj.SSHClient;

import java.net.Proxy;
import java.util.Date;
import java.util.List;

public interface ServerService {

    List<Tree<Long>> findAll(List<Long> serverIds);

    boolean existServerByProxyId(Long proxyId);

    List<Tree<Long>> findAllDb();

    void create(ServerCreateParams serverCreateParams);

    void update(ServerUpdateParams serverUpdateParams);

    void delete(Long id);

    void sort(List<TreeSortParams> treeSortParams);

    ServerDto findById(Long id);

    List<ServerDto> findByIdIn(List<Long> ids);

    List<ServerDto> findAllTestInfoServer();

    SSHClient createSSHClient(Long id);

    SSHClient createSSHClient(Long id, String sessionId);

    Proxy createProxy(ServerDto server);

    List<Tree<Long>> groupList();

    List<Long> findAllTopId();

    /**
     * 获取某个服务器某个用户的 history
     */
    List<String> getHistory(Long serverId);

    List<String> getMysqlHistory(Long serverId);

    List<ServerRunLogDTO> syncAllServerRunInfo();

    List<ServerRunLogDTO> getServerLastRunInfoAfterLimit(Date date);

    List<ServerRunLogDTO> getServerLastRunInfoAfter(Date date);

    List<ServerRunLogDTO> getServerLastRunInfoAfter(Long serverId, Date date);
}


