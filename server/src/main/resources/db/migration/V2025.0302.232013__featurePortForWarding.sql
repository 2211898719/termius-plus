-- 端口转发表
create table `port_forwarding`
(
    `id`              bigint unsigned not null auto_increment,
    `server_id`       bigint unsigned not null comment '服务器ID',
    `forwarding_name` varchar(128)    not null comment '转发名称',
    `local_host`      varchar(128)    not null comment '本地主机',
    `local_port`      int unsigned    not null comment '本地端口',
    `remote_host`     varchar(128)    not null comment '远程主机',
    `remote_port`     int unsigned    not null comment '远程端口',
    `status`          varchar(32) default null comment '状态',
    `created_at`      bigint unsigned not null comment '创建时间',
    `updated_at`      bigint unsigned not null comment '更新时间',
    primary key (`id`)
) ENGINE = InnoDB comment '端口转发表'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

