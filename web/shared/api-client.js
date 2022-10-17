import axios from "axios";
import {useAuthStore} from "./store/useAuthStore";

const client = axios.create({
    timeout: 60000,
});

client.interceptors.request.use((config) => {
    const store = useAuthStore();
    if (store.isLogin) {
        config.headers.Authorization = `Bearer ${store.user.token}`;
    }

    return config;
}, (error) => {
    return Promise.reject(error);
});

client.interceptors.response.use((response) => {
    return response.data;
}, (error) => {
    const response = error.response;
    const path = error.config.url;
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
