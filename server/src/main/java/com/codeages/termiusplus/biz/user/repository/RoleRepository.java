package com.codeages.termiusplus.biz.user.repository;

import com.codeages.termiusplus.biz.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    List<Role> findAllByIdIn(List<Long> userRoleIds);

    Role findFirstByName(String name);
}


