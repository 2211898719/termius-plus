<template>
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

        <router-view />
    </pro-layout>
</template>

<script setup>
import { ref, reactive, watch } from 'vue';
import {useRouter} from 'vue-router';
import { getMenuData, clearMenuItem } from '@ant-design-vue/pro-layout';
import {useAuthStore} from "@shared/store/useAuthStore";
import config from "@/config";

const store = useAuthStore();
const locale = (i18n) => i18n;
const router = useRouter();

const { menuData } = getMenuData(clearMenuItem(router.getRoutes()));

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
