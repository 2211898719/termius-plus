---
outline: deep
---

# Termius Plus 采用 Spring Boot + Vue + Mysql + Redis 的技术栈

## 前端
ui框架 ant-design-vue

终端部分采用 xterm.js

代码编辑器 codemirror

### 进入前端目录
cd web/admin
### 安装依赖
yarn install
### 启动
yarn serve

## 后端
持久层框架 spring data jpa，querydsl

工具类 hutool

ssh连接，sftp，端口转发等功能使用 sshj

数据库版本管理 flyway

钉钉机器人通知 dinger

rdp连接 guacamole

### 运行
建立数据库，要求 mysql8.0+

复制 application-dev.properties.dist 为 application-dev.properties , 修改其中的数据库配置，启动！