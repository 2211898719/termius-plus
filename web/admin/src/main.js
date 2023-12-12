import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import {bootAntDesignVue} from "@/boot/bootAntDesignVue";
import {bootAxios} from "@/boot/bootAxios";
import {bootPinia} from "@/boot/bootPinia";
import {bootStore} from "@/boot/bootStore";
import {bootFilters} from "@/boot/bootFilters";
import '@wangeditor/editor/dist/css/style.css' // 引入 css
import SplitBox from '@headerless/split-box'
import '@headerless/split-box/style.css'
import  'resize-observer-polyfill';
import { autoAnimatePlugin } from '@formkit/auto-animate/vue'
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'

const app = createApp(App,{
    compilerOptions: {
        isCustomElement: (tag) => tag.startsWith('a-'),
    },
});
app.use(router);
app.use(autoAnimatePlugin);
// app.component('Splitpanes', Splitpanes)
// app.component('Pane', Pane)
// app.component('SplitBox', SplitBox)

bootAntDesignVue(app);
bootAxios(app);
bootPinia(app);
bootStore(app);
bootFilters(app);

app.mount('#app')
