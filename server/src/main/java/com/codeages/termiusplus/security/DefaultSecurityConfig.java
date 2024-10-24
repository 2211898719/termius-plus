package com.codeages.termiusplus.security;


import cn.hutool.extra.spring.SpringUtil;
import com.codeages.termiusplus.biz.user.service.UserAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * @author hjl
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class DefaultSecurityConfig {

    private final UserAuthService userAuthService;

    private final AuthAccessDeniedHandler authAccessDeniedHandler;

    private final ObjectMapper objectMapper;

    private final UriConfig uriConfig;

    public DefaultSecurityConfig(UserAuthService userAuthService, AuthAccessDeniedHandler authAccessDeniedHandler,
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
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.cors(AbstractHttpConfigurer::disable)
            // 禁用 CSRF
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(e -> e.authenticationEntryPoint(SpringUtil.getBean(AuthenticationEntryPoint.class))
                                     .accessDeniedHandler(authAccessDeniedHandler))
            // 不需要session（不创建会话）
            .sessionManagement(e -> e.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(e -> e.requestMatchers("/api-admin/public/**")
                                     .permitAll()
                                     .requestMatchers("/api-app/public/**")
                                     .permitAll()
                                     .requestMatchers("/ws/**")
                                     .permitAll()
                                     .requestMatchers("/socket/**")
                                     .permitAll()
                                     .requestMatchers("/api-admin/file/get/**")
                                     .permitAll()
                                     // 其他都需要鉴权
                                     .anyRequest()
                                     .authenticated());


        // 禁用缓存
        http.headers(e -> e.cacheControl(HeadersConfigurer.CacheControlConfig::disable));

        http.addFilterBefore(SpringUtil.getBean(AuthTokenFilter.class), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
