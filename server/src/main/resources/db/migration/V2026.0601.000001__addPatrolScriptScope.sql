-- 巡检脚本增加服务器范围字段
ALTER TABLE `patrol_script`
    ADD COLUMN `server_ids` text NULL COMMENT '指定服务器ID列表 (JSON 数组)，空表示不限定';
ALTER TABLE `patrol_script`
    ADD COLUMN `group_ids` text NULL COMMENT '指定分组ID列表 (JSON 数组)，空表示不限定';
