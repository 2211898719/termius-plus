package com.codeages.termiusplus.biz.application.repository;

import com.codeages.termiusplus.biz.application.entity.ApplicationMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Optional;

public interface ApplicationMonitorRepository extends JpaRepository<ApplicationMonitor, Long>, QuerydslPredicateExecutor<ApplicationMonitor> {

    void deleteByApplicationId(Long applicationId);

    Optional<ApplicationMonitor> findByApplicationId(Long applicationId);
}


