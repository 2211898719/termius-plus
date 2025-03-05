import {client} from "@shared/api-client";

export const portForwardingApi = {

    list: async (params) => {
        return client.get("/api-admin/port-forwarding/list", {params: params});
    },
    create: async (params) => {
        return client.post("/api-admin/port-forwarding/create",  params);
    },
    update: async (params) => {
        return client.post("/api-admin/port-forwarding/update",  params);
    },
    del: async (params) => {
        return client.post("/api-admin/port-forwarding/delete",  params);
    },
    start: async (params) => {
        return client.post("/api-admin/port-forwarding/start",  params);
    },
    stop: async (params) => {
        return client.post("/api-admin/port-forwarding/stop",  params);
    },
    getLocalIp: async () => {
        return client.get("/api-admin/port-forwarding/getLocalIp");
    },
}
