create table `command`
(
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT,
    `name`       varchar(256)    not null default '' comment '命令名称',
    `command`    varchar(256)    not null default '' comment '命令',
    `remark`     longtext                 default null comment '备注',
    `created_at` bigint unsigned not null,
    `updated_at` bigint unsigned not null,
    primary key (id)
) engine = InnoDB
  default charset = utf8mb4
  collate = utf8mb4_general_ci comment ='命令';
