<script setup>
import {ref} from 'vue';
import {RouterView, useRouter} from 'vue-router'
import {ConfigProvider as AConfigProvider} from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import {useAuthStore} from "@shared/store/useAuthStore";
import {useStorage} from "@vueuse/core";

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


</script>

<template>
    <a-config-provider :locale="locale" :get-popup-container="getPopupContainer">
        <RouterView />
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
