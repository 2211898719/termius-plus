#给server表加parent_id字段，is_group字段
alter table server add column parent_id bigint unsigned not null default 0 comment '父组ID';
alter table server add column is_group tinyint unsigned not null default 0 comment '是否为组';
