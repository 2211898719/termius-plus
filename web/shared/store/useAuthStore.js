import {defineStore} from "pinia";

function setCookie(name, value, expires, path) {
    var cookieText = encodeURIComponent(name) + '=' + encodeURIComponent(value);
    if (expires instanceof Date) {
        cookieText += '; expires=' + expires.toUTCString();
    }
    if (path) {
        cookieText += '; path=' + path;
    }
    document.cookie = cookieText;
}

export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: null,
        session: ''
    }),

    getters: {
        isLogin(state) {
            return !!state.user;
        },
        getSession(state) {
            return state.session;
        }
    },

    actions: {
        login(user) {
            this.user = user;
            localStorage.setItem('AuthUser', JSON.stringify(user));
        },

        logout() {
            localStorage.removeItem('AuthUser');
            // localStorage.removeItem('token');
            // setCookie('token', '', new Date(0), '/');
            this.user = undefined;
        },
        init() {
            const user = JSON.parse(localStorage.getItem('AuthUser'));
            if (user) {
                this.user = user;
            }
        },
        hasRole(role) {
            return this.user.roles.includes(role);
        },
        setSession(session) {
            this.session = session;
        }
    }

});
