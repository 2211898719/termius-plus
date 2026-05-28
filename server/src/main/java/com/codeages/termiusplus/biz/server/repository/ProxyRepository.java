package com.codeages.termiusplus.biz.server.repository;

import com.codeages.termiusplus.biz.server.entity.Proxy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProxyRepository extends JpaRepository<Proxy, Long>, JpaSpecificationExecutor<Proxy> {

}


