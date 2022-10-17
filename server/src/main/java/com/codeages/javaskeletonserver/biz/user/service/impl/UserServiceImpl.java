package com.codeages.javaskeletonserver.biz.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.objectlog.service.ObjectLogService;
import com.codeages.javaskeletonserver.biz.user.dto.UserCreateParams;
import com.codeages.javaskeletonserver.biz.user.dto.UserDto;
import com.codeages.javaskeletonserver.biz.user.dto.UserSearchParams;
import com.codeages.javaskeletonserver.biz.user.entity.QUser;
import com.codeages.javaskeletonserver.biz.user.manager.UserCacheManager;
import com.codeages.javaskeletonserver.biz.user.mapper.UserMapper;
import com.codeages.javaskeletonserver.biz.user.repository.UserRepository;
import com.codeages.javaskeletonserver.biz.user.service.UserService;
import com.codeages.javaskeletonserver.exception.AppException;
import com.codeages.javaskeletonserver.security.SecurityContext;
import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.validation.Validator;
import java.util.Optional;

@Component
public class UserServiceImpl implements UserService {

    private final UserRepository repo;

    private final UserMapper mapper;

    private final UserCacheManager cacheManager;

    private final Validator validator;

    private final PasswordEncoder encoder;

    private final SecurityContext context;

    private final ObjectLogService logService;

    public UserServiceImpl(UserRepository repo, UserMapper mapper, UserCacheManager cacheManager, Validator validator, PasswordEncoder encoder, SecurityContext context, ObjectLogService logService) {
        this.repo = repo;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
        this.validator = validator;
        this.encoder = encoder;
        this.context = context;
        this.logService = logService;
    }

    @Override
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

        cacheManager.removeCache(user);

        if (context.getUser() != null) {
            logService.info("User", user.getId(), "create", "管理员创建用户");
        }

        return mapper.toDto(user);
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

        return repo.findAll(builder, pager).map(mapper::toDto);
    }
}
