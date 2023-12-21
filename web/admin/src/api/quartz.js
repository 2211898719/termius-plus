import {client} from "@shared/api-client";

export const quartzApi = {

    list: async (params) => {
        return client.get("/api-admin/quartz/list", {params: params});
    },
    create: async (params) => {
        return client.post("/api-admin/quartz/create",  params);
    },
    update: async (params) => {
        return client.post("/api-admin/quartz/update",  params);
    },
    delete: async (params) => {
        return client.post("/api-admin/quartz/delete",  params);
    }
}
