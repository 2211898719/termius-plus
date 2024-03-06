import {createRouter, createWebHistory} from 'vue-router'
import BasicLayout from "@/layouts/BasicLayout";
import BlankLayout from "@/layouts/BlankLayout";
import config from "@/config";
import {useAuthStore} from "@shared/store/useAuthStore";

let routes = [
  {
    path: '/',
    name: 'index',
    meta: {title: 'Home'},
    component: BasicLayout,
    redirect: '/server',
    children: [
      {
        path: '/welcome',
        name: 'welcome',
        meta: { title: '欢迎' },
        component: () => import('../views/WelcomePage.vue'),
      },
      {
        path: '/user',
        name: 'user',
        meta: {title: '用户', icon: 'UserOutlined', hasRole: 'ROLE_SUPER_ADMIN'},
        component: BlankLayout,
        redirect: '/user/index',
        children: [
          {
            path: '/user/index',
            name: 'user_index',
            meta: { title: '用户' },
            component: () => import('../views/UserPage.vue'),
          },
          {
            path: '/user/role',
            name: 'user_role',
            meta: { title: '角色' },
            component: () => import('../views/RolePage.vue'),
          },
        ]
      },
      {
        path: '/server',

        name: 'server',
        meta: { title: '服务器', icon: 'DesktopOutlined' },
        component:  () => import('../views/server/ServerPage.vue'),
      },
      // {
      //   path: '/db',
      //   name: 'db',
      //   meta: { title: '数据库', icon: 'DatabaseOutlined' },
      //   component:  () => import('../views/db/DbPage.vue'),
      // },
      {
        path: '/system',
        name: 'system',
        meta: { title: '系统', icon: 'SettingOutlined', hasRole: 'ROLE_SUPER_ADMIN' },
        component: BlankLayout,
        redirect: '/system/log',
        children: [
          {
            path: '/system/log',
            name: 'system_log',
            meta: { title: '系统日志' },
            component: () => import('../views/log/CommandLogPage.vue'),
          },
        ]
      }
      // ,
      // {
      //   path: '/split',
      //   name: 'split',
      //   meta: {title: '分屏开发', requiredAuth: false},
      //   component: () => import('../components/p-split.vue'),
      // },
    ]
  },
  {
    path: '/login',
    name: 'login',
    meta: {title: '登录', requiredAuth: false},
    component: () => import('../views/server/LoginPage.vue'),
  },

]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
});

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title + " | " + config.name;
  }

  if (to.meta.requiredAuth === undefined || to.meta.requiredAuth === true) {
    const store = useAuthStore();
    if (!store.isLogin) {
      next({name: "login"});
    }
  }

  next();
});

export default router
