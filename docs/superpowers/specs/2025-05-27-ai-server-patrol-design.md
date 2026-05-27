# AI 服务器巡查 Agent 设计文档

## 概述

在 Termius Plus 中实现 AI 驱动的服务器巡查系统，包括定时自动巡检和交互式 AI 排查两个模式。

## 核心设计原则

- **定时巡检确定性执行**：AI 在脚本生成阶段参与，执行阶段完全由预定义脚本驱动，不调用 AI
- **交互式 Agent 自主推理**：用户手动触发时，langchain4j Agent 自主选择工具完成排查
- **安全可控**：命令白名单机制，危险操作必须用户确认

## 一、整体架构

### 三大模块

**1. 巡检脚本管理（Script Management）**
- `PatrolScript` 实体：存储脚本定义（名称、描述、shell 脚本内容、输出 JSON Schema、分类、适用服务器范围）
- 用户描述需求 → AI（DeepSeek）生成 shell 脚本 + 输出 JSON 格式 → 用户审核 → 保存
- 脚本支持启用/禁用

**2. 定时巡检引擎（Scheduled Patrol）**
- 定时任务遍历所有启用的脚本 × 适用服务器
- 通过 `ExecuteCommandSSHClient` 执行脚本，解析 JSON 输出
- 异常结果触发钉钉通知（复用现有 `MessageService`）
- 完全确定性，不调用 AI

**3. 交互式 Agent（Interactive Agent）**
- 基于 langchain4j `@AiService` + `@Tool` 定义服务器操作工具集
- 工具包括：执行命令（白名单自动/需确认）、读文件、查 nginx 配置、查证书等
- AI Agent 自主推理调用工具完成用户请求
- 在前端 SSH 终端界面以 AI 面板形式交互

## 二、数据模型

### PatrolScript（巡检脚本）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键自增 |
| name | String | 脚本名称 |
| description | String | 脚本描述 |
| scriptContent | String (Text) | Shell 脚本内容 |
| outputSchema | String (Text) | 输出 JSON Schema 定义 |
| category | String | 分类：disk / nginx / security / service / custom |
| enabled | Boolean | 是否启用 |
| createdAt | Long | 创建时间 |
| updatedAt | Long | 更新时间 |

### PatrolTask（巡检任务记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键自增 |
| scriptId | Long | 关联脚本 ID |
| serverId | Long | 关联服务器 ID |
| status | String | ok / warning / error |
| output | String (Text) | 脚本 JSON 输出 |
| alertSent | Boolean | 是否已发送告警 |
| executedAt | Date | 执行时间 |
| createdAt | Long | 创建时间 |

### CommandWhitelist（命令白名单）

存储在 `application.yml` 配置中，不建表：

```yaml
patrol:
  whitelist:
    - pattern: "df *"
      autoExecute: true
    - pattern: "free *"
      autoExecute: true
    - pattern: "cat /etc/nginx/*"
      autoExecute: true
    - pattern: "openssl *"
      autoExecute: true
    - pattern: "systemctl status *"
      autoExecute: true
    - pattern: "systemctl restart *"
      autoExecute: false
    - pattern: "rm *"
      autoExecute: false
```

## 三、核心流程

### 定时巡检流程

1. 定时任务触发（Quartz cron，可配置，默认每天 2 次）
2. 遍历所有启用的 `PatrolScript` × 适用服务器列表
3. 对每台服务器，SSH 连接执行脚本
4. 解析脚本输出 JSON（约定 `{status, message, details}`)
5. 存入 `PatrolTask`
6. status 为 warning/error 时，组装钉钉消息发送告警

### 交互式 Agent 流程

1. 用户在前端 AI 面板输入自然语言
2. Agent 接收消息，调用 langchain4j 推理
3. Agent 选择 @Tool 执行操作
4. `executeCommand` 工具内部检查命令白名单：
   - 匹配白名单且 `autoExecute=true` → 直接执行
   - 不匹配或危险命令 → 返回给 Agent，Agent 在对话中询问用户确认
5. 用户确认后，Agent 再次调用工具执行
6. Agent 综合结果，生成分析报告

### 脚本生成流程

1. 用户输入需求描述
2. AI 生成 shell 脚本 + 输出 JSON 格式
3. 前端展示脚本预览，用户可编辑
4. 用户确认保存 → 存入 `PatrolScript`

## 四、Agent 工具定义

基于 langchain4j `@Tool` 定义：

| 工具 | 说明 | 白名单判断 |
|------|------|-----------|
| `executeCommand(serverId, command)` | SSH 执行命令 | 查白名单 |
| `readFile(serverId, path)` | 读取服务器文件 | 白名单路径自动执行 |
| `getDiskUsage(serverId)` | 获取磁盘使用情况 | 自动执行 |
| `getNginxCerts(serverId)` | 获取 nginx 站点证书信息 | 自动执行 |
| `getServiceStatus(serverId, serviceName)` | 检查服务运行状态 | 自动执行 |
| `restartService(serverId, serviceName)` | 重启服务 | 必须用户确认 |
| `cleanupFiles(serverId, path, pattern)` | 清理指定路径下的文件 | 必须用户确认 |

## 五、前端交互

### AI 面板（扩展现有终端界面）

- 终端区域右侧新增可折叠的 AI 对话面板
- 用户输入自然语言，Agent 执行工具时在对话中展示进度
- 需要确认的命令展示为可点击按钮 `[确认执行]` `[拒绝]`

### 巡查结果查看

- 在现有服务器监控页面增加「AI 巡查」Tab
- 展示历史巡查记录列表（时间、脚本名、服务器、状态、详情）
- 异常记录高亮显示

## 六、后端包结构

```
biz/patrol/
├── entity/       PatrolScript, PatrolTask
├── dto/          PatrolScriptCreateParams, PatrolScriptDto, PatrolTaskDto
├── repository/   PatrolScriptRepository, PatrolTaskRepository
├── mapper/       PatrolScriptMapper, PatrolTaskMapper
├── service/      PatrolScriptService, PatrolEngine
├── service/impl/ PatrolScriptServiceImpl, PatrolEngineImpl
├── job/          PatrolScheduledJob
└── agent/        PatrolAgentService

biz/patrol/agent/tool/
├── ExecuteCommandTool.java
├── NginxTool.java
├── DiskTool.java
├── ServiceTool.java
└── CleanupTool.java
```

## 七、技术依赖

全部复用现有依赖，无新增：
- langchain4j（已有）— Agent + @Tool
- DeepSeek（已有）— AI 推理
- sshj（已有）— SSH 执行
- Quartz + ShedLock（已有）— 定时任务
- DingTalk/Dinger（已有）— 告警通知
- Spring Data JPA + Flyway（已有）— 数据持久化
