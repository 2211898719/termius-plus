package com.codeages.termiusplus.api.app;

import com.codeages.termiusplus.biz.user.dto.ChangePasswordParams;
import com.codeages.termiusplus.biz.user.service.UserService;
import com.codeages.termiusplus.common.OkResponse;
import com.codeages.termiusplus.security.SecurityContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api-app/user")
public class AppUserController {

    private final SecurityContext securityContext;
    private final UserService userService;

    public AppUserController(SecurityContext securityContext, UserService userService) {
        this.securityContext = securityContext;
        this.userService = userService;
    }

    @PostMapping("/changePassword")
    public OkResponse changePassword(@RequestBody ChangePasswordParams params) {
        params.setUserId(securityContext.getUser().getId());
        userService.changePassword(params);
        return OkResponse.TRUE;
    }
}
