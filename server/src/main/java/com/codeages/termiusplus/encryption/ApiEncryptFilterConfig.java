package com.codeages.termiusplus.encryption;

import com.cxytiandi.encrypt.core.EncryptionConfig;
import com.cxytiandi.encrypt.springboot.init.ApiEncryptDataInit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiEncryptFilterConfig {

    @Autowired
    private RsaEncryptAlgorithm rsaEncryptAlgorithm;

    @Value("${spring.encrypt.enabled}")
    private Boolean enable;

    @Bean
    public FilterRegistrationBean<EncryptionFilter> filterRegistration() {
        EncryptionConfig config = new EncryptionConfig();
        config.setResponseCharset("UTF-8");
        config.setDebug(!enable);
        FilterRegistrationBean<EncryptionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new EncryptionFilter(config, rsaEncryptAlgorithm, null));
        registration.addUrlPatterns("/*");
        registration.setName("EncryptionFilter");
        registration.setOrder(1);
        registration.addUrlPatterns();
        return registration;
    }

    @Bean
    public ApiEncryptDataInit apiEncryptDataInit() {
        return new ApiEncryptDataInit();
    }

}
