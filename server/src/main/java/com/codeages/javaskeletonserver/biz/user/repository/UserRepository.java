package com.codeages.javaskeletonserver.biz.user.repository;

import com.codeages.javaskeletonserver.biz.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, QuerydslPredicateExecutor<User> {
    Optional<User> getByUsername(String username);
}
