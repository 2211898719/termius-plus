package com.codeages.javaskeletonserver.biz.server.service;

import cn.hutool.core.lang.tree.Tree;
import com.codeages.javaskeletonserver.biz.server.dto.*;
import net.schmizz.sshj.SSHClient;

import java.net.Proxy;
import java.util.List;

public interface ServerService {

    List<Tree<Long>> findAll(List<Long> serverIds);

    List<Tree<Long>> findAllDb();

    void create(ServerCreateParams serverCreateParams);

    void update(ServerUpdateParams serverUpdateParams);

    void delete(Long id);

    void sort(List<TreeSortParams> treeSortParams);

    ServerDto findById(Long id);

    List<ServerDto> findByIdIn(List<Long> ids);

    SSHClient createSSHClient(Long id);

    SSHClient createSSHClient(Long id,String sessionId);

    Proxy createProxy(ServerDto server);

    List<Tree<Long>> groupList();

    List<Long> findAllTopId();
}


