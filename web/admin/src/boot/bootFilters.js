import {filters} from "@shared/filters";

export function bootFilters(app) {
    app.config.globalProperties.$f = filters;
}
