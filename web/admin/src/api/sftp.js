import {client} from "@shared/api-client";

export const sftpApi = {

    init: async (params) => {
        return client.post("/api-admin/sftp/init", params);
    },
    pwd: async (params) => {
        return client.get("/api-admin/sftp/" + params.id + "/pwd", {params});
    },
    ls: async (params) => {
        return client.get("/api-admin/sftp/" + params.id + "/ls", {params});
    },
    mkdir: async (params) => {
        return client.post("/api-admin/sftp/" + params.id + "/mkdir", params);
    },
    rm: async (params) => {
        return client.post("/api-admin/sftp/" + params.id + "/rm", params);
    },
    rmDir: async (params) => {
        return client.post("/api-admin/sftp/" + params.id + "/rmDir", params);
    },
    rename: async (params) => {
        return client.post("/api-admin/sftp/" + params.id + "/rename", params);
    },
    upload: (params) => {
        return "/api-admin/sftp/" + params.id + "/upload";
    },
    uploadFile: (params) => {
        return "/api-admin/sftp/" + params.id + "/uploadFile";
    },
    download: (params) => {
        return "/api-admin/sftp/" + params.id + "/download?remotePath=" + encodeURI(params.remotePath).replace("+", "%2B");
    },
    close: async (params) => {
        return client.post("/api-admin/sftp/" + params.id + "/close", params);
    },
    serverUploadServer: async (params) => {
        return client.post("/api-admin/sftp/serverUploadServer", params);
    },
    readFile: async (params) => {
        return client.get("/api-admin/sftp/" + params.id + "/readFile", {params});
    },
    writeFile: async (params) => {
        return client.post("/api-admin/sftp/" + params.id + "/writeFile", params);
    }
}
