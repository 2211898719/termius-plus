package com.codeages.termiusplus.biz.server.repository;

import com.codeages.termiusplus.biz.server.entity.PortForwarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface PortForWardingRepository extends JpaRepository<PortForwarding, Long>, QuerydslPredicateExecutor<PortForwarding> {

}


