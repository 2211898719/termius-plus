<script setup>
import {ref} from 'vue';
import { RouterView } from 'vue-router'
import { ConfigProvider as AConfigProvider } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import {useRouter} from "vue-router";
import {useAuthStore} from "@shared/store/useAuthStore";

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
            console.log("logout result", result);
            await router.push({name: "login"});
        });
    }
});

</script>

<template>
    <a-config-provider :locale="locale" :get-popup-container="getPopupContainer">
        <RouterView />
    </a-config-provider>
</template>

<style lang="less">
@tailwind base;
@tailwind components;
@tailwind utilities;

#app {
    height: 100%;
}
.ant-popconfirm {
  z-index: 1001 !important;
}
</style>
