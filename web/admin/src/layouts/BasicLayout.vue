<template>
  <keep-alive>
    <pro-layout
        :locale="locale"
        v-model:openKeys="state.openKeys"
        v-model:collapsed="state.collapsed"
        v-model:selectedKeys="state.selectedKeys"
        :menu-data="menuData"
        v-bind="layoutConf"
    >
        <template #rightContentRender>
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
  <a-modal v-model:visible="authKeyBoardVisible" title="输入服务器二次验证码" @ok="handleOk">
    {{authKeyBoardServerKey}}
    <a-input v-model:value="authKeyBoardPassword"></a-input>
  </a-modal>
</template>

<script setup>
import {reactive, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import {clearMenuItem, getMenuData} from '@ant-design-vue/pro-layout';
import {useAuthStore} from "@shared/store/useAuthStore";
import config from "@/config";
import {walk} from "@/utils/treeUtil";
import {useWebSocket} from "@vueuse/core";

const store = useAuthStore();
const locale = (i18n) => i18n;
const router = useRouter();

let routerTree = router.getRoutes();
//过滤routerTree

walk(routerTree, (node) => {
    if (node.meta && node.meta.hasRole && !store.hasRole(node.meta.hasRole)) {
      node.meta.hideInMenu = true;
    }
})

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

let socket = null;
let wsProtocol = 'ws';
if (window.location.protocol === 'https:') {
  wsProtocol = 'wss';
}
const host = window.location.host;
let useSocket = useWebSocket(wsProtocol + '://' + host + '/socket/authKeyBoard', {
  autoReconnect: {
    delay: 500,
    onFailed: (e) => {
      console.log(e)
    }
  },
  onMessage: (w, e) => {
    const message = JSON.parse(e.data);
    switch (message.event) {
      case "AUTH_KEYBOARD":
        authKeyBoardServerKey.value = message.data
        authKeyBoardVisible.value = true
        break;
      case "SESSION":
        store.setSession(message.data)
        break;
      case "close":
        close();
        break;
    }
  },
  onError: (e) => {
    console.log(e)
  },
  onConnected: () => {
    socket = useSocket.ws.value;
  },
});

let authKeyBoardServerKey = ref('')
let authKeyBoardVisible = ref(false)
let authKeyBoardPassword = ref('')

const handleOk = () => {
  socket.send(JSON.stringify({serverKey: authKeyBoardServerKey.value, password: authKeyBoardPassword.value}))
  authKeyBoardVisible.value = false
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
