package com.codeages.termiusplus.biz.user.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.user.dto.LoginParams;
import com.codeages.termiusplus.biz.user.dto.UserAuthedDto;
import com.codeages.termiusplus.biz.user.dto.UserWithRolesDto;
import com.codeages.termiusplus.biz.user.entity.UserRole;
import com.codeages.termiusplus.biz.user.manager.UserCacheManager;
import com.codeages.termiusplus.biz.user.repository.UserRoleRepository;
import com.codeages.termiusplus.biz.user.service.UserAuthService;
import com.codeages.termiusplus.config.AppConfig;
import com.codeages.termiusplus.exception.AppException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class UserAuthServiceImpl implements UserAuthService {

    private final AppConfig config;

    private final PasswordEncoder encoder;

    private final UserCacheManager cacheManager;

    private final HttpServletResponse response;

    private final UserRoleRepository userRoleRepository;

    public UserAuthServiceImpl(AppConfig config, PasswordEncoder encoder, UserCacheManager cacheManager,
                               HttpServletResponse response, UserRoleRepository userRoleRepository) {
        this.config = config;
        this.encoder = encoder;
        this.cacheManager = cacheManager;
        this.response = response;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserAuthedDto login(LoginParams params) {
        var user = cacheManager.getByUsername(params.getUsername())
                .orElseThrow((() -> new AppException(ErrorCode.NOT_FOUND, "用户名或密码不正确")));

        if (!encoder.matches(params.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.NOT_FOUND, "用户名或密码不正确");
        }

        var algo = Algorithm.HMAC256(config.secret());
        var builder = JWT.create();
        builder.withExpiresAt(new Date(System.currentTimeMillis() + 86400000L*9000L));
        builder.withSubject(user.getId().toString());
        var token = builder.sign(algo);

        var userOp = cacheManager.getWithRoles(user.getId());
        if (userOp.isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        Cookie cookie = new Cookie("token", token);

        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 60 * 24 * 90);

        response.addCookie(cookie);

        return makeUserAuthedDto(userOp.get(), token);
    }

    @Override
    public UserAuthedDto auth(String token) {
        DecodedJWT jwt;
        try {
            jwt = JWT.decode(token);
        } catch (JWTDecodeException e) {
            throw new AppException(ErrorCode.INVALID_AUTHENTICATION, "认证令牌解码失败");
        }

        try {
            JWT.require(Algorithm.HMAC256(config.secret()))
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            throw new AppException(ErrorCode.INVALID_AUTHENTICATION, String.format("认证令牌校验失败(%s)", e.getMessage()));
        }

        Long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_AUTHENTICATION, "认证令牌解码用户ID失败");
        }

        var userOp = cacheManager.getWithRoles(userId);
        if (userOp.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_AUTHENTICATION, String.format("认证用户不存在(ID: %s)", userId));
        }

        return makeUserAuthedDto(userOp.get(), token);
    }

    private UserAuthedDto makeUserAuthedDto(UserWithRolesDto user, String token) {
        var userAuthed = new UserAuthedDto();
        userAuthed.setId(user.getId());
        userAuthed.setUsername(user.getUsername());
        userAuthed.setRoles(user.getRoles());
        userAuthed.setToken(token);
        userAuthed.setRoleIds(userRoleRepository.findAllByUserId(user.getId()).stream().map(UserRole::getRoleId).collect(Collectors.toList()));

        return userAuthed;
    }
}
