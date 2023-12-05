#添加一个字段表示可能有windows服务器，linux服务器
alter table `server`
    add column `os` varchar(64) not null default 'LINUX' after `ip`;

