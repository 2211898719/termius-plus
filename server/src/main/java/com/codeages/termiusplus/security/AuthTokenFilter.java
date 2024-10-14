package com.codeages.termiusplus.security;

import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.user.service.UserAuthService;
import com.codeages.termiusplus.exception.AppError;
import com.codeages.termiusplus.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final UserAuthService userAuthService;

    private final ObjectMapper objectMapper;

    public static final ThreadLocal<Long> userIdThreadLocal = new ThreadLocal<>();

    private final UriConfig uriConfig;

    public AuthTokenFilter(UserAuthService userAuthService, ObjectMapper objectMapper, UriConfig uriConfig) {
        this.userAuthService = userAuthService;
        this.objectMapper = objectMapper;
        this.uriConfig = uriConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        //是公开的URI，不需要验证token
        if (uriConfig.getUri().stream().anyMatch(uri -> request.getRequestURI().startsWith(uri))) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = parseToken(request);
            if (token != null) {
                var userAuthedDto = userAuthService.auth(token);
                var authUser = new AuthUser(userAuthedDto);
                authUser.setIp(request.getRemoteAddr());
                //如果是websocket请求，需要在这里设置userIdThreadLocal
                if (request.getRequestURI()
                           .startsWith("/socket")) {
                    userIdThreadLocal.set(authUser.getId());
                }
                var authentication = new UsernamePasswordAuthenticationToken(authUser, null, authUser.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext()
                                     .setAuthentication(authentication);
                //给 cookie 里的 token 续期
                Cookie cookie = new Cookie("token", token);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24 * 90);
                response.addCookie(cookie);
            }

            filterChain.doFilter(request, response);
        } catch (AppException e) {
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(e.getStatus());
            response.getWriter()
                    .write(objectMapper.writeValueAsString(AppError.fromAppException(e, request)));
        }
    }

    private String parseToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String headerAuth = request.getHeader("Authorization");
        if (StrUtil.isNotEmpty(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
