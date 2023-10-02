package com.codeages.javaskeletonserver.biz.server.repository;

import com.codeages.javaskeletonserver.biz.server.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface GroupRepository extends JpaRepository<Group, Long>, QuerydslPredicateExecutor<Group> {

}


