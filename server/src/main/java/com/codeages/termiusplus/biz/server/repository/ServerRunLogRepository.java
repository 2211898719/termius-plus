package com.codeages.termiusplus.biz.server.repository;

import com.codeages.termiusplus.biz.server.entity.ServerRunLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Date;
import java.util.List;

public interface ServerRunLogRepository extends JpaRepository<ServerRunLog, Long>, JpaSpecificationExecutor<ServerRunLog> {

    List<ServerRunLog> findByDateAfter(Date date);

    List<ServerRunLog> findByServerIdAndDateAfter(Long serverId, Date date);
}


