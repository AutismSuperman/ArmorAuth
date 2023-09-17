import { defineStore } from 'pinia';
import { useStorage } from '@vueuse/core';

export interface LocaleState {
  locale: string;
}

const LOCALE_KEY = 'locale';

export const userLocaleLocalStorage = useStorage(LOCALE_KEY, 'zh-CN');

export const useLocaleStore = defineStore({
  id: 'app-locale',
  state: (): LocaleState => ({
    locale: userLocaleLocalStorage.value,
  }),
  getters: {
    getLocale(state): string {
      return state.locale ?? 'zh-CN';
    },
  },
  actions: {
    setLocaleInfo(info: string) {
      userLocaleLocalStorage.value = info;
    },
  },
});
