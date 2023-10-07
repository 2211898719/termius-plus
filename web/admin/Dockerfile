FROM nginx:alpine-slim as admin
WORKDIR /front
COPY ./dist /front
COPY docker/nginx.conf /etc/nginx/nginx.conf
EXPOSE 8082
