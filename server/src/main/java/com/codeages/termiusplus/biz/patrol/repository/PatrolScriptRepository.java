package com.codeages.termiusplus.biz.patrol.repository;

import com.codeages.termiusplus.biz.patrol.entity.PatrolScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PatrolScriptRepository extends JpaRepository<PatrolScript, Long>,
        JpaSpecificationExecutor<PatrolScript> {
    List<PatrolScript> findAllByEnabledTrue();
    List<PatrolScript> findByCategory(String category);
}
