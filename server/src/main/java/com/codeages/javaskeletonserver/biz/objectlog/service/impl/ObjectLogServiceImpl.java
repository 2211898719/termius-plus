package com.codeages.javaskeletonserver.biz.objectlog.service.impl;

import com.codeages.javaskeletonserver.biz.objectlog.entity.ObjectLog;
import com.codeages.javaskeletonserver.biz.objectlog.repository.ObjectLogRepository;
import com.codeages.javaskeletonserver.biz.objectlog.service.ObjectLogService;
import com.codeages.javaskeletonserver.security.SecurityContext;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ObjectLogServiceImpl implements ObjectLogService {

    private final ObjectLogRepository repo;

    private final SecurityContext context;

    public ObjectLogServiceImpl(ObjectLogRepository repo, SecurityContext context) {
        this.repo = repo;
        this.context = context;
    }

    @Override
    public void info(String type, Long oid, String event) {
        log("info", type, oid, event, null, null);
    }

    @Override
    public void info(String type, Long oid, String event, String message) {
        log("info", type, oid, event, message, null);
    }

    @Override
    public void info(String type, Long oid, String event, String message, Map<String, Object> context) {
        log("info", type, oid, event, message, context);
    }

    @Override
    public void warn(String type, Long oid, String event) {
        log("warn", type, oid, event, null, null);
    }

    @Override
    public void warn(String type, Long oid, String event, String message) {
        log("warn", type, oid, event, message, null);
    }

    @Override
    public void warn(String type, Long oid, String event, String message, Map<String, Object> context) {
        log("warn", type, oid, event, message, context);
    }

    @Override
    public void error(String type, Long oid, String event) {
        log("error", type, oid, event, null, null);
    }

    @Override
    public void error(String type, Long oid, String event, String message) {
        log("error", type, oid, event, message, null);
    }

    @Override
    public void error(String type, Long oid, String event, String message, Map<String, Object> context) {
        log("error", type, oid, event, message, context);
    }

    private void log(String level, String type, Long oid, String event, String message, Map<String, Object> context) {
        var log = new ObjectLog();
        log.setLevel(level);
        log.setType(type);
        log.setOid(oid);
        log.setEvent(event);
        log.setMessage(message);
        log.setContext(context);

        var user = this.context.getUser();
        if (user != null) {
            log.setOperatorId(user.getId());
        }

        repo.save(log);
    }
}
