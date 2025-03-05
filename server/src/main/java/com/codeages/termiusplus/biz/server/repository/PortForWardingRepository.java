package com.codeages.termiusplus.biz.server.repository;

import com.codeages.termiusplus.biz.server.entity.PortForwarding;
import com.codeages.termiusplus.biz.server.enums.PortForWardingStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

public interface PortForWardingRepository extends JpaRepository<PortForwarding, Long>, QuerydslPredicateExecutor<PortForwarding> {

    List<PortForwarding> findAllByStatus(PortForWardingStatusEnum status);
}


