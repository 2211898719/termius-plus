package com.codeages.javaskeletonserver.biz.objectlog.repository;

import com.codeages.javaskeletonserver.biz.objectlog.entity.ObjectLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ObjectLogRepository extends JpaRepository<ObjectLog, Long>, QuerydslPredicateExecutor<ObjectLog> {
}
