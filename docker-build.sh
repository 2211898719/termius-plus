#!/bin/sh

# $version是版本号
version=$(cat .version)
if [ -z "$version" ]; then
    echo "请指定版本号，在文件.version中"
    exit 1
fi

echo 开始编译，版本号：$version

docker buildx build --push -t registry.cn-hangzhou.aliyuncs.com/kuozhi/termius-plus:$version --platform linux/amd64,linux/arm64  .

