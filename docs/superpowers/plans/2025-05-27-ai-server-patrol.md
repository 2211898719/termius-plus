# AI 服务器巡查 Agent 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 AI 驱动的服务器巡查系统，包括定时自动巡检（确定性脚本执行）和交互式 AI 排查（langchain4j Agent）

**Architecture:** 定时巡检由预生成 shell 脚本驱动（不调用 AI），交互式排查由 langchain4j Agent 自主推理调用 @Tool 工具。白名单机制控制命令执行权限。

**Tech Stack:** Spring Boot 3.3.4, Java 21, langchain4j, DeepSeek, sshj, Quartz, JPA/Flyway, Vue 3, ant-design-vue, WebSocket

---

## 文件结构

### 新增后端文件

| 文件路径 | 职责 |
|---------|------|
| `biz/patrol/entity/PatrolScript.java` | 巡检脚本 JPA 实体 |
| `biz/patrol/entity/PatrolTask.java` | 巡检任务记录 JPA 实体 |
| `biz/patrol/dto/PatrolScriptCreateParams.java` | 创建脚本请求参数 |
| `biz/patrol/dto/PatrolScriptUpdateParams.java` | 更新脚本请求参数 |
| `biz/patrol/dto/PatrolScriptDto.java` | 脚本 DTO |
| `biz/patrol/dto/PatrolTaskDto.java` | 任务记录 DTO |
| `biz/patrol/dto/PatrolScriptSearchParams.java` | 脚本搜索参数 |
| `biz/patrol/dto/PatrolTaskSearchParams.java` | 任务搜索参数 |
| `biz/patrol/repository/PatrolScriptRepository.java` | 脚本 Repository |
| `biz/patrol/repository/PatrolTaskRepository.java` | 任务 Repository |
| `biz/patrol/mapper/PatrolScriptMapper.java` | 脚本 MapStruct Mapper |
| `biz/patrol/mapper/PatrolTaskMapper.java` | 任务 MapStruct Mapper |
| `biz/patrol/service/PatrolScriptService.java` | 脚本管理接口 |
| `biz/patrol/service/PatrolEngine.java` | 巡检引擎接口 |
| `biz/patrol/service/PatrolScriptServiceImpl.java` | 脚本管理实现 |
| `biz/patrol/service/PatrolEngineImpl.java` | 巡检引擎实现 |
| `biz/patrol/job/PatrolScheduledJob.java` | 定时巡检 Quartz 任务 |
| `biz/patrol/agent/PatrolAgentService.java` | 交互式 Agent 服务（langchain4j） |
| `biz/patrol/agent/tool/ExecuteCommandTool.java` | 执行命令工具 |
| `biz/patrol/agent/tool/NginxTool.java` | Nginx 相关工具 |
| `biz/patrol/agent/tool/DiskTool.java` | 磁盘相关工具 |
| `biz/patrol/agent/tool/ServiceTool.java` | 服务状态工具 |
| `biz/patrol/agent/tool/CleanupTool.java` | 清理文件工具 |
| `api/admin/PatrolController.java` | 巡查 API 控制器 |

### 新增前端文件

| 文件路径 | 职责 |
|---------|------|
| `api/patrol.js` | 巡查 API 封装 |
| `components/p-patrol-panel.vue` | AI 巡查对话面板组件 |
| `components/p-patrol-script-list.vue` | 巡查脚本列表组件 |
| `components/p-patrol-result.vue` | 巡查结果展示组件 |
| `views/server/PatrolPage.vue` | 巡查管理页面 |

### 修改的文件

| 文件路径 | 修改内容 |
|---------|---------|
| `web/admin/src/views/server/ServerContent.vue` | 在终端界面添加 AI 面板入口 |
| `web/admin/src/views/server/DashboardPage.vue` | 添加 AI 巡查 Tab |
| `server/src/main/resources/application.yml` | 添加 patrol 配置节 |
| `server/src/main/resources/db/migration/V2025_0527_000001__createPatrolTables.sql` | Flyway 建表 |

---

## Task 1: 数据库迁移 — 建表

**Files:**
- Create: `server/src/main/resources/db/migration/V2025_0527_000001__createPatrolTables.sql`

- [ ] **Step 1: 编写 Flyway 迁移脚本**

```sql
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
```

- [ ] **Step 2: 确认迁移文件命名符合现有规范**

现有规范为 `V{YYYY}.{MMDD}.{HHMMSS}__{description}.sql`，确认文件名为 `V2025.0527.000001__createPatrolTables.sql`。

---

## Task 2: 实体层 — PatrolScript 和 PatrolTask

**Files:**
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/entity/PatrolScript.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/entity/PatrolTask.java`

- [ ] **Step 1: 创建 PatrolScript 实体**

```java
package com.codeages.termiusplus.biz.patrol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PatrolScript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String scriptContent;

    @Column(columnDefinition = "TEXT")
    private String outputSchema;

    @Column(nullable = false, length = 50)
    private String category = "custom";

    @Column(nullable = false)
    private Boolean enabled = true;

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;
}
```

- [ ] **Step 2: 创建 PatrolTask 实体**

```java
package com.codeages.termiusplus.biz.patrol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
    @Index(name = "idx_script_id", columnList = "scriptId"),
    @Index(name = "idx_server_id", columnList = "serverId"),
    @Index(name = "idx_executed_at", columnList = "executedAt")
})
public class PatrolTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long scriptId;

    private Long serverId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(nullable = false)
    private Boolean alertSent = false;

    @Temporal(TemporalType.TIMESTAMP)
    private Date executedAt;

    @CreatedDate
    private Long createdAt;
}
```

---

## Task 3: DTO 层

**Files:**
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/dto/PatrolScriptCreateParams.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/dto/PatrolScriptUpdateParams.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/dto/PatrolScriptDto.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/dto/PatrolTaskDto.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/dto/PatrolScriptSearchParams.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/dto/PatrolTaskSearchParams.java`

