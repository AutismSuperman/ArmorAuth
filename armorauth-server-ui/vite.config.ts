import { fileURLToPath, URL } from 'node:url';
import {resolve} from 'path';
import { defineConfig } from 'vite';
import UnoCSS from 'unocss/vite';
import Components from 'unplugin-vue-components/vite';
import AntdvResolver from 'antdv-component-resolver';
import vue from '@vitejs/plugin-vue';

const armorauthDir = resolve(__dirname, 'src/main/frontend');
const outDir = resolve(__dirname, 'target/dist');

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    UnoCSS(),
    Components({
      resolvers: [AntdvResolver()],
      dts: 'types/components.d.ts',
      dirs: ['./components'],
    }),
  ],
  root: armorauthDir,
  build: {
    target: 'es2020',
    sourcemap: true,
    outDir,
    rollupOptions: {
      input: {
        armorauth: resolve(armorauthDir, './index.html'),
      }
    },
  },
  resolve: {
    alias: {
      '@': armorauthDir,
    },
    extensions: ['.vue', '.js', '.json', '.ts'],
  },
});
