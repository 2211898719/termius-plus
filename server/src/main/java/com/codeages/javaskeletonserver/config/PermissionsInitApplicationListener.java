package com.codeages.javaskeletonserver.config;


import cn.hutool.json.JSONUtil;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.biz.user.dto.RoleDto;
import com.codeages.javaskeletonserver.biz.user.dto.RoleUpdateParams;
import com.codeages.javaskeletonserver.biz.user.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PermissionsInitApplicationListener implements ApplicationListener<ApplicationReadyEvent> {
    private static final String DEFAULT_ROLE = "ROLE_SUPER_ADMIN";


    private final RoleService roleService;
    private final ServerService serverService;

    public PermissionsInitApplicationListener(RoleService roleService, ServerService serverService) {
        this.roleService = roleService;
        this.serverService = serverService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("init permissions");
        RoleDto roleDto = roleService.findByName(DEFAULT_ROLE);
        roleService.update(new RoleUpdateParams(
                roleDto.getId(),
                roleDto.getName(),
                JSONUtil.toJsonStr(List.of(0L))
        ));
    }

}
