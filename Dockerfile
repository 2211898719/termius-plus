# 第一阶段：构建 Vue.js 前端（优化缓存）
FROM registry.cn-hangzhou.aliyuncs.com/education-portal/termius-plus:node-18 AS frontend-build

# 1. 先单独复制依赖描述文件（利用缓存层）
WORKDIR /app/admin
COPY web/admin/package.json web/admin/yarn.lock* ./
RUN npm config set registry https://registry.npmmirror.com/ && \
    yarn install --frozen-lockfile --network-timeout 1000000

# 2. 再复制源码并构建（源码变动时才重新构建）
COPY web /app
RUN yarn run build --no-analyze

# 第二阶段：构建 Java 后端（优化缓存）
FROM registry.cn-hangzhou.aliyuncs.com/education-portal/termius-plus:maven-jdk21 AS backend-build

# 1. 预先复制 Maven 配置和 POM 文件（利用缓存层）
COPY settings.xml /root/.m2/settings.xml
WORKDIR /app
COPY server/pom.xml .
RUN mvn dependency:go-offline -B

# 2. 再复制完整代码并编译（代码变动时才重新构建）
COPY server .
RUN mvn package -DskipTests

# 第三阶段：最终镜像（保持不变）
FROM registry.cn-hangzhou.aliyuncs.com/education-portal/termius-plus:jdk21-nginx-arthas
WORKDIR /app
COPY --from=backend-build /app/target/*.jar ./app.jar
COPY --from=frontend-build /app/admin/dist /app/front
COPY ./web/admin/docker/nginx.conf /etc/nginx/nginx.conf
ENV LANGUAGE en_US:en LANG=C.UTF-8 LC_ALL=C.UTF-8
EXPOSE 80 8080
CMD service nginx restart && java -jar -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 /app/app.jar