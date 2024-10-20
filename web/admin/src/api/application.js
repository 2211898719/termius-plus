import {client} from "@shared/api-client";

export const applicationApi = {
    list: async (params) => {
        return client.get("/api-admin/application/list", {params: params});
    },
    create: async (params) => {
        return client.post("/api-admin/application/create",  params);
    },
    update: async (params) => {
        return client.post("/api-admin/application/update",  params);
    },
    updateSort: async (params) => {
        return client.post("/api-admin/application/updateSort",  params);
    },
    del(id) {
        return client.post("/api-admin/application/delete", {id: id});
    },
    testMonitor(params) {
        return client.post("/api-admin/applicationMonitor/test", params);
    },
    groupList: async () => {
        return client.get("/api-admin/application/groupList");
    },
    getApplicationErrorRank: async (params) => {
        return client.get("/api-admin/application/getApplicationErrorRank", {params: params});
    },
    getApplicationRequestMap: async (params) => {
        return client.get("/api-admin/application/getApplicationRequestMap", {params: params});
    },
}
