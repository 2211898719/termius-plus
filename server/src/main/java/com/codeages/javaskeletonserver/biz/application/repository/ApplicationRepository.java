package com.codeages.javaskeletonserver.biz.application.repository;

import com.codeages.javaskeletonserver.biz.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long>, QuerydslPredicateExecutor<Application> {

    List<Application> findAllByParentId(Long parentId);
}


