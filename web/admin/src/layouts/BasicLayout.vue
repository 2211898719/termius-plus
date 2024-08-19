<template>
  <keep-alive>
    <pro-layout
        ref="fullscreenRef"
        :locale="locale"
        v-model:openKeys="state.openKeys"
        v-model:collapsed="state.collapsed"
        v-model:selectedKeys="state.selectedKeys"
        :menu-data="menuData"
        v-bind="layoutConf"
    >
        <template #rightContentRender>
            <div :style="{'font-family':currentFont}" style="opacity: 0;width: 0px;height: 0px">退出</div>
          <fullscreen-outlined @click="toggleFullScreen" style="margin-right: 8px;cursor: pointer"/>
            <span v-if="store.user" style="color: #fff">您好，{{ store.user.username }}</span>
            <a-button type="link" @click="onLogout">退出</a-button>
        </template>

      <router-view v-slot="{ Component }">
        <transition>
          <div>
          <keep-alive>
            <component :is="Component"/>
          </keep-alive>
          </div>
        </transition>
      </router-view>
    </pro-layout>
  </keep-alive>

</template>

<script setup>
import {reactive, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import {clearMenuItem, getMenuData} from '@ant-design-vue/pro-layout';
import {useAuthStore} from "@shared/store/useAuthStore";
import config from "@/config";
import {walk} from "@/utils/treeUtil";
import {useStorage} from "@vueuse/core";

const store = useAuthStore();
const locale = (i18n) => i18n;
const router = useRouter();

//预加载一下字体避免闪烁露馅
let currentFont = useStorage('currentFont', 'JetBrainsMono-ExtraBold')

let routerTree = router.getRoutes();
//过滤routerTree

walk(routerTree, (node) => {
    if (node.meta && node.meta.hasRole && !store.hasRole(node.meta.hasRole)) {
      node.meta.hideInMenu = true;
    }
})

function toggleFullScreen() {
  const doc = window.document;
  const docEl = doc.documentElement;

  const requestFullScreen = docEl.requestFullscreen || docEl.mozRequestFullScreen || docEl.webkitRequestFullScreen || docEl.msRequestFullscreen;
  const exitFullScreen = doc.exitFullscreen || doc.mozCancelFullScreen || doc.webkitExitFullscreen || doc.msExitFullscreen;

  if (!doc.fullscreenElement && !doc.mozFullScreenElement && !doc.webkitFullscreenElement && !doc.msFullscreenElement) {
    // 进入全屏模式
    if (requestFullScreen) {
      requestFullScreen.call(docEl);
    }
  } else {
    // 退出全屏模式
    if (exitFullScreen) {
      exitFullScreen.call(doc);
    }
  }
}

const {menuData} = getMenuData(clearMenuItem(routerTree));

const state = reactive({
    collapsed: false, // default value
    openKeys: [],
    selectedKeys: [],
});

const layoutConf = ref({
    title: config.name,
    navTheme: 'light',
    headerTheme: 'dark',
    layout: 'mix',
    splitMenus: true,
    fixSiderbar: true,
});

watch(router.currentRoute, () => {
    const matched = router.currentRoute.value.matched.concat()
    state.selectedKeys = matched.filter(r => r.name !== 'index').map(r => r.path)
    state.openKeys = matched.filter(r => r.path !== router.currentRoute.value.path).map(r => r.path)
}, {
    immediate: true,
})

const onLogout = () => {
    store.logout();
    router.push({name: "login"});
}


</script>

<style >
.ant-space{
  display: flex !important;
}

.ant-pro-basicLayout-content{
  margin: 0!important;
}
</style>
