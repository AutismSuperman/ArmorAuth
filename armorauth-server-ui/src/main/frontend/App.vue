<template>
  <ConfigProvider :theme="theme" :locale="antd">
    <App class="h-full font-chinese antialiased">
      <RouterView />
    </App>
  </ConfigProvider>
</template>

<script setup lang="ts">
import { App, ConfigProvider } from 'ant-design-vue/es';
import { useI18nLocale } from '@/composables/i18n-locale';
import { storeToRefs } from 'pinia';
import { useAppStore } from '@/stores/theme';
const { antd } = useI18nLocale();
const appStore = useAppStore();
const { theme } = storeToRefs(appStore);
</script>
