import { reactive, watch } from 'vue';
import { theme as antdTheme } from 'ant-design-vue/es';
import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context';
import { isDark, toggleDark } from '@/composables/theme';
import { defineStore } from 'pinia';

type ThemeType = 'light' | 'dark' | 'inverted';

export interface LayoutSetting {
  theme: ThemeType;
  colorPrimary?: string;
  copyright: string;
}

export const useAppStore = defineStore('app-theme', () => {
  const layoutSetting = reactive<LayoutSetting>({
    theme: 'light',
    colorPrimary: '#1677FF',
    copyright: 'ArmorAuth 2023',
  });
  const themeConfig = reactive<ThemeConfig>({
    algorithm: antdTheme.defaultAlgorithm,
    token: {
      colorBgContainer: '#fff',
      colorPrimary: layoutSetting.colorPrimary,
    },
    components: {},
  });

  const toggleTheme = (theme: ThemeType) => {
    if (layoutSetting.theme === theme) return;
    layoutSetting.theme = theme;
    if (theme === 'light' || theme === 'inverted') {
      if (themeConfig.token) themeConfig.token.colorBgContainer = '#fff';
      if (themeConfig.components?.Menu) delete themeConfig.components.Menu;
      themeConfig.algorithm = antdTheme.defaultAlgorithm;
      toggleDark(false);
    } else if (theme === 'dark') {
      toggleDark(true);
      if (themeConfig.token) themeConfig.token.colorBgContainer = 'rgb(36, 37, 37)';
      if (themeConfig.components) {
        themeConfig.components = {
          ...themeConfig.components,
          Menu: {
            colorItemBg: 'rgb(36, 37, 37)',
            colorSubItemBg: 'rgb(36, 37, 37)',
            menuSubMenuBg: 'rgb(36, 37, 37)',
          } as any,
        };
      }
      themeConfig.algorithm = antdTheme.darkAlgorithm;
    }
  };
  if (isDark.value) toggleTheme('dark');
  // 监听isDark的变化
  watch(isDark, () => {
    if (isDark.value) toggleTheme('dark');
    else toggleTheme('light');
  });
  return {
    layoutSetting,
    theme: themeConfig,
    toggleTheme,
  };
});
