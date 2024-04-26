package com.codeages.javaskeletonserver.biz.application.repository;

import com.codeages.javaskeletonserver.biz.application.entity.ApplicationMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Optional;
import java.util.stream.LongStream;

public interface ApplicationMonitorRepository extends JpaRepository<ApplicationMonitor, Long>, QuerydslPredicateExecutor<ApplicationMonitor> {

    void deleteByApplicationId(Long applicationId);

    Optional<ApplicationMonitor> findByApplicationId(Long applicationId);
}


