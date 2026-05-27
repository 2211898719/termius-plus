-- 巡检脚本表
create table `patrol_script`
(
    `id`              bigint unsigned not null auto_increment,
    `name`            varchar(255)    not null comment '脚本名称',
    `description`     text                     default null comment '脚本描述',
    `script_content`  text            not null comment '脚本内容',
    `output_schema`   text                     default null comment '输出格式定义',
    `category`        varchar(50)     not null default 'custom' comment '脚本分类',
    `enabled`         tinyint(1)      not null default 1 comment '是否启用',
    `created_at`      bigint unsigned not null comment '创建时间',
    `updated_at`      bigint unsigned not null comment '更新时间',
    primary key (`id`)
) ENGINE = InnoDB comment '巡检脚本表'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- 巡检任务记录表
create table `patrol_task`
(
    `id`              bigint unsigned not null auto_increment,
    `script_id`       bigint unsigned not null comment '巡检脚本ID',
    `server_id`       bigint unsigned not null comment '服务器ID',
    `status`          varchar(20)     not null comment '任务状态',
    `output`          text                     default null comment '执行输出',
    `alert_sent`      tinyint(1)      not null default 0 comment '是否已发送告警',
    `executed_at`     datetime        not null comment '执行时间',
    `created_at`      bigint unsigned not null comment '创建时间',
    `updated_at`      bigint unsigned not null comment '更新时间',
    primary key (`id`),
    index `idx_script_id` (`script_id`),
    index `idx_server_id` (`server_id`),
    index `idx_executed_at` (`executed_at`)
) ENGINE = InnoDB comment '巡检任务记录表'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
