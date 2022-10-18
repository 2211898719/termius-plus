package com.codeages.javaskeletonserver.biz;

import com.codeages.javaskeletonserver.common.ErrorCodeInterface;

public enum ErrorCode implements ErrorCodeInterface {

    BAD_REQUEST("请求格式不正确", 400),

    INVALID_ARGUMENT("参数不正确", 400),

    INTERNAL_ERROR("服务器内部错误", 500),

    ACCESS_DENIED("无权访问", 403),

    UNAUTHORIZED("未登陆", 401),

    INVALID_AUTHENTICATION("认证无效", 401),

    NOT_FOUND("对象不存在", 404),

    ;

    private final String message;

    /**
     * HTTP 状态码
     */
    private int status;

    ErrorCode(String message) {
        this.message = message;
    }

    ErrorCode(String message, int status) {
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "[ERROR " + getCode() + "] " + message;
    }
}
