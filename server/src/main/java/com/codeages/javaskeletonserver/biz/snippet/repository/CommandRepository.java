package com.codeages.javaskeletonserver.biz.snippet.repository;

import com.codeages.javaskeletonserver.biz.snippet.entity.Command;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface CommandRepository extends JpaRepository<Command, Long>, QuerydslPredicateExecutor<Command> {

}


