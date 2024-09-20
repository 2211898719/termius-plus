package com.codeages.termiusplus.biz.server.repository;

import com.codeages.termiusplus.biz.server.entity.ServerRunLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Date;
import java.util.List;

public interface ServerRunLogRepository extends JpaRepository<ServerRunLog, Long>, QuerydslPredicateExecutor<ServerRunLog> {

    List<ServerRunLog> findByDateAfter(Date date);
}