- [ ] **Step 1: 创建 PatrolScriptCreateParams**

```java
package com.codeages.termiusplus.biz.patrol.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatrolScriptCreateParams {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String scriptContent;
    private String outputSchema;
    private String category = "custom";
}
```

- [ ] **Step 2: 创建 PatrolScriptUpdateParams**

```java
package com.codeages.termiusplus.biz.patrol.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatrolScriptUpdateParams {
    @NotNull
    private Long id;
    private String name;
    private String description;
    private String scriptContent;
    private String outputSchema;
    private String category;
    private Boolean enabled;
}
```

- [ ] **Step 3: 创建 PatrolScriptDto**

```java
package com.codeages.termiusplus.biz.patrol.dto;

import lombok.Data;

@Data
public class PatrolScriptDto {
    private Long id;
    private String name;
    private String description;
    private String scriptContent;
    private String outputSchema;
    private String category;
    private Boolean enabled;
    private Long createdAt;
    private Long updatedAt;
}
```

- [ ] **Step 4: 创建 PatrolTaskDto**

```java
package com.codeages.termiusplus.biz.patrol.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class PatrolTaskDto {
    private Long id;
    private Long scriptId;
    private Long serverId;
    private String status;
    private String output;
    private Boolean alertSent;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date executedAt;
    private String scriptName;
    private String serverName;
}
```

- [ ] **Step 5: 创建搜索参数类**

```java
package com.codeages.termiusplus.biz.patrol.dto;

import com.codeages.termiusplus.common.PagerRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PatrolScriptSearchParams extends PagerRequest {
    private String category;
    private String keyword;
}
```

```java
package com.codeages.termiusplus.biz.patrol.dto;

import com.codeages.termiusplus.common.PagerRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PatrolTaskSearchParams extends PagerRequest {
    private Long scriptId;
    private Long serverId;
    private String status;
}
```

---

## Task 4: Repository 和 Mapper 层

**Files:**
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/repository/PatrolScriptRepository.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/repository/PatrolTaskRepository.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/mapper/PatrolScriptMapper.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/mapper/PatrolTaskMapper.java`

- [ ] **Step 1: 创建 Repository 接口**

```java
package com.codeages.termiusplus.biz.patrol.repository;

import com.codeages.termiusplus.biz.patrol.entity.PatrolScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

public interface PatrolScriptRepository extends JpaRepository<PatrolScript, Long>,
        QuerydslPredicateExecutor<PatrolScript> {
    List<PatrolScript> findAllByEnabledTrue();
    List<PatrolScript> findByCategory(String category);
}
```

```java
package com.codeages.termiusplus.biz.patrol.repository;

import com.codeages.termiusplus.biz.patrol.entity.PatrolTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Date;
import java.util.List;

public interface PatrolTaskRepository extends JpaRepository<PatrolTask, Long>,
        QuerydslPredicateExecutor<PatrolTask> {
    List<PatrolTask> findByServerIdAndExecutedAtAfter(Long serverId, Date after);
    List<PatrolTask> findByScriptId(Long scriptId);
    List<PatrolTask> findByStatusIn(List<String> statuses);
}
```

- [ ] **Step 2: 创建 MapStruct Mapper**

```java
package com.codeages.termiusplus.biz.patrol.mapper;

import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptCreateParams;
import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptDto;
import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptUpdateParams;
import com.codeages.termiusplus.biz.patrol.entity.PatrolScript;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface PatrolScriptMapper {
    PatrolScriptDto toDto(PatrolScript entity);
    PatrolScript toCreateEntity(PatrolScriptCreateParams params);
    PatrolScript toUpdateEntity(@MappingTarget PatrolScript entity, PatrolScriptUpdateParams params);
}
```

```java
package com.codeages.termiusplus.biz.patrol.mapper;

import com.codeages.termiusplus.biz.patrol.dto.PatrolTaskDto;
import com.codeages.termiusplus.biz.patrol.entity.PatrolTask;
import org.mapstruct.Mapper;

@Mapper
public interface PatrolTaskMapper {
    PatrolTaskDto toDto(PatrolTask entity);
}
```

---

## Task 5: 脚本生成 Service — AI 生成巡检脚本

**Files:**
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/service/PatrolScriptService.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/service/PatrolScriptServiceImpl.java`

- [ ] **Step 1: 定义 PatrolScriptService 接口**

```java
package com.codeages.termiusplus.biz.patrol.service;

import com.codeages.termiusplus.biz.patrol.dto.*;

import java.util.List;

public interface PatrolScriptService {
    PatrolScriptDto create(PatrolScriptCreateParams params);
    PatrolScriptDto update(PatrolScriptUpdateParams params);
    void delete(Long id);
    PatrolScriptDto findById(Long id);
    List<PatrolScriptDto> findAll();
    List<PatrolScriptDto> findEnabled();
    PatrolScriptDto generateScript(String description);
}
```

- [ ] **Step 2: 实现 PatrolScriptServiceImpl**

