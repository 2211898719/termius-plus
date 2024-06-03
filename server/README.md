#  如何运行本项目
1. 复制.env.dist文件为.env文件
2. 修改.env文件中的配置项
   ```
   主要是mysql和redis的配置
   ```
3. 运行即可
# 项目结构
```
├── api
│   ├── admin   # 后台管理api
│   ├── app    # app api 暂未使用
├── biz
│   ├── application # 应用
│   ├── job # 定时任务
│   ├── log # 服务器日志
│   ├── objectlog # 统一日志 不怎么用
│   ├── queue # 消息队列 暂未使用
│   ├── scheduler # 定时任务 暂未使用
│   ├── server # 服务器 核心业务
│   ├── snippet # 代码片段 
│   ├── sql # 备份sql
│   ├── storage # 文件
│   ├── user # 用户相关
│   ├── util # 工具类
├── common # 公共模块，缓存工具类等
├── config # 各种配置类啥的
├── exception # 异常处理
├── security # spring security相关
├── ws # websocket相关 核心业务
```

# 编译镜像

### 镜像默认推送到docker hub仓库
https://hub.docker.com/r/hongjunlong/termius-plus

### 首次编译
```
mvn compile jib:build -Djib.to.auth.username=xxx -Djib.to.auth.password=xxx
```
### 后续编译
```
mvn compile jib:build
```

### 镜像的版本号
是pom.xml文件中version的值


# windows远程桌面实现
#### windows 远程桌面以来与guacamole server实现
#### guacamole server可以将微软的rdp协议翻译为socket协议，可以简单的实现远程桌面功能
## 本地搭建guacamole server
#### docker 镜像为guacamole/guacd:latest
```
docker run -d --name guacd -p 4822:4822 guacamole/guacd
```

# todo
## sshj的坑
#### sshj 0.38.0版本在连接高版本的ssl服务器时，保活机制会导致连接失败，需要关闭保活机制
https://github.com/hierynomus/sshj/issues/933
https://github.com/hierynomus/sshj/pull/934
##### sshj已经合并了修复的PR，等待发布新版本

## ssh的websocket端点的连接回收问题
### 无论是直接关闭回收 还是定时任务回收，目前还是会出现幽灵连接。会导致内存溢出。
### 目前的临时解决方案是，添加了一个连接超时机制，超过一定时间没有任何消息，则关闭连接。
### 相关类`ConnectTimeoutJob`
## websocket缓冲区问题
### 为了支持rz/sz命令，需要调整websocket端点缓冲区大小。但是缓存区过大会导致工作内存爆满，导致连接断开。
### 目前的临时解决方案是，努力回收连接，减少缓存区大小。可以考虑实现动态调整缓存区大小。在使用rz/sz命令时，临时调整缓存区大小。
### 相关类`WebSocketConfig`
