package com.codeages.termiusplus.biz.application.repository;

import com.codeages.termiusplus.biz.application.entity.ApplicationMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ApplicationMonitorRepository extends JpaRepository<ApplicationMonitor, Long>, JpaSpecificationExecutor<ApplicationMonitor> {

    void deleteByApplicationId(Long applicationId);

    Optional<ApplicationMonitor> findByApplicationId(Long applicationId);
}


