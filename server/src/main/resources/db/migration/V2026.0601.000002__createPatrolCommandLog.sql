-- AI 巡查命令执行审计日志
CREATE TABLE `patrol_command_log`
(
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT,
    `server_id`  bigint unsigned          DEFAULT NULL COMMENT '服务器ID',
    `command`    text                     DEFAULT NULL COMMENT '执行的命令',
    `exec_type`  varchar(20)     NOT NULL COMMENT '执行类型: AUTO=白名单内自动执行, CONFIRM_PENDING=需用户确认, DANGEROUS=用户确认后执行',
    `output`     text                     DEFAULT NULL COMMENT '执行结果或确认提示（截断到 4KB）',
    `created_at` bigint unsigned NOT NULL COMMENT '记录时间',
    PRIMARY KEY (`id`),
    KEY `idx_server_id` (`server_id`),
    KEY `idx_exec_type` (`exec_type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='AI 巡查命令执行日志';
