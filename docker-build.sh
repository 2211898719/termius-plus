#!/bin/sh
set -e

# $version是版本号
version=$(cat .version)
if [ -z "$version" ]; then
    echo "请指定版本号，在文件.version中"
    exit 1
fi

echo "=============================="
echo " 开始编译，版本号：$version"
echo "=============================="

# 1. 宿主机编译后端 JAR
echo ""
echo "[1/3] 编译后端 JAR ..."
cd server
chmod +x gradlew
./gradlew bootJar --no-daemon -x test
cd ..
echo "后端 JAR 编译完成: server/build/libs/"

# 2. 宿主机编译前端 dist
echo ""
echo "[2/3] 编译前端 dist ..."
cd web/admin
yarn install --frozen-lockfile --network-timeout 1000000
yarn run build --no-analyze
cd ../..
echo "前端 dist 编译完成: web/admin/dist/"

# 3. 构建并推送镜像
echo ""
echo "[3/3] 构建并推送 Docker 镜像 ..."
docker buildx build --push \
  -t registry.cn-hangzhou.aliyuncs.com/kuozhi/termius-plus:$version \
  --platform linux/amd64,linux/arm64 \
  --cache-from type=registry,ref=registry.cn-hangzhou.aliyuncs.com/kuozhi/termius-plus:cache \
  .

echo ""
echo "=============================="
echo " 完成: $version"
echo "=============================="
