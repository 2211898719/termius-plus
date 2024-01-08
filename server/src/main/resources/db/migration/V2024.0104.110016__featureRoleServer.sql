#添加一个字段表示角色拥有的服务器权限,json格式
alter table `role` add column `server_permission` text default null comment '服务器权限' after `name`;
