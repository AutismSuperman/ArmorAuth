import type { RouteLocationNormalizedLoaded, RouteRecordRaw } from 'vue-router';
import { i18n } from '@/locales';
import { useTitle } from '@vueuse/core';

export const useMetaTitle = (route: RouteRecordRaw | RouteLocationNormalizedLoaded) => {
  const { title, locale } = route.meta ?? {};
  if (title || locale) {
    if (locale) useTitle((i18n.global as any).t(locale));
    else useTitle(title as any);
  }
};
