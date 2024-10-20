package com.codeages.termiusplus.config;

import cn.hutool.extra.spring.SpringUtil;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

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
