# 宿主机已完成编译,此镜像只负责组装最终产物
FROM registry.cn-hangzhou.aliyuncs.com/education-portal/termius-plus:jdk21-nginx-arthas
WORKDIR /app
COPY server/build/libs/*.jar ./app.jar
COPY web/admin/dist /app/front
COPY web/admin/docker/nginx.conf /etc/nginx/nginx.conf
COPY web/admin/docker/99-replace-vue-env-var-placeholders.sh /docker-entrypoint.d/
RUN chmod +x /docker-entrypoint.d/99-replace-vue-env-var-placeholders.sh
ENV LANGUAGE=en_US:en LANG=C.UTF-8 LC_ALL=C.UTF-8
EXPOSE 80 8080
CMD ["sh", "-c", "service nginx restart && exec java -jar -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 /app/app.jar"]
