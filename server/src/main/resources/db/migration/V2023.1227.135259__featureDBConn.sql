CREATE TABLE `db_conn`
(
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT,
    `conn_name`     varchar(32)     NOT NULL comment '连接名称',
    `username`      varchar(32)     NOT NULL comment '用户名',
    `password`      varchar(32)     NOT NULL comment '密码',
    `host`          varchar(64)     NOT NULL comment '主机',
    `port`          varchar(6)      NOT NULL comment '端口',
    `save_password` tinyint(1)      NOT NULL DEFAULT 0 comment '是否保存密码',
    `created_at`    bigint unsigned not null comment '创建时间',
    `updated_at`    bigint unsigned not null comment '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
