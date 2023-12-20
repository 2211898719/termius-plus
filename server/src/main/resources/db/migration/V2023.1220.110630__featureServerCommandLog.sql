create table `command_log`
(
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT,
    `session_id`   varchar(256)    not null default '' comment '会话id',
    `server_id`    bigint unsigned not null default 0 comment '服务器id',
    `command_data` longtext                 default null comment '命令数据',
    `created_at`   bigint unsigned not null,
    `updated_at`   bigint unsigned not null,
    primary key (id)
) engine = InnoDB
  default charset = utf8mb4
  collate = utf8mb4_general_ci comment ='命令执行日志';
