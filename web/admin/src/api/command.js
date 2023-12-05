import {client} from "@shared/api-client";

export const commandApi = {

    list: async (params) => {
        return client.get("/api-admin/command/list", {params: params});
    },
    create: async (params) => {
        return client.post("/api-admin/command/create",  params);
    },
    update: async (params) => {
        return client.post("/api-admin/command/update",  params);
    },
    delete: async (params) => {
        return client.post("/api-admin/command/delete",  params);
    }
}
