package com.codeages.termiusplus.biz.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.user.dto.RoleCreateParams;
import com.codeages.termiusplus.biz.user.dto.RoleDto;
import com.codeages.termiusplus.biz.user.dto.RoleSearchParams;
import com.codeages.termiusplus.biz.user.dto.RoleUpdateParams;
import com.codeages.termiusplus.biz.user.entity.Role;
import com.codeages.termiusplus.biz.user.mapper.RoleMapper;
import com.codeages.termiusplus.biz.user.repository.RoleRepository;
import com.codeages.termiusplus.biz.user.service.RoleService;
import com.codeages.termiusplus.exception.AppException;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    private final RoleMapper roleMapper;

    private final Validator validator;

    public RoleServiceImpl(RoleRepository roleRepository, RoleMapper roleMapper, Validator validator) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
        this.validator = validator;
    }

    @Override
    public Page<RoleDto> search(RoleSearchParams searchParams, Pageable pageable) {
        Specification<Role> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StrUtil.isNotEmpty(searchParams.getName())) {
                predicates.add(criteriaBuilder.equal(root.get("name"), searchParams.getName()));
            }
            if (searchParams.getName() != null) {
                predicates.add(criteriaBuilder.equal(root.get("name"), searchParams.getName()));
            }
            if (StrUtil.isNotEmpty(searchParams.getServerPermission())) {
                predicates.add(criteriaBuilder.equal(root.get("serverPermission"), searchParams.getServerPermission()));
            }
            if (searchParams.getServerPermission() != null) {
                predicates.add(criteriaBuilder.equal(root.get("serverPermission"), searchParams.getServerPermission()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return roleRepository.findAll(specification, pageable).map(roleMapper::toDto);
    }

    @Override
    public void create(RoleCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        roleRepository.save(roleMapper.toCreateEntity(createParams));
    }

    @Override
    public void update(RoleUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var role = roleRepository.findById(updateParams.getId())
                                 .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        roleMapper.toUpdateEntity(role, updateParams);
        roleRepository.save(role);
    }

    @Override
    public void delete(Long id) {
        roleRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        roleRepository.deleteById(id);
    }

    @Override
    public List<RoleDto> findByIds(List<Long> ids) {
        return roleMapper.toDto(roleRepository.findAllByIdIn(ids));
    }

    @Override
    public RoleDto findByName(String name) {
        return roleMapper.toDto(roleRepository.findFirstByName(name));
    }

}


