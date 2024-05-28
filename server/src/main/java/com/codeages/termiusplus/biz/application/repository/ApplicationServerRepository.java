package com.codeages.termiusplus.biz.application.repository;

import com.codeages.termiusplus.biz.application.entity.ApplicationServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ApplicationServerRepository extends JpaRepository<ApplicationServer, Long>, QuerydslPredicateExecutor<ApplicationServer> {

    void deleteByApplicationId(Long applicationId);
}


