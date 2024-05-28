package com.codeages.termiusplus.biz.storage;

import com.codeages.termiusplus.common.ErrorCodeInterface;

/**
 * @author hjl
 */

public enum StorageErrorCode implements ErrorCodeInterface {

    /**
     * 文件类型错误
     */
    TYPE_ERROR("文件类型错误", 400),
    /**
     * 文件为空
     */
    EMPTY_ERROR("文件为空", 400);

    private final String message;

    /**
     * HTTP 状态码
     */
    private int status;

    StorageErrorCode(String message) {
        this.message = message;
    }

    StorageErrorCode(String message, int status) {
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
