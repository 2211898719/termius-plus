package com.codeages.termiusplus.config;

import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.ServerSocket;

@Component
public class ServerSocketFactory {

    @Bean
    @SneakyThrows
    public ServerSocket createServerSocket() {
        return new ServerSocket(0);
    }
}
