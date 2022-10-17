package com.codeages.javaskeletonserver.exception;

import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppErrorConfig {

    @Bean
    public ErrorAttributes errorAttributes() {
        return new AppErrorAttributes();
    }
}
