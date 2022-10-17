import {defineStore} from "pinia";

export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: null,
    }),

    getters: {
        isLogin(state) {
            return !!state.user;
        }
    },

    actions: {
        login(user) {
            this.user = user;
            localStorage.setItem('AuthUser', JSON.stringify(user));
        },

        logout() {
            localStorage.removeItem('AuthUser');
            this.user = undefined;
        },

        init() {
            const user = JSON.parse(localStorage.getItem('AuthUser'));
            if (user) {
                this.user = user;
            }
        }
    }

});
