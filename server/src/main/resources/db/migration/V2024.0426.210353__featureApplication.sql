# application：应用
create table `application`
(
    `id`         bigint unsigned not null auto_increment,
    `parent_id`  bigint unsigned not null default 0 comment '父级ID',
    `is_group`   tinyint(1)      not null default 0 comment '是否为分组',
    `sort`       bigint unsigned not null default 0 comment '排序',
    `name`       varchar(32)     not null comment '应用名称',
    `icon`       varchar(128)             default null comment '应用图标',
    `content`    text                     default null comment '应用详情/链接',
    `identity`   longtext                 default null comment '应用身份',
    `created_at` bigint unsigned not null comment '创建时间',
    `updated_at` bigint unsigned not null comment '更新时间',
    primary key (`id`)
) ENGINE = InnoDB comment '应用'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

# 应用服务器
create table `application_server`
(
    `id`             bigint unsigned not null auto_increment,
    `application_id` bigint unsigned not null comment '应用ID',
    `server_id`      bigint unsigned not null comment '服务器ID',
    `tag`            varchar(64) default null comment '标签',
    `remark`         longtext    default null comment '备注',
    `created_at`     bigint unsigned not null comment '创建时间',
    `updated_at`     bigint unsigned not null comment '更新时间',
    primary key (`id`)
) ENGINE = InnoDB comment '应用服务器'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

# 应用监控
create table `application_monitor`
(
    `id`             bigint unsigned not null auto_increment,
    `application_id` bigint unsigned not null comment '应用ID',
    `type`           varchar(64) not null comment '监控类型',
    `config`         longtext    not null comment '监控配置',
    `remark`         longtext    default null comment '备注',
    `created_at`     bigint unsigned not null comment '创建时间',
    `updated_at`     bigint unsigned not null comment '更新时间',
    primary key (`id`)
) ENGINE = InnoDB comment '应用监控'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

