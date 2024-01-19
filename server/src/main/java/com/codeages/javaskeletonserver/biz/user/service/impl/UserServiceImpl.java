package com.codeages.javaskeletonserver.biz.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.objectlog.service.ObjectLogService;
import com.codeages.javaskeletonserver.biz.user.dto.*;
import com.codeages.javaskeletonserver.biz.user.entity.QUser;
import com.codeages.javaskeletonserver.biz.user.entity.User;
import com.codeages.javaskeletonserver.biz.user.entity.UserRole;
import com.codeages.javaskeletonserver.biz.user.manager.UserCacheManager;
import com.codeages.javaskeletonserver.biz.user.mapper.UserMapper;
import com.codeages.javaskeletonserver.biz.user.repository.UserRepository;
import com.codeages.javaskeletonserver.biz.user.repository.UserRoleRepository;
import com.codeages.javaskeletonserver.biz.user.service.UserService;
import com.codeages.javaskeletonserver.biz.util.QueryUtils;
import com.codeages.javaskeletonserver.exception.AppException;
import com.codeages.javaskeletonserver.security.SecurityContext;
import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserServiceImpl implements UserService {

    private final UserRepository repo;

    private final UserMapper mapper;

    private final UserCacheManager cacheManager;

    private final Validator validator;

    private final PasswordEncoder encoder;

    private final SecurityContext context;

    private final ObjectLogService logService;

    private final UserRoleRepository userRoleRepository;

    public UserServiceImpl(UserRepository repo,
                           UserMapper mapper,
                           UserCacheManager cacheManager,
                           Validator validator,
                           PasswordEncoder encoder,
                           SecurityContext context,
                           ObjectLogService logService,
                           UserRoleRepository userRoleRepository) {
        this.repo = repo;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
        this.validator = validator;
        this.encoder = encoder;
        this.context = context;
        this.logService = logService;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional
    public UserDto create(UserCreateParams params) {
        var errors = validator.validate(params);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        repo.getByUsername(params.getUsername()).ifPresent((u) -> {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "用户名已存在");
        });

        var user = mapper.toEntity(params);

        user.setPassword(encoder.encode(user.getPassword()));
        user.setRegisterAt(System.currentTimeMillis());
        user.setRegisterIp(context.getUser().getIp());

        repo.save(user);

        handleRoles(user, params.getRoleIds());

        cacheManager.removeCache(user);

        if (context.getUser() != null) {
            logService.info("User", user.getId(), "create", "管理员创建用户");
        }

        return mapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto update(UserUpdateParams params) {
        User user = repo.findById(params.getId())
                              .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
        if (StrUtil.isNotEmpty(params.getPassword())) {
            user.setPassword(encoder.encode(params.getPassword()));
        }
        repo.save(user);

        handleRoles(user, params.getRoleIds());

        cacheManager.removeCache(user);

        if (context.getUser() != null) {
            logService.info("User", user.getId(), "update", "管理员更新用户");
        }

        return mapper.toDto(user);
    }

    private void handleRoles(User user, List<Long> roleIds) {
        userRoleRepository.deleteAllByUserId(user.getId());

        if (CollUtil.isEmpty(roleIds)) {
            return;
        }

        List<UserRole> userRoles = roleIds.stream().map(roleId -> {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(roleId);
            return userRole;
        }).collect(Collectors.toList());

        userRoleRepository.saveAll(userRoles);
    }

    @Override
    public void lock(Long id) {
        var user = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.setLocked(true);

        cacheManager.removeCache(user);
        repo.save(user);

        logService.info("User", id, "lock", "锁定用户");
    }

    @Override
    public void unlock(Long id) {
        var user = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
        user.setLocked(false);

        cacheManager.removeCache(user);
        repo.save(user);

        logService.info("User", id, "unlock", "解锁用户");
    }

    @Override
    public Optional<UserDto> get(Long id) {
        return cacheManager.get(id).map(mapper::toDto);
    }

    @Override
    public Optional<UserDto> getByUsername(String name) {
        return cacheManager.getByUsername(name).map(mapper::toDto);
    }

    @Override
    public Page<UserDto> search(UserSearchParams params, Pageable pager) {
        var q = QUser.user;
        var builder = new BooleanBuilder();

        if (StrUtil.isNotEmpty(params.getUsername())) {
            builder.and(q.username.eq(params.getUsername()));
        }

        if (StrUtil.isNotEmpty(params.getEmail())) {
            builder.and(q.username.eq(params.getEmail()));
        }
        Page<UserDto> page = repo.findAll(builder, pager).map(mapper::toDto);

        QueryUtils.batchQueryOneToMany(
                page.getContent(),
                UserDto::getId,
                userRoleRepository::findAllByUserIdIn,
                UserRole::getUserId,
                this::setRoleIds
        );

        return page;
    }

    private void setRoleIds(UserDto userDto, List<UserRole> roleDto) {
        userDto.setRoleIds(roleDto.stream().map(UserRole::getRoleId).collect(Collectors.toList()));
    }

    @Override
    public List<UserDto> findAllByIdIn(List<Long> ids) {
        return mapper.toDto(repo.findAllByIdIn(ids));
    }
}
