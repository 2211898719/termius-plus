import {createApp} from 'vue';
import App from './App.vue';
import router from './router';
import {bootAntDesignVue} from "@/boot/bootAntDesignVue";
import {bootAxios} from "@/boot/bootAxios";
import {bootPinia} from "@/boot/bootPinia";
import {bootStore} from "@/boot/bootStore";
import {bootFilters} from "@/boot/bootFilters";
import '@wangeditor/editor/dist/css/style.css'
import 'shepherd.js/dist/css/shepherd.css';
import { autoAnimatePlugin } from '@formkit/auto-animate/vue'
import '@vuemap/vue-amap/dist/style.css';
import VueAMap, { initAMapApiLoader } from '@vuemap/vue-amap';
import {settingApi} from "@/api/setting";

const modulesFiles = require.context('@/assets/webfonts', true, /\.woff2$|\.woff$|\.ttf$|\.eot$|\.otf$/);
let allFonts = modulesFiles.keys().map(r=>r.slice(2));
allFonts.forEach(async fontName=>{
    let font = await new FontFace(
        fontName.split('.')[0],
        `url("${require(`@/assets/webfonts/${fontName}`)}")`,
        {display: 'auto'}
    );
    document.fonts.add(font);
})

settingApi.getMapSetting().then(res=>{
    initAMapApiLoader({
        key: res.key,
        securityJsCode: res.secret,
        plugins: ['AMap.Geocoder', 'AMap.PlaceSearch'],
    });
})


const app = createApp(App);
app.use(router);
app.use(autoAnimatePlugin);
app.use(VueAMap);
bootAntDesignVue(app);
bootAxios(app);
bootPinia(app);
bootStore(app);
bootFilters(app);

app.mount('#app')
app.config.globalProperties.$allFonts = allFonts.map((fontName)=>({name:fontName.split('.')[0]}));
