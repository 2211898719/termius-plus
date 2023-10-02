import {client} from "@shared/api-client";

export const proxyApi = {

    list: async (params) => {
        return client.get("/api-admin/proxy/list", {params: params});
    },
    create: async (params) => {
        return client.post("/api-admin/proxy/create",  params);
    },
    update: async (params) => {
        return client.post("/api-admin/proxy/update",  params);
    },
    delete: async (params) => {
        return client.post("/api-admin/proxy/delete",  params);
    }
}