```java
package com.codeages.termiusplus.biz.patrol.service.impl;

import com.codeages.termiusplus.biz.patrol.dto.*;
import com.codeages.termiusplus.biz.patrol.entity.PatrolScript;
import com.codeages.termiusplus.biz.patrol.mapper.PatrolScriptMapper;
import com.codeages.termiusplus.biz.patrol.repository.PatrolScriptRepository;
import com.codeages.termiusplus.biz.patrol.service.PatrolScriptService;
import com.codeages.termiusplus.exception.AppException;
import com.codeages.termiusplus.biz.ErrorCode;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatrolScriptServiceImpl implements PatrolScriptService {

    private final PatrolScriptRepository repository;
    private final PatrolScriptMapper mapper;
    private final ScriptGeneratorAiService scriptGeneratorAiService;

    @Override
    public PatrolScriptDto create(PatrolScriptCreateParams params) {
        PatrolScript entity = mapper.toCreateEntity(params);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public PatrolScriptDto update(PatrolScriptUpdateParams params) {
        PatrolScript entity = repository.findById(params.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        mapper.toUpdateEntity(entity, params);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public PatrolScriptDto findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    @Override
    public List<PatrolScriptDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PatrolScriptDto> findEnabled() {
        return repository.findAllByEnabledTrue().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PatrolScriptDto generateScript(String description) {
        String result = scriptGeneratorAiService.generate(description);
        // 解析 AI 返回的内容，提取脚本和 schema
        PatrolScriptCreateParams params = parseAiResult(result, description);
        // 不直接保存，返回给用户审核
        PatrolScriptDto dto = new PatrolScriptDto();
        dto.setName(params.getName());
        dto.setDescription(params.getDescription());
        dto.setScriptContent(params.getScriptContent());
        dto.setOutputSchema(params.getOutputSchema());
        dto.setCategory(params.getCategory());
        return dto;
    }

    private PatrolScriptCreateParams parseAiResult(String aiResult, String originalDescription) {
        // AI 返回格式：
        // ---NAME---
        // 脚本名称
        // ---DESCRIPTION---
        // 脚本描述
        // ---SCRIPT---
        // #!/bin/bash ...
        // ---SCHEMA---
        // {"type":"object",...}
        // ---CATEGORY---
        // disk
        PatrolScriptCreateParams params = new PatrolScriptCreateParams();
        String[] sections = aiResult.split("---");
        for (int i = 0; i < sections.length; i++) {
            String section = sections[i].trim();
            switch (section) {
                case "NAME":
                    if (i + 1 < sections.length) params.setName(sections[++i].trim());
                    break;
                case "DESCRIPTION":
                    if (i + 1 < sections.length) params.setDescription(sections[++i].trim());
                    break;
                case "SCRIPT":
                    if (i + 1 < sections.length) params.setScriptContent(sections[++i].trim());
                    break;
                case "SCHEMA":
                    if (i + 1 < sections.length) params.setOutputSchema(sections[++i].trim());
                    break;
                case "CATEGORY":
                    if (i + 1 < sections.length) params.setCategory(sections[++i].trim());
                    break;
            }
        }
        if (params.getName() == null) params.setName(originalDescription.substring(0, Math.min(50, originalDescription.length())));
        if (params.getCategory() == null) params.setCategory("custom");
        return params;
    }

    @AiService
    interface ScriptGeneratorAiService {
        @SystemMessage("你是一个 Linux 服务器运维专家。根据用户描述的巡查需求，生成一个 shell 脚本。" +
                "要求：\n" +
                "1. 脚本必须输出 JSON 格式结果到 stdout\n" +
                "2. JSON 必须包含字段：status(ok/warning/error), message(简要描述), details(详细数据)\n" +
                "3. 脚本应该是可独立执行的 bash 脚本\n" +
                "4. 不要使用交互式命令\n" +
                "5. 按以下格式返回，每个部分用 --- 标记分隔：\n" +
                "---NAME---\n脚本名称\n" +
                "---DESCRIPTION---\n脚本描述\n" +
                "---SCRIPT---\n#!/bin/bash\n...\n" +
                "---SCHEMA---\nJSON Schema 描述输出格式\n" +
                "---CATEGORY---\ndisk/nginx/security/service/custom")
        String generate(String userMessage);
    }
}
```

---

## Task 6: 巡检引擎 — 确定性执行脚本

**Files:**
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/service/PatrolEngine.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/service/PatrolEngineImpl.java`

- [ ] **Step 1: 定义 PatrolEngine 接口**

```java
package com.codeages.termiusplus.biz.patrol.service;

import com.codeages.termiusplus.biz.patrol.dto.PatrolTaskDto;

import java.util.List;

public interface PatrolEngine {
    /**
     * 对指定服务器执行指定脚本
     */
    PatrolTaskDto executeScript(Long scriptId, Long serverId);

    /**
     * 执行所有启用的脚本对所有适用服务器
     */
    List<PatrolTaskDto> executeAll();

    /**
     * 执行指定脚本对所有适用服务器
     */
    List<PatrolTaskDto> executeScriptOnAllServers(Long scriptId);
}
```

- [ ] **Step 2: 实现 PatrolEngineImpl**

```java
package com.codeages.termiusplus.biz.patrol.service.impl;

