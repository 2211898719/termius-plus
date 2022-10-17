package com.codeages.javaskeletonserver.biz.queue.autoconfigure;

import com.codeages.javaskeletonserver.biz.queue.annotation.QueueConsumeHandler;
import com.codeages.javaskeletonserver.biz.queue.handler.ConsumeHandler;
import com.codeages.javaskeletonserver.biz.queue.handler.ConsumeHandlerRegistry;
import com.codeages.javaskeletonserver.biz.queue.service.QueueService;
import com.codeages.javaskeletonserver.biz.queue.service.impl.QueueServiceImpl;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(value = "queue.mock", havingValue = "false", matchIfMissing = true)
public class QueueAutoConfiguration implements ApplicationContextAware, SmartInitializingSingleton {

    private ConfigurableApplicationContext applicationContext;

    private final ConsumeHandlerRegistry registry;

    private final RocketMQTemplate rocketMQTemplate;

    private final Environment environment;

    public QueueAutoConfiguration(Environment environment, ConsumeHandlerRegistry registry, RocketMQTemplate rocketMQTemplate) {
        this.environment = environment;
        this.registry = registry;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Bean
    public QueueService queueService() {
        var normalTopic = environment.getProperty("queue.rocketmq.normal-topic");
        var delayTopic = environment.getProperty("queue.rocketmq.delay-topic");
        var orderlyTopic = environment.getProperty("queue.rocketmq.orderly-topic");

        return new QueueServiceImpl(rocketMQTemplate, normalTopic, delayTopic, orderlyTopic);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.applicationContext.getBeansWithAnnotation(QueueConsumeHandler.class).forEach(
                (key, value) -> {
                    if (ScopedProxyUtils.isScopedTarget(key)) {
                        return;
                    }

                    registerContainer(key, value);
                }
        );
    }

    private void registerContainer(String beanName, Object bean) {
        Class<?> clazz = AopProxyUtils.ultimateTargetClass(bean);

        if (!ConsumeHandler.class.isAssignableFrom(bean.getClass())) {
            throw new IllegalStateException(clazz + " is not instance of " + ConsumeHandler.class.getName());
        }

        QueueConsumeHandler annotation = clazz.getAnnotation(QueueConsumeHandler.class);

        registry.register(annotation.type(), (ConsumeHandler<?>) bean, annotation.log());
    }
}
