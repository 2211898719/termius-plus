package com.codeages.termiusplus.biz.patrol.agent;

import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.user.entity.Role;
import com.codeages.termiusplus.biz.user.repository.RoleRepository;
import com.codeages.termiusplus.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 巡查工具调用的服务器权限检查。
 * 当前用户的角色若有 0L（表示"全部"），则可访问所有服务器；
 * 否则只能访问角色 serverPermission 列表中明确列出的服务器 ID。
 *
 * 工具调用会跨越 Reactor 调度线程，SecurityContextHolder 的 ThreadLocal 不会跟随。
 * 调用方必须在进入流式调用前通过 {@link #setCurrentUser(AuthUser)} 把当前用户传进来，
 * 在 finally 中调用 {@link #clear()}。这里存的是全局快照，多线程都能读到。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolPermissionService {

    /** serverPermission 列表中的 0L 表示「全部服务器」 */
    public static final long ALL_SERVERS = 0L;

    private final RoleRepository roleRepository;

    private static volatile AuthUser CURRENT_USER;

    public static void setCurrentUser(AuthUser user) {
        CURRENT_USER = user;
    }

    public static void clear() {
        CURRENT_USER = null;
    }

    /** 当前用户可访问的服务器 ID 集合。包含 0L 表示全部。 */
    public Set<Long> getAccessibleServerIds() {
        AuthUser user = currentUser();
        if (user == null || user.getRoleIds() == null || user.getRoleIds().isEmpty()) {
            return Collections.emptySet();
        }
        List<Role> roles = roleRepository.findAllByIdIn(user.getRoleIds());
        Set<Long> ids = new HashSet<>();
        for (Role r : roles) {
            if (r.getServerPermission() == null || r.getServerPermission().isBlank()) {
                continue;
            }
            try {
                List<Long> parsed = JSONUtil.toList(r.getServerPermission(), Long.class);
                if (parsed.contains(ALL_SERVERS)) {
                    return Set.of(ALL_SERVERS);
                }
                ids.addAll(parsed);
            } catch (Exception e) {
                log.warn("解析角色 {} 的 serverPermission 失败: {}", r.getName(), r.getServerPermission(), e);
            }
        }
        return ids;
    }

    public boolean canAccessServer(Long serverId) {
        if (serverId == null) {
            return false;
        }
        Set<Long> accessible = getAccessibleServerIds();
        return accessible.contains(ALL_SERVERS) || accessible.contains(serverId);
    }

    private AuthUser currentUser() {
        AuthUser user = CURRENT_USER;
        if (user != null) {
            return user;
        }
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof AuthUser u)) {
                return null;
            }
            return u;
        } catch (Exception e) {
            return null;
        }
    }
}
