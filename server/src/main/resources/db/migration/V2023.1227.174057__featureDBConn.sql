#添加一个字段表示是否db服务器
alter table `server`
    add column `is_db` tinyint(1) not null default 0 comment '是否db服务器';
