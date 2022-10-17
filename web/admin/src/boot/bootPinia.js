import { createPinia } from 'pinia';

export function bootPinia(app) {
    app.use(createPinia());
}
