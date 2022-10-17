import {client} from "@shared/api-client";

export const userApi = {

    create: async (params) => {
        return client.post("/api-admin/user/create", params);
    },

    search: async (params) => {
        return client.get("/api-admin/user/search", {params: params});
    }
}
