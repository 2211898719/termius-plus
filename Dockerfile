FROM registry.cn-hangzhou.aliyuncs.com/kuozhi/termius-plus:build as builder
#ENV NVM_INC=/root/.nvm/versions/node/v14.17.1/include/node
ENV NVM_INC=/root/.nvm/versions/node/v16.20.2/include/node

ENV JAVA_HOME=/root/.sdkman/candidates/java/11.0.24-amzn
#ENV JAVA_HOME=/root/.sdkman/candidates/java/17.0.12-amzn
#ENV JAVA_HOME=/root/.sdkman/candidates/java/21.0.2-open

ENV GRADLE_HOME=/root/.sdkman/candidates/gradle/current
ENV SDKMAN_CANDIDATES_DIR=/root/.sdkman/candidates
ENV NVM_DIR=/root/.nvm
ENV TERM=xterm
#ENV MAVEN_HOME=/root/.sdkman/candidates/maven/3.9.9
ENV MAVEN_HOME=/root/.sdkman/candidates/maven/3.8.4
ENV SDKMAN_DIR=/root/.sdkman
ENV SHLVL=1
ENV SDKMAN_CANDIDATES_API=https://api.sdkman.io/2
ENV NVM_NODEJS_ORG_MIRROR=http://npmmirror.com/mirrors/node/
ENV PATH=/root/.sdkman/candidates/maven/3.8.4/bin:/root/.sdkman/candidates/java/11.0.24-amzn/bin:/root/.sdkman/candidates/gradle/current/bin:/root/.nvm/versions/node/v16.20.2/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
ENV NVM_BIN=/root/.nvm/versions/node/v16.20.2/bin

WORKDIR /opt/code/
COPY ./ /opt/code/

WORKDIR /opt/code/server

RUN mvn compile jib:build  -Djib.to.auth.username=${USERNAME} -Djib.to.auth.password=${password}

WORKDIR /opt/code/web/admin

RUN yarn && yarn build

# 第二阶段，用于运行应用程序
FROM nginx:alpine-slim as admin
WORKDIR /front
COPY --from=builder /opt/code/web/admin/dist /front
COPY --from=builder /opt/code/web/admin/docker/nginx.conf /etc/nginx/nginx.conf
EXPOSE 8082
