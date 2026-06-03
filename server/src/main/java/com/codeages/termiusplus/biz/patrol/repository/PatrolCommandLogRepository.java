package com.codeages.termiusplus.biz.patrol.repository;

import com.codeages.termiusplus.biz.patrol.entity.PatrolCommandLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatrolCommandLogRepository extends JpaRepository<PatrolCommandLog, Long> {
}
