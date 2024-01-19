import {client} from "@shared/api-client";

/*
角色名: name
服务器权限: serverPermission
*/
export const roleApi = {
    create: async (params) => {
        return client.post("/api-admin/role/create", params);
    },
    search: async (params) => {
        return client.get("/api-admin/role/search", {params: params});
    },
    update: async (params) => {
        return client.post("/api-admin/role/update", params);
    },
    delete: async (params = {id: 0}) => {
        return client.post("/api-admin/role/delete", params);
    },
    list: async (params) => {
        return client.get("/api-admin/role/list", {params: params});
    },
}

