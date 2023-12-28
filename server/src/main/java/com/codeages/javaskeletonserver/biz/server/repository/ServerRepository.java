package com.codeages.javaskeletonserver.biz.server.repository;

import com.codeages.javaskeletonserver.biz.server.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServerRepository extends JpaRepository<Server, Long>, QuerydslPredicateExecutor<Server> {

    List<Server> findAllByParentId(Long id);

    List<Server> findAllByParentIdIn(Collection<Long> parentId);

    Optional<Server> getFirstByParentIdOrderBySortDesc(Long parentId);

    List<Server> findAllByIsGroupTrue();
}


