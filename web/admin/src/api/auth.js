import {client} from "@shared/api-client";

export const authApi = {
    login: async (params) => {
        return client.post("/api-admin/public/login", params);
    }
}
