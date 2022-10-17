package com.codeages.javaskeletonserver.biz.user.repository;

import com.codeages.javaskeletonserver.biz.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long>, QuerydslPredicateExecutor<Role> {

    List<Role> findAllByIdIn(List<Long> ids);

}
