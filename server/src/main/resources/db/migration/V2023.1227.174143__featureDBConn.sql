#添加一个字段表示db端口，多个用逗号分隔
alter table `server`
    add column `db_port` varchar(256) not null default '';
