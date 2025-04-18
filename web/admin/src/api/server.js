import {client} from "@shared/api-client";

export const serverApi = {

    list: async (params) => {
        return client.get("/api-admin/server/list", {params: params});
    },
    groupList: async (params) => {
        return client.get("/api-admin/server/groupList", {params: params});
    },
    create: async (params) => {
        return client.post("/api-admin/server/create", params);
    },
    testServerParams: async (params) => {
        return client.post("/api-admin/server/testServerParams", params);
    },
    update: async (params) => {
        return client.post("/api-admin/server/update", params);
    },
    updateSort: async (params) => {
        return client.post("/api-admin/server/updateSort", params);
    },
    del(id) {
        return client.post("/api-admin/server/delete", {id: id});
    },
    getHistory: async (serverId) => {
        return client.get(`/api-admin/server/${serverId}/history`);
    },
    getMysqlHistory: async (serverId) => {
        return client.get(`/api-admin/server/${serverId}/mysqlHistory`);
    },
    get(serverId) {
        return client.get(`/api-admin/server/${serverId}/get`);
    },
    aiChat(data) {
        return `/api-admin/ai/chat?${new URLSearchParams(data).toString()}`
    },
    getAllServerRunInfo() {
        return client.get("/api-admin/server/getAllServerRunInfo");
    },
    clearAllConnections() {
        return client.post("/api-admin/server/clearConnect");
    },
    syncAllServerRunInfo() {
        return client.get("/api-admin/server/syncAllServerRunInfo");
    },
    getServerDetail(serverId) {
        return client.get(`/api-admin/server/getServerRunInfoDetail/${serverId}`);
    }
}
