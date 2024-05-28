package com.codeages.termiusplus.biz.server.repository;

import com.codeages.termiusplus.biz.server.entity.ServerServiceMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ServerServiceMonitorRepository extends JpaRepository<ServerServiceMonitor, Long>, QuerydslPredicateExecutor<ServerServiceMonitor> {

}


