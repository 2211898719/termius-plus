package com.codeages.termiusplus.api.admin;


import com.codeages.termiusplus.biz.user.dto.LoginParams;
import com.codeages.termiusplus.biz.user.dto.UserAuthedDto;
import com.codeages.termiusplus.biz.user.service.UserAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api-admin/public")
public class AdminPublicController {

    private final UserAuthService authService;

    public AdminPublicController(UserAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public UserAuthedDto login(@RequestBody LoginParams params,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        return authService.login(params, request, response);
    }

}
