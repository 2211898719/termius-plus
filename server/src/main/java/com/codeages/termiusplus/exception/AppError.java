package com.codeages.termiusplus.exception;

import com.codeages.termiusplus.biz.ErrorCode;
import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Data
public class AppError {

    private long timestamp;

    private int status;

    private String code;

    private String message;

    private String path;

    private String traceId;

    public static AppError fromAppException(AppException e, HttpServletRequest req) {
        var error = new AppError();
        error.setTimestamp(System.currentTimeMillis());
        error.setStatus(e.getStatus());
        error.setCode(e.getCode());
        error.setMessage(e.getMessage());
        error.setPath(req.getRequestURI());
        error.setTraceId("");

        return error;
    }

    public static AppError fromAttributeMap(final Map<String, Object> attributes) {
        var error = new AppError();
        error.setTimestamp(System.currentTimeMillis());
        error.setStatus((Integer)attributes.get("status"));

        if  (error.getStatus() == 401) {
            error.setCode(ErrorCode.UNAUTHORIZED.getCode());
        } else if (error.getStatus() == 403) {
          error.setCode(ErrorCode.ACCESS_DENIED.getCode());
        } else if (error.getStatus() == 404) {
            error.setCode(ErrorCode.NOT_FOUND.getCode());
        } else if (error.getStatus() >= 500) {
            error.setCode(ErrorCode.INTERNAL_ERROR.getCode());
        } else {
            error.setCode(ErrorCode.BAD_REQUEST.getCode());
        }

        error.setMessage((String) attributes.getOrDefault("error", ""));
        error.setPath((String) attributes.getOrDefault("path", ""));
        error.setTraceId("");

        return error;
    }

    public Map<String, Object> toAttributeMap() {
        return Map.of(
            "timestamp", timestamp,
            "status", status,
            "code", code,
            "message", message,
            "path", path,
            "traceId", traceId
        );
    }
}
