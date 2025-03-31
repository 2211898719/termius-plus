ALTER TABLE `server_run_log`
    ADD COLUMN `date_diff` int(10) NULL DEFAULT NULL AFTER `network_usages`;

ALTER TABLE `server_run_log`
    ADD COLUMN `time_zone` varchar(128) NULL DEFAULT NULL AFTER `network_usages`;
