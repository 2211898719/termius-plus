package com.codeages.javaskeletonserver.biz.application.repository;

import com.codeages.javaskeletonserver.biz.application.entity.ApplicationServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ApplicationServerRepository extends JpaRepository<ApplicationServer, Long>, QuerydslPredicateExecutor<ApplicationServer> {

    void deleteByApplicationId(Long applicationId);
}


