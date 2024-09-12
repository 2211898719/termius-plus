-- 应用监控日志
create table `application_monitor_log`
(
    `id`                  bigint unsigned not null auto_increment,
    `application_id`      bigint unsigned not null comment '应用ID',
    `date`                datetime     not null comment '日期',
    `created_at`          bigint unsigned not null comment '创建时间',
    `updated_at`          bigint unsigned not null comment '更新时间',
    primary key (`id`)
) ENGINE = InnoDB comment '应用监控日志'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

