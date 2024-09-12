import {client} from "@shared/api-client";

export const portForwardingApi = {

    list: async (params) => {
        return client.get("/api-admin/port-forwarding/list", {params: params});
    },
    create: async (params) => {
        return client.post("/api-admin/port-forwarding/start",  params);
    },
    update: async (params) => {
        return client.post("/api-admin/port-forwarding/update",  params);
    },
    stop: async (params) => {
        return client.post("/api-admin/port-forwarding/stop",  params);
    },
    isRunning: async (params) => {
        return client.get("/api-admin/port-forwarding/isRunning", {params: params});
    },
    stopAll: async (params) => {
        return client.post("/api-admin/port-forwarding/stopAll",  params);
    },
}
