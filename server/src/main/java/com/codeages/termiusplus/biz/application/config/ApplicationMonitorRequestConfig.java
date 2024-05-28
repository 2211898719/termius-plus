package com.codeages.termiusplus.biz.application.config;

import cn.hutool.http.Method;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMonitorRequestConfig {
    private String url;
    private Method method;
    private Map<String, List<String>> headers;
    private String body;
    private String responseRegex;
    private List<String> timeRange;
}
