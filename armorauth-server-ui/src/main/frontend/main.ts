import { createApp } from 'vue';
import Antd from 'ant-design-vue';
import App from '@/App.vue';
import router from '@/router';
import { mocker } from '@/mocks/browser';
import { setupI18n } from '@/locales';
import { createPinia } from 'pinia';
import { createHead } from '@vueuse/head';
import 'ant-design-vue/dist/reset.css';
import '@/assets/styles/reset.css';
import 'uno.css';

const pinia = createPinia();
const head = createHead();

if (process.env.NODE_ENV === 'development') {
  mocker.start({
    onUnhandledRequest: 'bypass',
    serviceWorker: {
      url: './mockServiceWorker.js',
    },
  });
}

async function start() {
  const app = createApp(App);
  app.use(pinia);
  app.use(Antd);
  app.use(router);
  await setupI18n(app);
  app.use(head);
  app.mount('#app');
}

start();
