import {client} from "@shared/api-client";
import {message} from "ant-design-vue";
import axios from "axios";
import {useAuthStore} from "@shared/store/useAuthStore";

export const fileTypeEnum = {
    image: "image",
    all: "all",
    media: "media",
    excel: "excel",
    word: "word",
    pdf: "pdf",
    ppt: "ppt",
    zip: "zip",
}


const defaultCallback = (res, fileName) => {
    console.log("defaultCallback, file:", res, fileName);
}

const defaultBeforeUpload = (res, fileName, fileUrl) => {
    console.log("defaultCallback, file:", res, fileName, fileUrl);
}

export const uploadFile = (url, callback = defaultCallback, otherParams = {}, beforeUpload = defaultBeforeUpload, errorCallback) => {
    const input = document.createElement('input');
    input.setAttribute('type', 'file');
    input.onchange = function () {
        if (beforeUpload) {
            beforeUpload();
        }

        const file = this.files[0];
        upload(url, file, otherParams).then(res => {
            callback(res, file.name)
        }).catch(err => {
            errorCallback(err)
            message.error(err.message)
        })
    }

    input.click();
}

export const upload = (url, file, otherParams) => {
    const data = new FormData();
    data.append("file", file);
    Object.keys(otherParams).forEach(key => {
        data.append(key, otherParams[key]);
    })

    let uploadProgress = 0;
    let key = 'uploadFileProgress'
    message.loading({content: `上传中...${uploadProgress}%`, key}, 0);

    return client.post(url, data, {
        headers: {
            "content-type": "multipart/form-data",
        },
        timeout: 3600 * 1000,
        onUploadProgress: (progressEvent) => {
            if (progressEvent.lengthComputable) {
                uploadProgress = progressEvent.loaded / progressEvent.total * 100
                message.loading({content: `上传中...${uploadProgress.toFixed(2)}%`, key}, 0);
            }
        }
    }).then(res => {
            message.destroy(key);
            return res;
        }
    ).catch(err => {
        message.destroy(key);
        return Promise.reject(err);
    })
}


export const download = async (url, data = null, fileName = null) => {
    let res = await getFileLocalUrl(url, data);
    let name = fileName !== null ? fileName : getFileName(res.res);
    send(name, res.url)
    window.URL.revokeObjectURL(res.url)
}

export const getFileLocalUrl = async (url, data = null, loading = true) => {
    let store = useAuthStore();

    let downloadFileProgress = 0;
    let key = 'downloadFileProgress'

    if (loading) {
        message.loading({content: `下载中...${downloadFileProgress}%`, key}, 0);
    }

    let res = await axios({
        method: 'get', url: url, responseType: 'blob', headers: {
            'Authorization': `Bearer ${store.user.token}`
        }, params: data,
        onDownloadProgress: (progressEvent) => {
            downloadFileProgress = progressEvent.loaded / progressEvent.total * 100
            if (loading) {
                message.loading({content: `下载中...${downloadFileProgress.toFixed(2)}%`, key}, 0);
            }
        }
    })

    if (loading) {
        message.destroy(key);
    }

    if (res.data.type === "application/json") {
        blobToObject(res.data).then(errorInfo => {
            handleError(errorInfo)
        })
        return
    }

    return {url: window.URL.createObjectURL(res.data), res};
}


function send(filename, link) {
    let DownloadLink = document.createElement('a');
    DownloadLink.style = 'display: none';
    DownloadLink.download = filename;
    DownloadLink.href = link;
    document.body.appendChild(DownloadLink);
    DownloadLink.click();
    document.body.removeChild(DownloadLink);
}

export function getFileName(response) {
    let fileName
    let contentDisposition = response.headers['content-disposition']
    if (contentDisposition) {
        let filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/
        let matches = filenameRegex.exec(contentDisposition)
        if (matches != null && matches[1]) {
            fileName = matches[1].replace(/['"]/g, '')
        }
        fileName = decodeURI(fileName)
    }
    return fileName
}

function blobToObject(data) {
    return new Promise((resolve) => {
        let reader = new FileReader();
        reader.readAsText(data, 'utf-8');
        reader.onload = function () {
            try {
                resolve(JSON.parse(reader.result))
            } catch (error) {
                resolve({
                    code: 200, message: '获取文件信息成功'
                })
            }
        }
    })
}

function handleError(errorData) {
    let err;
    if (errorData) {
        err = errorData;
        if (!err.code) {
            err = {
                timestamp: new Date().getTime(),
                status: errorData.status,
                code: "INVALID_ERROR_RESPONSE",
                message: "Invalid error response format.",
                traceId: "",
            }
        }
    } else {
        err = {
            timestamp: new Date().getTime(), status: 0, code: "CLIENT_ERROR", message: errorData.message, traceId: "",
        }
    }

    if (err.status === 401) {
        const store = useAuthStore();
        store.logout();
    }
}


export function isImage(ext) {
    return ['jpg', 'jpeg', 'png', 'gif', 'bmp'].includes(ext.toLowerCase());
}

export function isVideo(ext) {
    return ['mp4', 'avi', 'rmvb', 'rm', 'flv', '3gp', 'mkv', 'mov'].includes(ext.toLowerCase());
}

//根据文件类型显示不同图标
export const computedFileIcon = (type) => {
    let fileIcon = ''
    if (type === 'doc' || type === 'docx') {
        fileIcon += 'word'
    } else if (type === 'xls' || type === 'xlsx') {
        fileIcon += 'excel'
    } else if (type === 'ppt' || type === 'pptx') {
        fileIcon += 'ppt'
    } else if (type === 'pdf') {
        fileIcon += 'pdf'
    } else if (type === 'txt') {
        fileIcon += 'txt'
    } else if (type === 'zip' || type === 'rar') {
        fileIcon += 'zip'
    } else if (type === 'jpg' || type === 'jpeg' || type === 'png' || type === 'gif') {
        fileIcon += 'img-default'
    } else if (type === 'mp4' || type === 'avi' || type === 'rmvb' || type === 'flv' || type === 'wmv' || type === 'mov' || type === 'mkv') {
        fileIcon += 'video'
    } else {
        fileIcon += 'unknown'
    }

    return require('@/assets/file-icon/' + fileIcon + '.png');
}


//根据byte计算文件大小
export const computedFileSize = (size) => {
    if (size < 1024) {
        return size + 'B'
    } else if (size < 1024 * 1024) {
        return (size / 1024).toFixed(2) + 'KB'
    } else if (size < 1024 * 1024 * 1024) {
        return (size / 1024 / 1024).toFixed(2) + 'MB'
    } else {
        return (size / 1024 / 1024 / 1024).toFixed(2) + 'GB'
    }
}
