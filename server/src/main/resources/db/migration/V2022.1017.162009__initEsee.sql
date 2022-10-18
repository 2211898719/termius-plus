CREATE TABLE `file`
(
    `id`          int(10) unsigned                 NOT NULL AUTO_INCREMENT COMMENT 'id',
    `target_type` varchar(32)                      not NULL COMMENT '所属分类',
    `name`        varchar(1024) CHARACTER SET utf8 NOT NULL COMMENT '文件原名',
    `ext`         varchar(16) CHARACTER SET utf8   NOT NULL COMMENT '文件格式',
    `size`        int(10)                          NOT NULL COMMENT '文件大小',
    `user_id`     int(10) DEFAULT NULL COMMENT '上传人',
    `uri`         varchar(512) CHARACTER SET utf8  NOT NULL COMMENT '存储地址',
    `uuid`        varchar(32) CHARACTER SET utf8   NOT NULL COMMENT 'uuid',
    `created_at`  bigint(20) unsigned              NOT NULL,
    `updated_at`  bigint(20) unsigned              NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='文件';
