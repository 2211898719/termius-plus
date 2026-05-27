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
