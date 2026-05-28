package com.codeages.termiusplus.biz.patrol.repository;

import com.codeages.termiusplus.biz.patrol.entity.PatrolTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Date;
import java.util.List;

public interface PatrolTaskRepository extends JpaRepository<PatrolTask, Long>,
        JpaSpecificationExecutor<PatrolTask> {
    List<PatrolTask> findByServerIdAndExecutedAtAfter(Long serverId, Date after);
    List<PatrolTask> findByScriptId(Long scriptId);
    List<PatrolTask> findByStatusIn(List<String> statuses);
}
