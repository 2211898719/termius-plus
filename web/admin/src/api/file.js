import {client} from "@shared/api-client";

export const fileApi = {
    upload: () => {
        return "/api-admin/file/upload"
    },
    uploadImg: () => {
        return "/api-admin/file/uploadImg"
    },
    uploadExcel: () => {
        return `/api-admin/excel/import`
    },
    getFile: (uuid) => {
        return `/api-admin/file/get/${uuid}`
    },
    getFileInfo: async (uuid) => {
        return client.get(`/api-admin/file/getFileInfo/${uuid}`)
    },
    findFileInfoList: async (uuidList = []) => {
        return client.get(`/api-admin/file/findFileInfoList?uuids=${uuidList.join(',')}`)
    },
    getAppFile: (uuid) => {
        return `/api-admin/file/get/${uuid}`
    },
    makePlayerRequestToken: async (params = {cloudNo: ""}) => {
        return client.post(`/api-admin/file/makePlayerRequestToken`, params)
    },
    downloadFile: (url) => {
        const a = document.createElement('a')
        a.download = name
        a.href = url
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
    },
    isTranscodeSuccess: async (uuid) => {
        return client.get(`/api-admin/file/isTranscodeSuccess/${uuid}`)
    },
    back:  () => {
        return `/api-admin/back/back`
    },
}
