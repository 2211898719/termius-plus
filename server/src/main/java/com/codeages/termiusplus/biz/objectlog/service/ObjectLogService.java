package com.codeages.termiusplus.biz.objectlog.service;

import java.util.Map;

/**
 * 业务对象日志服务
 *
 * 记录业务对象的关键操作事件，可用于回溯
 */
public interface ObjectLogService {

    void info(String type, Long oid, String event);

    void info(String type, Long oid, String event, String message);

    void info(String type, Long oid, String event, String message, Map<String, Object> context);

    void warn(String type, Long oid, String event);

    void warn(String type, Long oid, String event, String message);

    void warn(String type, Long oid, String event, String message, Map<String, Object> context);

    void error(String type, Long oid, String event);

    void error(String type, Long oid, String event, String message);

    void error(String type, Long oid, String event, String message, Map<String, Object> context);
}
