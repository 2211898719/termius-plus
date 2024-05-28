package com.codeages.termiusplus.biz.user.service;

import com.codeages.termiusplus.biz.user.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {

    UserDto create(UserCreateParams params);

    UserDto update(UserUpdateParams params);

    void lock(Long id);

    void unlock(Long id);

    Optional<UserDto> get(Long id);

    Optional<UserDto> getByUsername(String name);

    Page<UserDto> search(UserSearchParams params, Pageable pager);

    List<UserDto> findAllByIdIn(List<Long> ids);
}
