import {fileApi} from "@/api/file";
import {client} from "@shared/api-client";
import {message} from "ant-design-vue";
import axios from "axios";
import {useAuthStore} from "@shared/store/useAuthStore";
import {isObject} from "ant-design-vue/es/_util/util";

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

const fileParams = {
    [fileTypeEnum.image]: {accept: '.jpg, .jpeg, .png, .gif', url: fileApi.upload()},
    [fileTypeEnum.media]: {accept: '.mp3, .mp4', url: fileApi.upload()},
    [fileTypeEnum.all]: {accept: '*', url: fileApi.upload()},
    [fileTypeEnum.excel]: {accept: '.xlsx, .xls', url: fileApi.uploadExcel()},
    [fileTypeEnum.word]: {accept: '.docx, .doc', url: fileApi.upload()},
    [fileTypeEnum.pdf]: {accept: '.pdf', url: fileApi.upload()},
    [fileTypeEnum.ppt]: {accept: '.pptx, .ppt', url: fileApi.upload()},
    [fileTypeEnum.zip]: {accept: '.zip', url: fileApi.upload()},
}

const defaultCallback = (res, fileName, fileUrl) => {
    console.log("defaultCallback, file:", fileUrl);
}

const defaultBeforeUpload = (res, fileName, fileUrl) => {
    console.log("defaultCallback, file:", fileUrl);
}

export const uploadFile = (filetype = fileTypeEnum.all, callback = defaultCallback, otherParams = {}, beforeUpload = defaultBeforeUpload, errorCallback) => {
    if (!fileParams[filetype]) {
        console.error(`File type ${filetype} is not supported`);
    }

    const input = document.createElement('input');
    input.setAttribute('type', 'file');
    input.setAttribute('accept', fileParams[filetype].accept);
    input.onchange = function () {
        if (beforeUpload) {
            beforeUpload();
        }

        const file = this.files[0];
        upload(fileParams[filetype].url, file, otherParams).then(res => {
            callback(res, file.name, fileApi.getAppFile(res.uuid))
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

function generateURL(url, data) {
    let param = '';
    for (let value in data) {
        if (data[value] === undefined || data[value] === '') {
            continue;
        }

        if (isObject(data[value])) {
            for (let item in data[value]) {
                if (data[value][item]) {
                    let array = item.toLowerCase().split('');
                    array[0] = array[0].toUpperCase();
                    param += `${value}${array.join('')}=${data[value][item]}&&`;
                }
            }
        } else {
            param += `${value}=${data[value]}&&`;
        }
    }

    return url + '?' + param;
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
    return new Promise((resolve, reject) => {
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
