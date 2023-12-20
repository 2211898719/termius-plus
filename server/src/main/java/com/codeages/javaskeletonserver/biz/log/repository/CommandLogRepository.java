package com.codeages.javaskeletonserver.biz.log.repository;

import com.codeages.javaskeletonserver.biz.log.entity.CommandLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface CommandLogRepository extends JpaRepository<CommandLog, Long>, QuerydslPredicateExecutor<CommandLog> {

}


