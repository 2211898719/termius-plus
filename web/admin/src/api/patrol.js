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
    chat: (message, serverId, conversationId) => client.post("/api-admin/patrol/agent/chat", {message, serverId, conversationId}),

    // Agent 流式对话 - 返回 EventSource URL
    chatStreamUrl: (message, conversationId) => {
        const params = new URLSearchParams({message});
        if (conversationId) params.append('conversationId', conversationId);
        return `/api-admin/patrol/agent/chat/stream?${params.toString()}`;
    },

    // 对话管理
    listConversations: () => client.get("/api-admin/patrol/agent/conversations"),
    createConversation: () => client.post("/api-admin/patrol/agent/conversations"),
    deleteConversation: (conversationId) => client.post("/api-admin/patrol/agent/conversations/delete", {conversationId}),
    listMessages: (conversationId) => client.get(`/api-admin/patrol/agent/messages?conversationId=${conversationId}`),
    saveMessage: (params) => client.post("/api-admin/patrol/agent/messages", params),
}
