-- 给application_monitor表增加字段失败次数,失败时间,响应结果,响应时间


ALTER TABLE application_monitor ADD COLUMN failure_count INT(10) unsigned DEFAULT 0;
ALTER TABLE application_monitor ADD COLUMN failure_time TIMESTAMP DEFAULT NULL;
ALTER TABLE application_monitor ADD COLUMN response_result VARCHAR(255) DEFAULT NULL;
ALTER TABLE application_monitor ADD COLUMN response_time bigint unsigned DEFAULT NULL;