import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.patrol.dto.PatrolTaskDto;
import com.codeages.termiusplus.biz.patrol.entity.PatrolScript;
import com.codeages.termiusplus.biz.patrol.entity.PatrolTask;
import com.codeages.termiusplus.biz.patrol.mapper.PatrolTaskMapper;
import com.codeages.termiusplus.biz.patrol.repository.PatrolScriptRepository;
import com.codeages.termiusplus.biz.patrol.repository.PatrolTaskRepository;
import com.codeages.termiusplus.biz.patrol.service.PatrolEngine;
import com.codeages.termiusplus.biz.server.dto.ServerDto;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import com.github.jaemon.dinger.core.entity.enums.MessageSubType;
import com.codeages.termiusplus.biz.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolEngineImpl implements PatrolEngine {

    private final PatrolScriptRepository scriptRepository;
    private final PatrolTaskRepository taskRepository;
    private final PatrolTaskMapper taskMapper;
    private final ServerService serverService;
    private final MessageService messageService;

    @Override
    public PatrolTaskDto executeScript(Long scriptId, Long serverId) {
        PatrolScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("脚本不存在"));
        ServerDto server = serverService.findById(serverId);

        PatrolTask task = new PatrolTask();
        task.setScriptId(scriptId);
        task.setServerId(serverId);
        task.setExecutedAt(new Date());

        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            String output = client.executeCommand(script.getScriptContent());
            task.setOutput(output);

            // 解析 JSON 输出
            Map<String, Object> result = JSONUtil.parseObj(output);
            String status = (String) result.getOrDefault("status", "error");
            task.setStatus(status);

            // 异常状态发送钉钉通知
            if ("warning".equals(status) || "error".equals(status)) {
                String message = (String) result.getOrDefault("message", "未知异常");
                sendAlert(script.getName(), server.getName(), status, message);
                task.setAlertSent(true);
            } else {
                task.setAlertSent(false);
            }
        } catch (Exception e) {
            log.error("执行巡检脚本失败: scriptId={}, serverId={}", scriptId, serverId, e);
            task.setStatus("error");
            task.setOutput("{\"status\":\"error\",\"message\":\"执行失败: " + e.getMessage() + "\"}");
            sendAlert(script.getName(), server.getName(), "error", "脚本执行失败: " + e.getMessage());
            task.setAlertSent(true);
        }

        return taskMapper.toDto(taskRepository.save(task));
    }

    @Override
    public List<PatrolTaskDto> executeAll() {
        List<PatrolScript> scripts = scriptRepository.findAllByEnabledTrue();
        List<ServerDto> servers = serverService.findAllTestInfoServer();
        List<PatrolTaskDto> results = new ArrayList<>();

        for (PatrolScript script : scripts) {
            for (ServerDto server : servers) {
                try {
                    results.add(executeScript(script.getId(), server.getId()));
                } catch (Exception e) {
                    log.error("执行巡检失败: script={}, server={}", script.getId(), server.getId(), e);
                }
            }
        }
        return results;
    }

    @Override
    public List<PatrolTaskDto> executeScriptOnAllServers(Long scriptId) {
        List<ServerDto> servers = serverService.findAllTestInfoServer();
        List<PatrolTaskDto> results = new ArrayList<>();
        for (ServerDto server : servers) {
            try {
                results.add(executeScript(scriptId, server.getId()));
            } catch (Exception e) {
                log.error("执行巡检失败: scriptId={}, serverId={}", scriptId, server.getId(), e);
            }
        }
        return results;
    }

    private void sendAlert(String scriptName, String serverName, String status, String message) {
        String title = "AI 巡查告警";
        String content = String.format("\n## %s\n- **脚本**: %s\n- **服务器**: %s\n- **状态**: %s\n- **详情**: %s",
                title, scriptName, serverName, status, message);
        messageService.send(MessageSubType.MARKDOWN, title, content);
    }
}
```

---

## Task 7: 命令白名单配置

**Files:**
- Modify: `server/src/main/resources/application.yml`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/config/CommandWhitelistConfig.java`

- [ ] **Step 1: 在 application.yml 添加白名单配置**

在 `application.yml` 末尾添加：

```yaml
patrol:
  whitelist:
    - pattern: "df *"
      autoExecute: true
    - pattern: "free *"
      autoExecute: true
    - pattern: "cat /etc/nginx/*"
      autoExecute: true
    - pattern: "cat /etc/*"
      autoExecute: true
    - pattern: "openssl *"
      autoExecute: true
    - pattern: "systemctl status *"
      autoExecute: true
    - pattern: "systemctl list-units *"
      autoExecute: true
    - pattern: "du -sh *"
      autoExecute: true
    - pattern: "ls *"
      autoExecute: true
    - pattern: "top -b *"
      autoExecute: true
    - pattern: "ps *"
      autoExecute: true
    - pattern: "ip *"
      autoExecute: true
    - pattern: "netstat *"
      autoExecute: true
    - pattern: "ss *"
      autoExecute: true
    - pattern: "nginx -t"
      autoExecute: true
    - pattern: "curl *"
      autoExecute: true
    - pattern: "systemctl restart *"
      autoExecute: false
    - pattern: "rm *"
      autoExecute: false
    - pattern: "reboot"
      autoExecute: false
    - pattern: "shutdown *"
      autoExecute: false
  schedule:
    cron: "0 0 2,14 * * ?"
```

- [ ] **Step 2: 创建白名单配置类**

```java
package com.codeages.termiusplus.biz.patrol.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "patrol")
public class CommandWhitelistConfig {

    private List<WhitelistEntry> whitelist = new ArrayList<>();
    private ScheduleConfig schedule = new ScheduleConfig();

    @Data
    public static class WhitelistEntry {
        private String pattern;
        private boolean autoExecute;
    }

    @Data
    public static class ScheduleConfig {
        private String cron = "0 0 2,14 * * ?";
    }

    /**
     * 检查命令是否匹配白名单，返回是否自动执行
     * 如果不匹配任何规则，返回 false（需要确认）
     */
    public boolean shouldAutoExecute(String command) {
        for (WhitelistEntry entry : whitelist) {
            if (matchesPattern(entry.getPattern(), command)) {
                return entry.isAutoExecute();
            }
        }
        return false;
    }

    private boolean matchesPattern(String pattern, String command) {
        // 将通配符 * 转换为正则
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        return command.trim().matches(regex);
    }
}
```

