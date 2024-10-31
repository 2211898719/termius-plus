import {client} from "@shared/api-client";

export const serverApi = {

    list: async (params) => {
        return client.get("/api-admin/server/list", {params: params});
    },
    groupList: async (params) => {
        return client.get("/api-admin/server/groupList", {params: params});
    },
    create: async (params) => {
        return client.post("/api-admin/server/create",  params);
    },
    update: async (params) => {
        return client.post("/api-admin/server/update",  params);
    },
    updateSort: async (params) => {
        return client.post("/api-admin/server/updateSort",  params);
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
    get(serverId){
        return client.get(`/api-admin/server/${serverId}/get`);
    },
    aiChat(data){
        return `/api-admin/ai/chat?${new URLSearchParams(data).toString()}`
    },
    requestMap(id){
        return `/api-admin/application/requestMap/${id}`
    },
    getAllServerRunInfo(){
        return client.get("/api-admin/server/getAllServerRunInfo");
    },

}
