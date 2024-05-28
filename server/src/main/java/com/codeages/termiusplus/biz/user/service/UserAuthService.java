package com.codeages.termiusplus.biz.user.service;

import com.codeages.termiusplus.biz.user.dto.LoginParams;
import com.codeages.termiusplus.biz.user.dto.UserAuthedDto;

public interface UserAuthService {

    UserAuthedDto login(LoginParams params);

    /**
     * 认证 Token
     *
     * @param token
     * @return
     */
    UserAuthedDto auth(String token);
}
