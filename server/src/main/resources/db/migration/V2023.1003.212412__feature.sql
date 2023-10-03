#添加一个服务器连接时执行命令
alter table server add column first_command varchar(256) default null;
