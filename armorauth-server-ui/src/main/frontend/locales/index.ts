import { I18nOptions, createI18n } from 'vue-i18n';
import { App, nextTick } from 'vue';
import { useLocaleStore } from '@/stores/locale';
import { storeToRefs } from 'pinia';

export let i18n: ReturnType<typeof createI18n>;

async function createI18nOptions(): Promise<I18nOptions> {
  const localeStore = useLocaleStore();
  const { locale } = storeToRefs(localeStore);
  const defaultLocal = await import(`./lang/${locale.value}.ts`);
  return {
    legacy: false,
    locale: locale.value,
    fallbackLocale: locale.value,
    messages: {
      [locale.value]: defaultLocal.default,
    },
    sync: true, //If you don’t want to inherit locale from global scope, you need to set sync of i18n component option to false.
    silentTranslationWarn: true, // true - warning off
    missingWarn: false,
    silentFallbackWarn: true,
  };
}

export async function setupI18n(app: App) {
  const options = await createI18nOptions();
  i18n = createI18n(options);
  app.use(i18n);
}

/**
 * 异步加载其他多语言
 * @param locale 语言类型
 * @returns 返回参数
 */
export const loadLanguageAsync = async (locale: string) => {
  const current = (i18n.global.locale as any).value;
  try {
    if (current === locale) return nextTick();
    const messages = await import(`./lang/${locale}.ts`);
    if (messages) i18n.global.setLocaleMessage(locale, messages.default);
  } catch (e) {
    console.warn('load language error', e);
  }
  return nextTick();
};
