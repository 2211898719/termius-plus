CREATE TABLE `patrol_conversation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `conversation_id` VARCHAR(36) NOT NULL,
    `title` VARCHAR(255) DEFAULT NULL,
    `created_at` BIGINT UNSIGNED DEFAULT NULL,
    `updated_at` BIGINT UNSIGNED DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `patrol_message` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `conversation_id` VARCHAR(36) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `content` TEXT,
    `timeline` MEDIUMTEXT,
    `sort_order` INT NOT NULL DEFAULT 0,
    `created_at` BIGINT UNSIGNED DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_sort_order` (`conversation_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `patrol_chat_memory` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `conversation_id` VARCHAR(36) NOT NULL,
    `messages_json` MEDIUMTEXT,
    `created_at` BIGINT UNSIGNED DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_memory_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
