CREATE TABLE `object_log` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `level` enum('info','warn','error') NOT NULL COMMENT '日志级别(info,warn,error)',
    `type` varchar(64) NOT NULL COMMENT '对象类型',
    `oid` bigint UNSIGNED NOT NULL DEFAULT '0' COMMENT '对象ID',
    `event` varchar(32) NOT NULL COMMENT '事件名',
    `message` longtext DEFAULT NULL COMMENT '日志内容',
    `context` json DEFAULT NULL COMMENT '上下文数据',
    `operator_id` bigint UNSIGNED NOT NULL DEFAULT '0' COMMENT '操作人用户ID(0: 表示系统用户)',
    `created_at` bigint UNSIGNED NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务对象日志表';