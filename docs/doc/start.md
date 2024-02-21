---
outline: deep
---

# docker 安装

## 创建一个docker-compose.yml
内容如下，包含了服务端、前端、redis、mysql、guacd服务。
如果要用外部的 mysql 服务和 redis 服务可以按需修改。

```
version: "3"

services:
  server:
    image: registry.cn-hangzhou.aliyuncs.com/education-portal/termius-plus:server-${VERSION:?error}
    restart: unless-stopped
    environment:
      - TZ=Asia/Shanghai
    env_file:
      - server.env
    volumes:
      - file_data:/var/www/data
    ports:
      - "8200-8500:8200-8500"
    networks:
      - edu-admin
    depends_on:
      - redis
      - mysql
      - some-guacd
  front:
    image: registry.cn-hangzhou.aliyuncs.com/education-portal/termius-plus:front-${VERSION:?error}
    restart: unless-stopped
    environment:
      - TZ=Asia/Shanghai
    ports:
      - 9999:80
    networks:
      - edu-admin
    depends_on:
      - server

  redis:
    image: redis:5
    container_name: redis
    command: redis-server --appendonly yes
    networks:
      - edu-admin
  mysql:
    image: mysql:8
    container_name: db
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci --init-file /docker-entrypoint-initdb.d/init.sql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
    networks:
      - edu-admin
    volumes:
      - ./mysql-data:/var/lib/mysql
      - ./mysql-data:/var/log/mysql
      - ./mysql-data:/etc/mysql/conf.d
      - ./mysql-init:/docker-entrypoint-initdb.d
  some-guacd:
    image: guacamole/guacd
    container_name: some-guacd
    restart: always
    volumes:
      - /tmp:/tmp
    networks:
      - edu-admin

networks:
  edu-admin:
    driver: bridge

volumes:
  file_data:
```

## 创建一个 .env 文件
内容如下，记录应用版本号
```
VERSION=v1.2.5
```

## 创建一个 server.env 文件
内容如下，记录服务端的配置
```

DB_HOST: db # mysql 服务的地址
DB_PORT: 3306 # mysql 服务的端口
DB_NAME: termius-plus # 数据库名称
DB_USER: root # 数据库用户名
DB_PASSWORD: root # 数据库密码

REDIS_DATABASE: 0 # redis 数据库
REDIS_HOST: redis # redis 服务的地址
REDIS_PORT: 6379 # redis 服务的端口

CURRENT_IP: 127.0.0.1 # 当前服务器地址，用于端口转发绑定

DINGTALK_SECRET: PkiCrZs1ocp2dp3vu9eqh35zhUt8wxcdH4GlfNAjday0PyzLltq5YFgoFnGVi2BH # 钉钉机器人的 secret,定时任务的通知
DINGTALK_TOKEN_ID: 25237a4fbf8d819aff4d5081781338e77ca594bb94d611de1253d242c04b02b4 # 钉钉机器人的 token id

PORT_FOR_WARDING_MIN: 8200 # 端口转发的最小端口 与 docker-compose.yml 中的端口对应
PORT_FOR_WARDING_MAX: 8500 # 端口转发的最大端口
GUACAMOLE_HOST: some-guacd # guacd 服务的地址
GUACAMOLE_PORT: 4822  # guacd 服务的端口
GUACAMOLE_MAPPING:/tmp # rdp 连接时映射到服务器的目录
```
## 启动
```bash
docker-compose up -d
```

## 后续升级
修改 .env 文件中的版本号，然后重新启动即可。

