package com.codeages.termiusplus.biz.snippet.repository;

import com.codeages.termiusplus.biz.snippet.entity.Command;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CommandRepository extends JpaRepository<Command, Long>, JpaSpecificationExecutor<Command>  {

}


