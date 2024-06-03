# 功能列表
## 服务器组列表
![img.png](images/img.png)
![img.png](images/img0.png)
![img_1.png](images/img_1.png)

## 连接服务器
### 终端
![img_2.png](images/img_2.png)
### 支持为每个服务器单独记录文档
![img_9.png](images/img_9.png)
### 执行自定义命令
![img_10.png](images/img_10.png)
### 内置linux命令文档，可以快速查看命令用法
![img_24.png](images/img_24.png)
### sftp
![img_3.png](images/img_3.png)
#### 基本文件操作
![img_11.png](images/img_11.png)
![img_12.png](images/img_12.png)
##### 分屏拖拽可直接服务器对服务器传递文件
ps:多浏览器标签页也可以
![img_13.png](images/img_13.png)

## 权限管理
##### 基于角色赋予服务器权限，支持多角色，多用户管理
![img_25.png](images/img_25.png)
![img_26.png](images/img_26.png)

## 主题色
![img_4.png](images/img_4.png)

## 自定义命令片段
![img_5.png](images/img_5.png)
![img_6.png](images/img_6.png)


## 自定义代理
![img_7.png](images/img_7.png)
![img_8.png](images/img_8.png)
### 支持组上设置代理,服务器连接时会就近使用
![img_8.png](images/img_92.png)

## 应用
目前主要是网站，后续支持更多类型应用
![img_14.png](images/img_14.png)
### 信息
#### 记录应用各种信息，其中的负责人手机号用于后续监控告警
![img_15.png](images/img_15.png)
### 身份
#### 记录应用的账号/密码等身份信息，用于后续登录等维护
![img_16.png](images/img_16.png)
### 监控
#### 可以对应用的一个端点进行监控，并使用正则判断是否有异常，并发送钉钉通知
![img_17.png](images/img_17.png)
##### 监控效果，如果异常会显示异常时间，并发送钉钉通知
![img_21.png](images/img_21.png)
### 服务器
#### 记录应用所用服务器，用于后续服务器运维。也可以实现应用架构图
![img_20.png](images/img_20.png)
##### 架构图
![img_18.png](images/img_18.png)
![img_19.png](images/img_19.png)

## 端口转发
### 支持动态端口转发，方便直接访问服务,如内部的mysql，redis等。或不公开的站点
![img_22.png](images/img_22.png)

## 定时任务
#### 使用类java语言的mvel表达式脚本，支持cron表达式的定时任务
##### 提供全局变量session，可以直接访问服务器执行命令。
##### DingerRequest对象，可以发送钉钉通知
![img_23.png](images/img_23.png)

## 日志
#### 记录每次操作的日志，方便后续问题排查
##### 日志采用缓存区+文件的方式，避免频繁写入磁盘，提高性能
![img_27.png](images/img_27.png)
![img_28.png](images/img_28.png)


