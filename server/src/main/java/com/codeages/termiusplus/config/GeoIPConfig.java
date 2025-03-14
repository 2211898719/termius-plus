package com.codeages.termiusplus.config;

import com.maxmind.geoip2.DatabaseReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Slf4j
@Configuration
public class GeoIPConfig {
    @Value("${geo.path}")
    private String geoPath;

    @Bean
    public DatabaseReader databaseReader() {
        File database = new File(geoPath);
        try {
            return new DatabaseReader.Builder(database).build();
        } catch (IOException e) {
            log.error("GeoIP database not found at {}", geoPath);
            return null;
        }
    }
}
