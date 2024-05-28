package com.codeages.termiusplus.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {
    private final Environment env;

    public AppConfig(Environment env) {
        this.env = env;
    }

    public String secret() {
        return env.getProperty("app.secret");
    }
}
