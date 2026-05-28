package com.codeages.termiusplus.exception;

import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppErrorConfig {

    @Bean
    public ErrorAttributes errorAttributes() {
        return new AppErrorAttributes();
    }
}
