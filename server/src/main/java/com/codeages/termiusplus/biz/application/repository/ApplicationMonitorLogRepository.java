package com.codeages.termiusplus.biz.application.repository;

import com.codeages.termiusplus.biz.application.entity.ApplicationMonitorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface ApplicationMonitorLogRepository extends JpaRepository<ApplicationMonitorLog, Long>, QuerydslPredicateExecutor<ApplicationMonitorLog> {


    @Query(value = "SELECT e.applicationId, COUNT(e) * 60 AS totalCount " +
            "FROM ApplicationMonitorLog e " +
            "WHERE e.date BETWEEN :startDate AND :endDate " +
            "GROUP BY e.applicationId " +
            "ORDER BY COUNT(e) * 60 DESC")
    Page<Object[]> rankApplicationMonitorLogs(@Param("startDate") Date startDate, @Param("endDate") Date endDate , Pageable pageable);
}


