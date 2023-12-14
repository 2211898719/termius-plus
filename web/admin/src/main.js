import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import {bootAntDesignVue} from "@/boot/bootAntDesignVue";
import {bootAxios} from "@/boot/bootAxios";
import {bootPinia} from "@/boot/bootPinia";
import {bootStore} from "@/boot/bootStore";
import {bootFilters} from "@/boot/bootFilters";
import '@wangeditor/editor/dist/css/style.css'
import  'resize-observer-polyfill';
import { autoAnimatePlugin } from '@formkit/auto-animate/vue'

const app = createApp(App);
app.use(router);
app.use(autoAnimatePlugin);

bootAntDesignVue(app);
bootAxios(app);
bootPinia(app);
bootStore(app);
bootFilters(app);

app.mount('#app')
