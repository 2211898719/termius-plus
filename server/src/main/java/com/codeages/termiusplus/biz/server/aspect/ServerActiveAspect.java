package com.codeages.termiusplus.biz.server.aspect;

import com.codeages.termiusplus.biz.server.annotation.SftpActive;
import com.codeages.termiusplus.biz.server.dto.SFTPBean;
import com.codeages.termiusplus.biz.server.service.SFTPService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class ServerActiveAspect {

    @Autowired
    private SFTPService sftpService;

    @Pointcut("execution(public * com.codeages.termiusplus.biz.server.service.SFTPService.*(..))")
    public void webLog() {
    }

    @Before("webLog()")
    public void doBefore() {
    }

    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        // 获取执行方法
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        SftpActive sftpActive = method.getAnnotation(SftpActive.class);
        if (sftpActive == null) {
            return joinPoint.proceed();
        }

        String value = sftpActive.value();

        Object[] args = joinPoint.getArgs();
        ExpressionParser parser = new SpelExpressionParser();
        LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
        String[] params = discoverer.getParameterNames(method);
        EvaluationContext context = new StandardEvaluationContext();
        for (int len = 0; len < params.length; len++) {
            context.setVariable(params[len], args[len]);
        }
        Expression expression = parser.parseExpression(value);
        value = expression.getValue(context, String.class);

        log.info("sftp连接在激活状态，连接信息：{}", value);

        SFTPBean sftpBean = sftpService.getSftpBean(value);
        sftpBean.setActive(true);
        Object proceed = joinPoint.proceed();
        sftpBean.setActive(false);

        return proceed;
    }

}
