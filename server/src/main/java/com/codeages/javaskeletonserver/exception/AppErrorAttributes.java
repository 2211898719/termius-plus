package com.codeages.javaskeletonserver.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

public class AppErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        var attrs =  super.getErrorAttributes(webRequest, options);
        var error = AppError.fromAttributeMap(attrs);
        return error.toAttributeMap();
    }
}
