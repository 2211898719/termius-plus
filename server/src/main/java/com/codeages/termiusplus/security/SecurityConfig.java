package com.codeages.termiusplus.security;

import com.codeages.termiusplus.biz.user.service.UserAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserAuthService userAuthService;

    private final AuthAccessDeniedHandler authAccessDeniedHandler;

    private final ObjectMapper objectMapper;

    private final UriConfig uriConfig;

    public SecurityConfig(UserAuthService userAuthService, AuthAccessDeniedHandler authAccessDeniedHandler,
                          ObjectMapper objectMapper,
                          UriConfig uriConfig) {
        this.userAuthService = userAuthService;
        this.authAccessDeniedHandler = authAccessDeniedHandler;
        this.objectMapper = objectMapper;
        this.uriConfig = uriConfig;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter(userAuthService, objectMapper, uriConfig);
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedHandler() {
        return new AuthEntryPoint(objectMapper);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors()
            .and()
            // 禁用 CSRF
            .csrf()
            .disable()
            .exceptionHandling()
            .accessDeniedHandler(authAccessDeniedHandler)
            .authenticationEntryPoint(unauthorizedHandler())
            .and()
            // 不需要session（不创建会话）
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/api-admin/public/**")
            .permitAll()
            .antMatchers("/api-app/public/**")
            .permitAll()
            .antMatchers("/ws/**")
            .permitAll()
            .antMatchers("/socket/**")
            .permitAll()
            .antMatchers("/api-admin/file/get/**")
            .permitAll()
            // 其他都需要鉴权
            .anyRequest()
            .authenticated();

        // 禁用缓存
        http.headers()
            .cacheControl();

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