---

## Task 8: 交互式 Agent — langchain4j Agent 服务

**Files:**
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/agent/PatrolAgentService.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/agent/tool/ExecuteCommandTool.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/agent/tool/NginxTool.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/agent/tool/DiskTool.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/agent/tool/ServiceTool.java`
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/agent/tool/CleanupTool.java`

- [ ] **Step 1: 创建 ExecuteCommandTool**

```java
package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.patrol.config.CommandWhitelistConfig;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecuteCommandTool {

    private final CommandWhitelistConfig whitelistConfig;

    @Tool("在指定服务器上执行 shell 命令。如果命令在白名单中会自动执行，否则返回需要用户确认的提示。")
    public String executeCommand(Long serverId, String command) {
        boolean autoExecute = whitelistConfig.shouldAutoExecute(command);

        if (!autoExecute) {
            return "[需要用户确认] 命令 \"" + command + "\" 不在白名单中，需要用户确认后执行。请询问用户是否确认执行。";
        }

        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            String result = client.executeCommand(command);
            return result;
        } catch (Exception e) {
            log.error("执行命令失败: serverId={}, command={}", serverId, command, e);
            return "执行失败: " + e.getMessage();
        }
    }

    @Tool("执行需要用户确认的危险命令（如 rm, systemctl restart, reboot 等）。必须先获得用户明确确认后才能调用此工具。")
    public String executeDangerousCommand(Long serverId, String command) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            String result = client.executeCommand(command);
            return result;
        } catch (Exception e) {
            log.error("执行危险命令失败: serverId={}, command={}", serverId, command, e);
            return "执行失败: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 2: 创建 NginxTool**

```java
package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NginxTool {

    @Tool("获取服务器上所有 nginx 站点的 SSL 证书信息，包括域名、到期时间、颁发者等。")
    public String getNginxCerts(Long serverId) {
        String script = "for conf in /etc/nginx/sites-enabled/* /etc/nginx/conf.d/*.conf; do "
                + "[ -f \"$conf\" ] || continue; "
                + "domain=$(basename \"$conf\" .conf); "
                + "ssl_cert=$(grep -oP 'ssl_certificate\\s+\\K[^;]+' \"$conf\" 2>/dev/null | head -1); "
                + "if [ -n \"$ssl_cert\" ] && [ -f \"$ssl_cert\" ]; then "
                + "echo \"{\\\"domain\\\":\\\"$domain\\\",\\\"cert\\\":\\\"$ssl_cert\\\","
                + "\\\"info\\\":\\\"$(openssl x509 -in \"$ssl_cert\" -noout -subject -issuer -dates 2>/dev/null | tr '\\n' '|')\\\"}\"; "
                + "fi; done";

        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand(script);
        } catch (Exception e) {
            log.error("获取 nginx 证书信息失败: serverId={}", serverId, e);
            return "获取失败: " + e.getMessage();
        }
    }

    @Tool("检查 nginx 配置是否正确。")
    public String checkNginxConfig(Long serverId) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand("nginx -t 2>&1");
        } catch (Exception e) {
            return "检查失败: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 3: 创建 DiskTool**

```java
package com.codeages.termiusplus.biz.patrol.agent.tool;

import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import com.codeages.termiusplus.biz.util.command.DiskUsage;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DiskTool {

    @Tool("获取服务器磁盘使用情况，包括各分区的总容量、已用、可用和使用率。")
    public String getDiskUsage(Long serverId) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            List<DiskUsage> usages = client.getDiskUsage();
            return JSONUtil.toJsonStr(usages);
        } catch (Exception e) {
            return "获取失败: " + e.getMessage();
        }
    }

    @Tool("分析指定目录下占用空间最大的子目录，用于定位存储占用来源。传入路径如 / 或 /var。")
    public String analyzeStorageUsage(Long serverId, String path) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand("du -sh " + path + "/* 2>/dev/null | sort -rh | head -20");
        } catch (Exception e) {
            return "分析失败: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 4: 创建 ServiceTool**

```java
package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServiceTool {

    @Tool("检查指定服务（如 nginx, mysql, redis 等）的运行状态。")
    public String getServiceStatus(Long serverId, String serviceName) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand("systemctl status " + serviceName + " 2>&1");
        } catch (Exception e) {
            return "检查失败: " + e.getMessage();
        }
    }

    @Tool("列出所有正在运行的服务。")
    public String listRunningServices(Long serverId) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand("systemctl list-units --type=service --state=running --no-pager");
        } catch (Exception e) {
            return "获取失败: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 5: 创建 CleanupTool**

```java
package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CleanupTool {

    @Tool("清理指定路径下符合模式的旧文件（如 /tmp 下超过 7 天的文件）。此操作会删除文件，必须先获得用户确认。")
    public String cleanupFiles(Long serverId, String path, String pattern, int olderThanDays) {
        // 此工具始终需要用户确认，由 Agent system prompt 约束
        String command = String.format("find %s -name '%s' -mtime +%d -type f 2>/dev/null | head -50", path, pattern, olderThanDays);
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            String files = client.executeCommand(command);
            if (files.isEmpty()) {
                return "没有找到符合条件的文件";
            }
            return "以下文件将被删除（需要确认）:\n" + files;
        } catch (Exception e) {
            return "查找失败: " + e.getMessage();
        }
    }

    @Tool("确认执行文件清理。在用户确认后调用此工具实际删除文件。")
    public String confirmCleanup(Long serverId, String path, String pattern, int olderThanDays) {
        String command = String.format("find %s -name '%s' -mtime +%d -type f -delete 2>&1", path, pattern, olderThanDays);
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return "清理完成: " + client.executeCommand(command);
        } catch (Exception e) {
            return "清理失败: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 6: 创建 PatrolAgentService**

```java
package com.codeages.termiusplus.biz.patrol.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface PatrolAgentService {

    @SystemMessage("你是一个 Linux 服务器运维 AI 助手。你可以帮助用户检查和解决服务器问题。" +
            "你可以使用工具来执行命令、检查磁盘、分析 nginx 配置等。" +
            "\n重要规则：" +
            "\n1. 只能使用提供的工具，不要自行构造任意命令" +
            "\n2. 对于危险操作（删除文件、重启服务等），必须先告知用户并获得确认" +
            "\n3. 先收集信息再分析，给出清晰的问题诊断和解决方案" +
            "\n4. 如果用户要求执行危险操作，先用 cleanupFiles 等工具列出将要影响的文件，等用户确认后再调用 confirmCleanup 等工具" +
            "\n5. 输出使用 Markdown 格式，便于前端展示")
    String chat(@UserMessage String userMessage);

    @SystemMessage("你是一个 Linux 服务器运维 AI 助手。你可以帮助用户检查和解决服务器问题。" +
            "你可以使用工具来执行命令、检查磁盘、分析 nginx 配置等。" +
            "\n重要规则：" +
            "\n1. 只能使用提供的工具，不要自行构造任意命令" +
            "\n2. 对于危险操作（删除文件、重启服务等），必须先告知用户并获得确认" +
            "\n3. 先收集信息再分析，给出清晰的问题诊断和解决方案" +
            "\n4. 输出使用 Markdown 格式，便于前端展示")
    Flux<String> chatStream(@UserMessage String userMessage);
}
```

---

## Task 9: 定时巡检 Job

**Files:**
- Create: `server/src/main/java/com/codeages/termiusplus/biz/patrol/job/PatrolScheduledJob.java`

- [ ] **Step 1: 创建定时巡检任务**

```java
package com.codeages.termiusplus.biz.patrol.job;

import com.codeages.termiusplus.biz.patrol.config.CommandWhitelistConfig;
import com.codeages.termiusplus.biz.patrol.dto.PatrolTaskDto;
import com.codeages.termiusplus.biz.patrol.service.PatrolEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatrolScheduledJob {

    private final PatrolEngine patrolEngine;
    private final CommandWhitelistConfig whitelistConfig;

    @Scheduled(cron = "${patrol.schedule.cron:0 0 2,14 * * ?}")
    @SchedulerLock(name = "PatrolScheduledJob_execute", lockAtMostFor = "10m")
    public void execute() {
        log.info("定时巡检任务开始执行");
        long start = System.currentTimeMillis();

        try {
            List<PatrolTaskDto> results = patrolEngine.executeAll();
            long errorCount = results.stream().filter(r -> "error".equals(r.getStatus())).count();
            long warningCount = results.stream().filter(r -> "warning".equals(r.getStatus())).count();
            log.info("定时巡检完成: 总数={}, 错误={}, 警告={}, 耗时={}ms",
                    results.size(), errorCount, warningCount, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("定时巡检任务执行失败", e);
        }
    }
}
```

---

## Task 10: API 控制器

**Files:**
- Create: `server/src/main/java/com/codeages/termiusplus/api/admin/PatrolController.java`

- [ ] **Step 1: 创建 PatrolController**

```java
package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.patrol.dto.*;
import com.codeages.termiusplus.biz.patrol.service.PatrolEngine;
import com.codeages.termiusplus.biz.patrol.service.PatrolScriptService;
import com.codeages.termiusplus.common.OkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api-admin/patrol")
@RequiredArgsConstructor
public class PatrolController {

    private final PatrolScriptService scriptService;
    private final PatrolEngine patrolEngine;

    // === 脚本管理 ===

    @GetMapping("/scripts")
    public List<PatrolScriptDto> listScripts() {
        return scriptService.findAll();
    }

    @GetMapping("/scripts/{id}")
    public PatrolScriptDto getScript(@PathVariable Long id) {
        return scriptService.findById(id);
    }

    @PostMapping("/scripts")
    public PatrolScriptDto createScript(@RequestBody PatrolScriptCreateParams params) {
        return scriptService.create(params);
    }

    @PostMapping("/scripts/update")
    public PatrolScriptDto updateScript(@RequestBody PatrolScriptUpdateParams params) {
        return scriptService.update(params);
    }

    @PostMapping("/scripts/delete")
    public OkResponse deleteScript(@RequestBody IdPayload idPayload) {
        scriptService.delete(idPayload.getId());
        return OkResponse.TRUE;
    }

    /**
     * AI 生成巡检脚本（不保存，返回给用户审核）
     */
    @PostMapping("/scripts/generate")
    public PatrolScriptDto generateScript(@RequestBody GenerateScriptParams params) {
        return scriptService.generateScript(params.getDescription());
    }

    // === 执行巡检 ===

    /**
     * 执行单个脚本对单个服务器
     */
    @PostMapping("/execute")
    public PatrolTaskDto execute(@RequestBody PatrolExecuteParams params) {
        return patrolEngine.executeScript(params.getScriptId(), params.getServerId());
    }

    /**
     * 执行单个脚本对所有适用服务器
     */
    @PostMapping("/execute/script/{scriptId}")
    public List<PatrolTaskDto> executeScriptOnAll(@PathVariable Long scriptId) {
        return patrolEngine.executeScriptOnAllServers(scriptId);
    }

    /**
     * 执行所有启用脚本对所有服务器
     */
    @PostMapping("/execute/all")
    public List<PatrolTaskDto> executeAll() {
        return patrolEngine.executeAll();
    }

    // === 交互式 Agent ===

    /**
     * AI Agent 对话
     */
    @PostMapping("/agent/chat")
    public AgentChatResponse chat(@RequestBody AgentChatParams params) {
        // TODO: 实现 Agent 对话
        return null;
    }
}
```

- [ ] **Step 2: 补充缺失的 DTO**

需要额外创建以下 DTO：

```java
// GenerateScriptParams.java
@Data
public class GenerateScriptParams {
    @NotBlank
    private String description;
}

// PatrolExecuteParams.java
@Data
public class PatrolExecuteParams {
    @NotNull
    private Long scriptId;
    @NotNull
    private Long serverId;
}

// AgentChatParams.java
@Data
public class AgentChatParams {
    @NotBlank
    private String message;
    private Long serverId;
}

// AgentChatResponse.java
@Data
@AllArgsConstructor
public class AgentChatResponse {
    private String reply;
    private boolean needsConfirmation;
    private String pendingCommand;
}
```

同时需要创建 IdPayload（检查是否已存在）：

```java
// 检查 common/IdPayload.java 是否已存在
```

已存在 `common/IdPayload.java`，无需创建。

---

## Task 11: 前端 — API 封装

**Files:**
- Create: `web/admin/src/api/patrol.js`

- [ ] **Step 1: 创建巡逻 API 封装**

```javascript
import {client} from "@shared/api-client";

export const patrolApi = {
    // 脚本管理
    listScripts: () => client.get("/api-admin/patrol/scripts"),
    getScript: (id) => client.get(`/api-admin/patrol/scripts/${id}`),
    createScript: (params) => client.post("/api-admin/patrol/scripts", params),
    updateScript: (params) => client.post("/api-admin/patrol/scripts/update", params),
    deleteScript: (id) => client.post("/api-admin/patrol/scripts/delete", {id}),
    generateScript: (description) => client.post("/api-admin/patrol/scripts/generate", {description}),

    // 执行巡检
    execute: (scriptId, serverId) => client.post("/api-admin/patrol/execute", {scriptId, serverId}),
    executeScriptOnAll: (scriptId) => client.post(`/api-admin/patrol/execute/script/${scriptId}`),
    executeAll: () => client.post("/api-admin/patrol/execute/all"),

    // Agent 对话
    chat: (message, serverId) => client.post("/api-admin/patrol/agent/chat", {message, serverId}),
}
```

---

## Task 12: 前端 — AI 巡查对话面板

**Files:**
- Create: `web/admin/src/components/p-patrol-panel.vue`

- [ ] **Step 1: 创建 AI 巡查对话面板组件**

```vue
<template>
  <div class="patrol-panel" v-if="visible">
    <div class="patrol-panel-header">
      <span>AI 巡查助手</span>
      <a-button type="text" size="small" @click="close">×</a-button>
    </div>
    <div class="patrol-panel-messages" ref="messagesRef">
      <div v-for="(msg, index) in messages" :key="index"
           :class="['message', msg.role]">
        <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
        <div v-if="msg.needsConfirmation" class="message-actions">
          <a-button type="primary" size="small" @click="confirmCommand(msg, index)">确认执行</a-button>
          <a-button size="small" @click="rejectCommand(msg, index)">拒绝</a-button>
        </div>
      </div>
    </div>
    <div class="patrol-panel-input">
      <a-input
          v-model:value="inputText"
          placeholder="描述你想检查的问题，如：检查 nginx 证书是否过期"
          @pressEnter="sendMessage"
          :loading="loading"
      />
      <a-button type="primary" @click="sendMessage" :loading="loading">发送</a-button>
    </div>
  </div>
</template>

<script setup>
import {ref, nextTick, defineProps, defineEmits} from 'vue';
import {patrolApi} from '@/api/patrol';
import markdownIt from 'markdown-it';

const props = defineProps({
  visible: Boolean,
  serverId: Number
});

const emit = defineEmits(['update:visible']);

const md = markdownIt();
const messages = ref([]);
const inputText = ref('');
const loading = ref(false);
const messagesRef = ref(null);

const renderMarkdown = (content) => md.render(content || '');

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
};

const sendMessage = async () => {
  if (!inputText.value.trim() || loading.value) return;

  const userMessage = inputText.value.trim();
  messages.value.push({role: 'user', content: userMessage});
  inputText.value = '';
  scrollToBottom();

  loading.value = true;
  try {
    const response = await patrolApi.chat(userMessage, props.serverId);
    messages.value.push({
      role: 'assistant',
      content: response.reply,
      needsConfirmation: response.needsConfirmation,
      pendingCommand: response.pendingCommand
    });
  } catch (e) {
    messages.value.push({role: 'assistant', content: '请求失败: ' + e.message});
  }
  loading.value = false;
  scrollToBottom();
};

const confirmCommand = async (msg, index) => {
  msg.needsConfirmation = false;
  loading.value = true;
  try {
    const response = await patrolApi.chat('确认执行: ' + msg.pendingCommand, props.serverId);
    messages.value.push({role: 'assistant', content: response.reply});
  } catch (e) {
    messages.value.push({role: 'assistant', content: '执行失败: ' + e.message});
  }
  loading.value = false;
  scrollToBottom();
};

const rejectCommand = (msg, index) => {
  msg.needsConfirmation = false;
  messages.value.push({role: 'assistant', content: '已取消执行'});
  scrollToBottom();
};

const close = () => emit('update:visible', false);
</script>

<style scoped>
.patrol-panel {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  width: 400px;
  background: #1e1e1e;
  border-left: 1px solid #333;
  display: flex;
  flex-direction: column;
  z-index: 100;
}

.patrol-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #333;
  color: #fff;
}

