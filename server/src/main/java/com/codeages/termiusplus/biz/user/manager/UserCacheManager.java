package com.codeages.termiusplus.biz.user.manager;

import com.codeages.termiusplus.biz.user.dto.UserWithRolesDto;
import com.codeages.termiusplus.biz.user.entity.Role;
import com.codeages.termiusplus.biz.user.entity.User;
import com.codeages.termiusplus.biz.user.entity.UserRole;
import com.codeages.termiusplus.biz.user.mapper.UserMapper;
import com.codeages.termiusplus.biz.user.repository.RoleRepository;
import com.codeages.termiusplus.biz.user.repository.UserRepository;
import com.codeages.termiusplus.biz.user.repository.UserRoleRepository;
import com.codeages.termiusplus.common.cache.RedisCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserCacheManager {
    private final UserRepository userRepo;

    private final UserRoleRepository userRoleRepo;

    private final RoleRepository roleRepo;

    private final UserMapper userMapper;

    private final RedisCacheManager cacheManager;

    public UserCacheManager(UserRepository userRepo, UserRoleRepository userRoleRepo, RoleRepository roleRepo, UserMapper userMapper, RedisCacheManager cacheManager) {
        this.userRepo = userRepo;
        this.userRoleRepo = userRoleRepo;
        this.roleRepo = roleRepo;
        this.userMapper = userMapper;
        this.cacheManager = cacheManager;
    }

    public Optional<User> get(Long id) {
        return cacheManager.getById("user", id, () -> userRepo.findById(id), Duration.ofMinutes(10));
    }

    public Optional<User> getByUsername(String username) {
        return cacheManager.getByRel("user", "nameIdRel/" + username, () -> userRepo.getByUsername(username), Duration.ofMinutes(10));
    }

    public Optional<UserWithRolesDto> getWithRoles(Long id) {
        return cacheManager.get("user","withRoles/"+id, () -> {
            var userOp = userRepo.findById(id);
            if (userOp.isEmpty()) {
                return Optional.empty();
            }
            var user = userOp.get();

            var userRoles = userRoleRepo.findAllByUserId(user.getId());
            var userRoleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());

            var roles = roleRepo.findAllByIdIn(userRoleIds).stream().map(Role::getName).collect(Collectors.toList());

            var dto = userMapper.toWithRolesDto(user);
            dto.setRoles(roles);

            return Optional.of(dto);
        });
    }

    public void removeCache(User user) {
        cacheManager.delete("user", List.of(
                "id/"+user.getId(),
                "nameIdRel/"+user.getUsername(),
                "withRoles/"+user.getId()
        ));
    }
}
