#给server表加sort字段
alter table server add column sort bigint unsigned not null default 0;
