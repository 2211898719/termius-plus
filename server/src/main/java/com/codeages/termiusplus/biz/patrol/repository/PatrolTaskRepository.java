package com.codeages.termiusplus.biz.patrol.repository;

import com.codeages.termiusplus.biz.patrol.entity.PatrolTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Date;
import java.util.List;

public interface PatrolTaskRepository extends JpaRepository<PatrolTask, Long>,
        QuerydslPredicateExecutor<PatrolTask> {
    List<PatrolTask> findByServerIdAndExecutedAtAfter(Long serverId, Date after);
    List<PatrolTask> findByScriptId(Long scriptId);
    List<PatrolTask> findByStatusIn(List<String> statuses);
}
