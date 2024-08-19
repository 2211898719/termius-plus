<script setup>
import {getCurrentInstance, ref} from 'vue';
import {RouterView, useRouter} from 'vue-router'
import {ConfigProvider as AConfigProvider} from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import {useAuthStore} from "@shared/store/useAuthStore";
import {useStorage, useWebSocket} from "@vueuse/core";

const router = useRouter();

dayjs.locale('zh-cn');
const locale = ref(zhCN);

const getPopupContainer = () => {
    return document.body
}

const store = useAuthStore();
store.$onAction(({name, after}) => {
    if (name === "logout") {
        after(async (result) => {
            await router.push({name: "login"});
        });
    }
});


let socket = null;
let wsProtocol = 'ws';
if (window.location.protocol === 'https:') {
  wsProtocol = 'wss';
}
const host = window.location.host;

let sftp = new BroadcastChannel('sftp')
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
      case "SFTP_SERVER_UPLOAD_SERVER_PROGRESS":
        sftp.postMessage(message.data)
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

<template>
    <a-config-provider :locale="locale" :get-popup-container="getPopupContainer">
        <RouterView />
      <a-modal v-model:visible="authKeyBoardVisible" title="输入服务器二次验证码" @ok="handleOk">
        {{authKeyBoardServerKey}}
        <a-input v-model:value="authKeyBoardPassword"></a-input>
      </a-modal>
    </a-config-provider>
</template>

<style lang="less">
#app {
    height: 100%;
}
.ant-popconfirm {
  z-index: 1001 !important;
}


</style>
