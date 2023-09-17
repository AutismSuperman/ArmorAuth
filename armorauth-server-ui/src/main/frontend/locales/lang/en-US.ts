import enUS from 'ant-design-vue/es/locale/en_US';
import { genMessage } from '../helper';

const enUSModules = import.meta.glob(['../../locales/lang/en-US/**/*.ts'], {
  eager: true,
});

export default {
  ...genMessage(enUSModules as Record<string, Record<string, any>>, 'en-US'),
  antd: enUS,
};
