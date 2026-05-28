package com.codeages.termiusplus.biz.server.repository;

import com.codeages.termiusplus.biz.server.entity.ServerServiceMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServerServiceMonitorRepository extends JpaRepository<ServerServiceMonitor, Long>, JpaSpecificationExecutor<ServerServiceMonitor> {

}


