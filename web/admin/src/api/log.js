import {client} from "@shared/api-client";

export const commandLogApi = {

    list: async (params) => {
        return client.get("/api-admin/commandLog/list", {params: params});
    },
    search: async (params) => {
        return client.get("/api-admin/commandLog/search", {params: params});
    },
    get: async (id) => {
        return client.get(`/api-admin/commandLog/get/${id}`);
    },
}
