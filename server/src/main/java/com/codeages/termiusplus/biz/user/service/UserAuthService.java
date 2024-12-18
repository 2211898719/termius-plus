package com.codeages.termiusplus.biz.user.service;

import com.codeages.termiusplus.biz.user.dto.LoginParams;
import com.codeages.termiusplus.biz.user.dto.UserAuthedDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UserAuthService {

    UserAuthedDto login(LoginParams params, HttpServletRequest request, HttpServletResponse response);

    /**
     * 认证 Token
     *
     * @param token
     * @return
     */
    UserAuthedDto auth(String token);
}
