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
      <div :style="{'font-family':currentFont}" style="opacity: 0;width: 0px;height: 0px;z-index: -999">退出</div>
        <template #rightContentRender>
          <div style="margin-right: 16px">
            <fullscreen-outlined @click="toggleFullScreen" style="margin-right: 8px"/>
            <a-dropdown>
              <a class="ant-dropdown-link" @click.prevent>
                <span v-if="store.user" style="color: #fff">{{ store.user.username }}</span>
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a @click="handleChangePassword">修改密码</a>
                  </a-menu-item>
                  <a-menu-item>
                    <a @click="onLogout">退出</a>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </template>

      <a-modal
          v-model:visible="visible"
          title="修改密码"
          :confirm-loading="confirmLoading"
          @ok="handleOk"
      >
        <a-form :model="form" :rules="rules" ref="formRef">
          <a-form-item label="旧密码" name="oldPassword">
            <a-input-password type="password" v-model:value="form.oldPassword"/>
          </a-form-item>
          <a-form-item label="新密码" name="newPassword">
            <a-input-password type="password" v-model:value="form.newPassword"  />
          </a-form-item>
          <a-form-item label="确认密码" name="confirmPassword">
            <a-input-password type="password"  v-model:value="form.confirmPassword"/>
          </a-form-item>
        </a-form>
      </a-modal>

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
import {userApi} from "@/api/user";
import {message} from "ant-design-vue";

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

let visible = ref(false);
let confirmLoading = ref(false);
let form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const rules = {
  oldPassword: [
    {required: true, message: '请输入旧密码', trigger: 'blur'},
  ],
  newPassword: [
    {required: true, message: '请输入新密码', trigger: 'blur'},
    {min: 6, message: '密码长度至少6位', trigger: 'blur'},
  ],
  confirmPassword: [
    {required: true, message: '请确认密码', trigger: 'blur'},
    {min: 6, message: '密码长度至少6位', trigger: 'blur'},
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) {
          callback('两次密码输入不一致!');
        } else {
          callback();
        }
      }, trigger: 'blur'
    },
  ],
};

const handleCancel = () => {
  visible.value = false;
  confirmLoading.value = false;

  form.oldPassword = '';
  form.newPassword = '';
  form.confirmPassword = '';
}

const handleOk = async () => {
  confirmLoading.value = true;
  try {
    await userApi.changePassword({oldPassword: form.oldPassword, newPassword: form.newPassword});
    message.success('密码修改成功，3秒后自动退出');
    handleCancel();

    setTimeout(() => {
      store.logout();
    }, 3000);
  } catch (error) {
    confirmLoading.value = false;

    if (error.message) {
      message.error(error.message);
    }
  }
}

const handleChangePassword = () => {
  visible.value = true;
}

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
.ant-pro-top-nav-header-main-left{
  margin-right: 8px;
}
</style>
