ALTER TABLE `server_run_log`
    ADD COLUMN `ntp` tinyint(1) NULL DEFAULT NULL AFTER `network_usages`;