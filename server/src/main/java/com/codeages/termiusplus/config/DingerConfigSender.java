package com.codeages.termiusplus.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
@ComponentScan(basePackages = {"com.github.jaemon.dinger.config"})
public class DingerConfigSender {

}
