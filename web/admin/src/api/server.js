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
    }
}
