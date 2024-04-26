package com.codeages.javaskeletonserver.biz.application.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMonitorRequestConfig {
    private String url;
    private HttpMethod method;
    private Map<String, String> headers;
    private String body;
    private String responseRegex;
}
