# 介绍
模仿termius的服务器运维工具，实现了termius的80%的重要功能，后续会加入对服务器监控，服务监控等，故称为plus

# 目前实现功能
## 服务器组列表
![img.png](docs/img.png)
![img.png](docs/img0.png)
![img_1.png](docs/img_1.png)

## 连接服务器
### 终端
![img_2.png](docs/img_2.png)
### 支持为每个服务器单独记录文档
![img_9.png](docs/img_9.png)
### 执行自定义命令
![img_10.png](docs/img_10.png)
### sftp
![img_3.png](docs/img_3.png) 
#### 基本文件操作
![img_11.png](docs/img_11.png)
![img_12.png](docs/img_12.png)
![img_13.png](docs/img_13.png)


## 主题色
![img_4.png](docs/img_4.png)

## 自定义命令片段
![img_5.png](docs/img_5.png)
![img_6.png](docs/img_6.png)


## 自定义代理
![img_7.png](docs/img_7.png)
![img_8.png](docs/img_8.png)

# 部署安装
## docker

```shell
git clone https://gitee.com/zimehjl/termius-plus.git
cd termius-plus/scripts/docker
#修改server.env文件中的配置WEBSSH_URL为你的服务器地址
docker-compose up -d
#根据docker-compose.yml中的front配置，访问http://ip:9999
```