.patrol-panel-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.message {
  margin-bottom: 12px;
  padding: 8px 12px;
  border-radius: 8px;
}

.message.user {
  background: #264f78;
  color: #fff;
  margin-left: 40px;
}

.message.assistant {
  background: #2d2d2d;
  color: #ccc;
  margin-right: 40px;
}

.message-content {
  word-break: break-word;
}

.message-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.patrol-panel-input {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #333;
}

.patrol-panel-input .ant-input {
  flex: 1;
}
</style>
```

---

## Task 13: 前端 — 集成到终端界面

**Files:**
- Modify: `web/admin/src/views/server/ServerContent.vue`

- [ ] **Step 1: 在 ServerContent.vue 添加 AI 面板入口**

在 `<template>` 中现有的工具栏区域添加 AI 巡查按钮和面板：

```vue
<!-- 在 ServerContent.vue 的模板中找到合适位置添加 -->
<a-button @click="showPatrolPanel = !showPatrolPanel" size="small">
  AI 巡查
</a-button>

<p-patrol-panel
    v-model:visible="showPatrolPanel"
    :server-id="currentServerId"
/>
```

在 `<script setup>` 中添加：

```javascript
import PPatrolPanel from "@/components/p-patrol-panel.vue";

const showPatrolPanel = ref(false);
const currentServerId = computed(() => props.server?.id);
```

---

## Task 14: 前端 — 巡查管理页面

**Files:**
- Create: `web/admin/src/views/server/PatrolPage.vue`

- [ ] **Step 1: 创建巡查管理页面**

```vue
<template>
  <div class="patrol-page">
    <a-tabs v-model:activeKey="activeTab">
      <!-- 脚本管理 Tab -->
      <a-tab-pane key="scripts" tab="巡检脚本">
        <div class="script-header">
          <a-button type="primary" @click="showGenerateModal = true">AI 生成脚本</a-button>
          <a-button @click="loadScripts">刷新</a-button>
        </div>
        <a-table :data-source="scripts" :columns="scriptColumns" row-key="id">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'enabled'">
              <a-switch :checked="record.enabled" @change="toggleScript(record)" />
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a @click="executeScript(record)">执行</a>
                <a @click="editScript(record)">编辑</a>
                <a-popconfirm title="确认删除？" @confirm="deleteScript(record.id)">
                  <a style="color: red">删除</a>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- 执行记录 Tab -->
      <a-tab-pane key="tasks" tab="执行记录">
        <a-table :data-source="tasks" :columns="taskColumns" row-key="id">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <!-- AI 生成脚本弹窗 -->
    <a-modal v-model:visible="showGenerateModal" title="AI 生成巡检脚本" @ok="generateScript">
      <a-textarea v-model:value="generateDescription" :rows="4"
                  placeholder="描述你想检查什么，如：检查所有磁盘分区使用率，超过 80% 报警" />
    </a-modal>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue';
