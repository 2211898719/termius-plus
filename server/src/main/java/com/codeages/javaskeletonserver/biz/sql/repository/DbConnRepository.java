package com.codeages.javaskeletonserver.biz.sql.repository;

import com.codeages.javaskeletonserver.biz.sql.entity.DbConn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface DbConnRepository extends JpaRepository<DbConn, Long>, QuerydslPredicateExecutor<DbConn> {

}


