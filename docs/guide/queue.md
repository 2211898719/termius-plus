# 消息队列

脚手架集成了 RocketMQ 作为消息队列服务，并提供了 `QueueService` ，用于发送消息。

## 启动 RocketMQ 服务

TODO.

## 本地开发环境配置

`application-dev.properties` 配置文件，如果不存在，则在`application.properties`相同目录下手动创建一个。


**开启消息队列：**

编辑 `application-dev.properties` ，修改以下配置：
```
rocketmq.name-server=127.0.0.1:9876
queue.mock=false
```

**关闭消息队列：**

编辑 `application-dev.properties` ，修改以下配置：
```
# rocketmq.name-server=127.0.0.1:9876 # 注释掉这一行
queue.mock=true
```
