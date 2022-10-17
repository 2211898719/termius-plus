import {useAuthStore} from "@shared/store/useAuthStore";

export function bootStore() {
    const store = useAuthStore();
    store.init();
}