import {patrolApi} from '@/api/patrol';
import {message} from 'ant-design-vue';

const activeTab = ref('scripts');
const scripts = ref([]);
const tasks = ref([]);
const showGenerateModal = ref(false);
const generateDescription = ref('');

const scriptColumns = [
  {title: '名称', dataIndex: 'name', key: 'name'},
  {title: '分类', dataIndex: 'category', key: 'category'},
  {title: '描述', dataIndex: 'description', key: 'description', ellipsis: true},
  {title: '启用', dataIndex: 'enabled', key: 'enabled'},
  {title: '操作', key: 'action'},
];

const taskColumns = [
  {title: '脚本', dataIndex: 'scriptName', key: 'scriptName'},
  {title: '服务器', dataIndex: 'serverName', key: 'serverName'},
  {title: '状态', dataIndex: 'status', key: 'status'},
  {title: '执行时间', dataIndex: 'executedAt', key: 'executedAt'},
];

const statusColor = (status) => {
  return {ok: 'green', warning: 'orange', error: 'red'}[status] || 'default';
};

const loadScripts = async () => {
  scripts.value = await patrolApi.listScripts();
};

const generateScript = async () => {
  if (!generateDescription.value.trim()) return;
  const result = await patrolApi.generateScript(generateDescription.value);
  // 跳转到编辑/确认页面
  message.success('脚本已生成，请确认后保存');
  showGenerateModal.value = false;
};

