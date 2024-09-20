-- 服务器运行日志
create table `server_run_log`
(
    `id`             bigint unsigned not null auto_increment,
    `server_id`      bigint unsigned not null comment '服务器ID',
    `date`           datetime not null comment '日期',
    `cpu_usage`      text comment 'CPU使用率',
    `memory_usage`   text comment '内存使用率',
    `disk_usages`    text comment '磁盘使用率',
    `network_usages` text comment '网络使用率',
    `created_at`     bigint unsigned not null comment '创建时间',
    `updated_at`     bigint unsigned not null comment '更新时间',
    primary key (`id`)
) ENGINE = InnoDB comment '应用监控日志'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

