import axios from "axios";
import {useAuthStore} from "./store/useAuthStore";
import {doDecryptStr, doEncrypt} from "./encoded";
import {reqKey, resKey} from "./encodedKey";
import pako from 'pako';

const client = axios.create({
    timeout: 120 * 1000,
});

console.log(process.env.NODE_ENV)
client.interceptors.request.use((config) => {
    const store = useAuthStore();
    if (store.isLogin) {
        config.headers.Authorization = `Bearer ${store.user.token}`;
    }
    config.headers["Content-Type"] = 'application/json;charset=UTF-8';

    if (process.env.NODE_ENV !== "development" && config.data && config.method.toUpperCase() === "POST") {
        //如果config.data里面有file对象，不加密
        let isFile = false;
        if (config.data instanceof FormData) {
            config.data.forEach((value, key) => {
                if (value instanceof File) {
                    isFile = true;
                }
            })
        } else {
            for (let key in config.data) {
                if (config.data[key] instanceof File) {
                    isFile = true;
                    break;
                }
            }
        }

        if (!isFile) {
            config.data = doEncrypt(config.data, reqKey);
        }
    }

    return config;
}, (error) => {
    return Promise.reject(error);
});

function compressArrayBuffer(str) {
    return pako.gzip(str, {to: 'array'});
}

function decompressArrayBuffer(arrayBuffer) {
    const compressedData = Uint8Array.from(atob(arrayBuffer), c => c.charCodeAt(0));
    const decompressedData = pako.inflate(compressedData);
    return decompressedData.buffer;
}

const arrayBufferToString = (arrayBuffer) => {
    return new TextDecoder('utf-8').decode(arrayBuffer);
}

function stringToBinary(str) {
    const encoder = new TextEncoder();
    return encoder.encode(str);
}

client.interceptors.response.use((response) => {
    if (process.env.NODE_ENV === "development") {
        return response.data;
    } else {
        if (typeof response.data === 'string') {
            //如果解密时间大于300ms则日志打印
            let start = new Date().getTime();
            response.data = doDecryptStr(response.data, resKey);
            let end = new Date().getTime();
            if (end - start > 300) {
                console.warn('解密时间过长', response.config.url, end - start);
            }
        }

        try {
            return JSON.parse(response.data);
        } catch (e) {
            return response.data;
        }
    }
}, (error) => {
    const response = error.response;
    const path = error.config.url;
    if (process.env.NODE_ENV !== "development") {
        if (typeof response.data === 'string') {
            //如果解密时间大于300ms则日志打印
            let start = new Date().getTime();
            response.data = doDecryptStr(response.data, resKey);
            let end = new Date().getTime();
            if (end - start > 300) {
                console.warn('解密时间过长', response.config.url, end - start);
            }
        }

        try {
            let data = JSON.parse(response.data);
            response.data = data;
        } catch (e) {
            // ignore
        }
    }

    let err;
    if (response) {
        err = response.data;
        if (!err.code) {
            err = {
                timestamp: new Date().getTime(),
                status: response.status,
                code: "INVALID_ERROR_RESPONSE",
                message: "Invalid error response format.",
                path: path,
                traceId: "",
            }
        }
    } else {
        err = {
            timestamp: new Date().getTime(),
            status: 0,
            code: "CLIENT_ERROR",
            message: error.message,
            path: path,
            traceId: "",
        }
    }

    if (err.status === 401) {
        const store = useAuthStore();
        store.logout();
    }

    return Promise.reject(err);
});

export {client};
