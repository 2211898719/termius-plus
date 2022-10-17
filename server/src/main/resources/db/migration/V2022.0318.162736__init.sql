CREATE TABLE `user` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `username` varchar(128) NOT NULL COMMENT '用户名',
    `password` varchar(128) NOT NULL DEFAULT '' COMMENT '密码',
    `locked` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '是否锁定',
    `email` varchar(128) NOT NULL DEFAULT '' COMMENT '电子邮件',
    `avatar` varchar(1024) NOT NULL DEFAULT '' COMMENT '头像地址',
    `register_at` bigint unsigned NOT NULL DEFAULT '0' COMMENT '注册时间',
    `register_ip` varchar(40) NOT NULL DEFAULT '' COMMENT '注册IP',
    `login_at` bigint unsigned NOT NULL DEFAULT '0' COMMENT '最后登录时间',
    `login_ip` varchar(40) NOT NULL DEFAULT '' COMMENT '最后登录IP',
    `created_at` bigint unsigned NOT NULL,
    `updated_at` bigint unsigned NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户';

CREATE TABLE `role` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `name` varchar(128) NOT NULL COMMENT '角色名',
    `created_at` bigint unsigned NOT NULL,
    `updated_at` bigint unsigned NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色';

CREATE TABLE `user_role` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
    `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
    `created_at` bigint unsigned NOT NULL,
    `updated_at` bigint unsigned NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户拥有角色';

INSERT INTO role(id, name, created_at, updated_at) VALUES(1, 'ROLE_USER', 1647570000000, 1647570000000);
INSERT INTO role(id, name, created_at, updated_at) VALUES(2, 'ROLE_ADMIN', 1647570000000, 1647570000000);
INSERT INTO role(id, name, created_at, updated_at) VALUES(3, 'ROLE_SUPER_ADMIN', 1647570000000, 1647570000000);

INSERT INTO user(id, username, password, email, register_at, register_ip, login_at, login_ip, created_at, updated_at) VALUES(1, 'admin', '$2a$10$/bpl6SFBifLsSyOicTLF4O8FQJ1b.ilievII2TnbFixUB8NNkdCS6', 'hello@skeleton.java', 1647570000000, '127.0.0.1', 1647570000000, '127.0.0.1', 1647570000000, 1647570000000);
INSERT INTO user_role(user_id, role_id, created_at, updated_at) VALUES(1, 2, 1647570000000, 1647570000000);
INSERT INTO user_role(user_id, role_id, created_at, updated_at) VALUES(1, 3, 1647570000000, 1647570000000);

