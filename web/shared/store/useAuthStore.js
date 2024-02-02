import {defineStore} from "pinia";

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
