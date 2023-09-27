#代理表
create table `proxy`
(
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT,
    `ip`         varchar(40)     not null default '' comment '代理IP',
    `port`       int unsigned    not null default 80 comment '代理端口',
    `type`       varchar(40)     not null default 'HTTP' comment '代理类型,HTTP,SOCKS5',
    `password`   varchar(128)    not null default '' comment '密码',
    `created_at` bigint unsigned not null,
    `updated_at` bigint unsigned not null,
    primary key (id)
) engine = InnoDB
  default charset = utf8mb4
  collate = utf8mb4_general_ci comment ='代理';

#group表
create table `group`
(
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT,
    `name`       varchar(128)    not null default '' comment '组名',
    `parent_id`  bigint unsigned not null default 0 comment '父组ID',
    `proxy_id`   bigint unsigned          default null comment '代理ID',
    `created_at` bigint unsigned not null,
    `updated_at` bigint unsigned not null,
    primary key (id)
) engine = InnoDB
  default charset = utf8mb4
  collate = utf8mb4_general_ci comment ='组';

#服务器表
create table `server`
(
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT,
    `group_id`   bigint unsigned not null default 0 comment '组ID',
    `name`       varchar(128)    not null default '' comment '服务器名',
    `ip`         varchar(512)    not null default '' comment '服务器IP/host',
    `port`       int unsigned    not null default 22 comment 'ssh服务器端口',
    `password`   varchar(512)    not null default '' comment '密码',
    `key`        varchar(512)    not null default '' comment '密钥',
    `proxy_id`   bigint unsigned          default null comment '代理ID',
    `created_at` bigint unsigned not null,
    `updated_at` bigint unsigned not null,
    primary key (id)
) engine = InnoDB
  default charset = utf8mb4
  collate = utf8mb4_general_ci comment ='服务器';

#服务监控表
create table `server_service_monitor`
(
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT,
    `server_id`  bigint unsigned not null default 0 comment '服务器ID',
    `name`       varchar(128)    not null default '' comment '服务名',
    `port`       int unsigned    not null default 0 comment '端口',
    `created_at` bigint unsigned not null,
    `updated_at` bigint unsigned not null,
    primary key (id)
) engine = InnoDB
  default charset = utf8mb4
  collate = utf8mb4_general_ci comment ='服务监控';



