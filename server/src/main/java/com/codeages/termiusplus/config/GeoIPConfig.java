package com.codeages.termiusplus.config;

import com.maxmind.geoip2.DatabaseReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Configuration
public class GeoIPConfig {
    @Value("${geo.path}")
    private String geoPath ;

    @Bean
    public DatabaseReader databaseReader() throws IOException {
        File database = new File(geoPath);
        return new DatabaseReader.Builder(database).build();
    }
}
