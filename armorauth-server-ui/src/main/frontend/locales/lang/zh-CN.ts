import zhCN from 'ant-design-vue/es/locale/zh_CN';
import { genMessage } from '../helper';

const zhCNModules = import.meta.glob(['../../locales/lang/zh-CN/**/*.ts'], {
  eager: true,
});

export default {
  ...genMessage(zhCNModules as Record<string, Record<string, any>>, 'zh-CN'),
  antd: zhCN,
};