const toggleScript = async (record) => {
  await patrolApi.updateScript({id: record.id, enabled: !record.enabled});
  await loadScripts();
};

const executeScript = async (record) => {
  message.info('开始执行...');
  await patrolApi.executeScriptOnAll(record.id);
  message.success('执行完成');
};

const editScript = (record) => {
  // TODO: 打开编辑弹窗
};

const deleteScript = async (id) => {
  await patrolApi.deleteScript(id);
  await loadScripts();
};

onMounted(() => {
  loadScripts();
});
</script>
```

---

## Commit 策略

每完成一个 Task 后提交：

```bash
git add <相关文件>
git commit -m "feat(patrol): <Task 简要描述>"
```

示例：
- `feat(patrol): add patrol database migration`
- `feat(patrol): add PatrolScript and PatrolTask entities`
- `feat(patrol): add patrol DTOs, repository and mapper`
- `feat(patrol): add patrol script service with AI generation`
- `feat(patrol): add patrol engine for script execution`
- `feat(patrol): add command whitelist config`
- `feat(patrol): add interactive AI agent with tools`
- `feat(patrol): add scheduled patrol job`
- `feat(patrol): add patrol API controller`
- `feat(patrol): add frontend patrol API and AI panel`
- `feat(patrol): add patrol management page`
