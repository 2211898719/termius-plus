ALTER TABLE `server`
    ADD COLUMN `proxy_server_id` int UNSIGNED NULL COMMENT '代理服务器ID' AFTER `proxy_id`;
