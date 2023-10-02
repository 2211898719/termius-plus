package com.codeages.javaskeletonserver.biz.server.repository;

import com.codeages.javaskeletonserver.biz.server.entity.Proxy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ProxyRepository extends JpaRepository<Proxy, Long>, QuerydslPredicateExecutor<Proxy> {

}


