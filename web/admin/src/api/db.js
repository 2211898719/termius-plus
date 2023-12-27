import {client} from "@shared/api-client";

export const dbApi = {
    getDbConnInfo: async (id) => {
        return client.get(`/api-admin/dbConn/get/${id}`);
    },
    showDatabase: async (params) => {
        return client.get("/api-admin/dbConnOperation/showDatabase", {params: params});
    },
    showTables: async (params) => {
        return client.get("/api-admin/dbConnOperation/showTables", {params: params});
    },
    getTableColumns: async (params) => {
        return client.get("/api-admin/dbConnOperation/getTableColumns", {params: params});
    },
    selectTableData: async (params) => {
        return client.get("/api-admin/dbConnOperation/selectTableData", {params: params});
    },
}
