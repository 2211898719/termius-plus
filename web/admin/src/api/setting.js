import {client} from "@shared/api-client";

export const settingApi = {

    getMapSetting: async () => {
        return client.get("/api-admin/public/getMapSetting");
    },
}
