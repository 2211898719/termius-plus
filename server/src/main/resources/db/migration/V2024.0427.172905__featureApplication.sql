ALTER TABLE `application_monitor`
    MODIFY COLUMN `response_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL AFTER `failure_time`;
