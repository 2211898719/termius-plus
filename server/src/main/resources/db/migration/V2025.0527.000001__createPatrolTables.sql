-- 巡检脚本表
CREATE TABLE patrol_script (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    script_content TEXT NOT NULL,
    output_schema TEXT,
    category VARCHAR(50) NOT NULL DEFAULT 'custom',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at BIGINT,
    updated_at BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 巡检任务记录表
CREATE TABLE patrol_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    script_id BIGINT NOT NULL,
    server_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    output TEXT,
    alert_sent TINYINT(1) NOT NULL DEFAULT 0,
    executed_at DATETIME NOT NULL,
    created_at BIGINT,
    INDEX idx_script_id (script_id),
    INDEX idx_server_id (server_id),
    INDEX idx_executed_at (executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
