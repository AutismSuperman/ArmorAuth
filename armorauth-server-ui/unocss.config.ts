import {
  defineConfig,
  presetAttributify,
  presetIcons,
  presetTypography,
  presetUno,
  presetWebFonts,
  transformerDirectives,
  transformerVariantGroup,
} from 'unocss';
import presetChinese from 'unocss-preset-chinese';
import presetEase from 'unocss-preset-ease';
// @ts-ignore
import antdUnoTheme from './themes/antd-uno-theme.json';

export default defineConfig({
  theme: {
    ...antdUnoTheme,
  },
  presets: [
    presetUno(),
    presetAttributify(),
    presetChinese(),
    presetEase(),
    presetTypography(),
    presetIcons({
      scale: 1.2,
      warn: true,
    }),
    presetWebFonts({
      fonts: {
        sans: 'DM Sans',
        serif: 'DM Serif Display',
        mono: 'DM Mono',
      },
    }),
  ],
  shortcuts: [
    ['flex-center', 'flex items-center justify-center'],
    ['flex-between', 'flex items-center justify-between'],
  ],
  transformers: [transformerDirectives(), transformerVariantGroup()],
})
