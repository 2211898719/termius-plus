#添加userId字段
alter table `command_log` add column `user_id` int(10) unsigned not null default 0 comment '用户ID' after `session_id`;
