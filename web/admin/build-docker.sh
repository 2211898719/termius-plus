#!/bin/bash

echo "开始编译前端，版本: "$1

if [ ! -n "$1" ] ;then
    echo "请输入版本号"
    exit
fi

sudo sysctl -w kern.maxfiles=65535

sudo sysctl -w kern.maxfilesperproc=65535

ulimit -n 65536

yarn build

docker build -t registry.cn-hangzhou.aliyuncs.com/kuozhi/termius-plus:front-$1 .

docker login registry.cn-hangzhou.aliyuncs.com/kuozhi/termius-plus

docker push registry.cn-hangzhou.aliyuncs.com/kuozhi/termius-plus:front-$1


