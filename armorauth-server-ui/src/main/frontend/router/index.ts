import { createRouter, createWebHistory } from 'vue-router';
import index from '@/views/index.vue';
import Login from '@/views/Login.vue';
import Consent from '@/views/Consent.vue';
import DeviceActivate from '@/views/device/DeviceActivate.vue';
import DeviceActivated from '@/views/device/DeviceActivated.vue';
import { useMetaTitle } from '@/composables/meta-title';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      meta: {
        title: '默认页面',
      },
      component: index,
    },
    {
      path: '/login',
      name: 'login',
      meta: {
        title: '用户登录',
      },
      component: Login,
    },
    {
      path: '/consent',
      name: 'consent',
      meta: {
        title: '用户授权',
      },
      component: Consent,
    },
    {
      path: '/activate',
      name: 'deviceActivate',
      meta: {
        title: '设备激活',
      },
      component: DeviceActivate,
    },
    {
      path: '/activated',
      name: 'deviceActivated',
      meta: {
        title: '激活成功',
      },
      component: DeviceActivated,
    },
  ],
});

router.afterEach((to) => {
  useMetaTitle(to);
});

export default router;
