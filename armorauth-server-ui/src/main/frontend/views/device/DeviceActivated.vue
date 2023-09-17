<script setup lang="ts">
import { storeToRefs } from 'pinia';
import { useAppStore } from '@/stores/theme';
import { useI18nLocale } from '@/composables/i18n-locale';
const { t } = useI18nLocale();
const appStore = useAppStore();
const { layoutSetting } = storeToRefs(appStore);
</script>

<template>
  <div class="device-activated-container">
    <div h-screen w-screen absolute>
      <canvas ref="bubbleCanvas" absolute z-10 />
    </div>
    <div class="device-activated-content flex-center flex-col">
      <div class="ant-pro-form-device-activated-main rounded">
        <!-- 登录头部 -->
        <div class="flex-between h-15 px-4 mb-[2px] rounded-2">
          <div>
            <span class="ant-pro-form-device-activated-logo">
              <img class="h-[55px] w-[64px]" src="@/assets/logo.svg" />
            </span>
            <span class="ant-pro-form-device-activated-title"> ArmorAuth </span>
            <span class="ant-pro-form-device-activated-desc">
              {{ t('activated.desc') }}
            </span>
          </div>
          <div class="login-lang flex-center relative z-11">
            <span
              class="flex-center cursor-pointer text-16px"
              @click="appStore.toggleTheme(layoutSetting.theme === 'dark' ? 'light' : 'dark')"
            >
              <!-- 亮色和暗黑模式切换按钮 -->
              <template v-if="layoutSetting.theme === 'light'">
                <carbon-moon />
              </template>
              <template v-else>
                <carbon-sun />
              </template>
            </span>
            <SelectLang />
          </div>
        </div>
        <a-divider m-0 />
        <div class="box-border flex min-h-[550px]">
          <div class="ant-pro-form-device-activated-main-left flex-center flex-col px-5 relative z-11">
            <img src="@/assets/images/device-activated.svg" class="h-5/6 w-5/6" />
          </div>
          <a-divider m-0 type="vertical" class="ant-pro-device-activated-divider min-h-[550px]" />
          <div class="ant-pro-form-device-activated-main-right px-5 w-[360px] flex items-center flex-col relative z-11">
            <div class="text-center py-6 text-2xl">
              {{ t('activated.tips') }}
            </div>
            <div class="text-center text-base py-3">
              {{ t('activated.remark') }}
            </div>
            <div class="text-center text-base">
              {{ t('activated.remarkContinue') }}
            </div>
          </div>
        </div>
      </div>
      <div class="py-24px px-50px min-w-[450px] bottom-2 text-14px">
        <GlobalFooter :copyright="layoutSetting.copyright" :data-theme="layoutSetting.theme">
          <template #renderFooterLinks>
            <footer-links />
          </template>
        </GlobalFooter>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
body {
  display: flex;
}

.ant-pro-form-device-activated-desc {
  color: var(--text-color-1);
  font-size: 14px;
  margin: 0 15px;
}

.ant-pro-form-device-activated-title {
  position: relative;
  top: 2px;
  color: var(--text-color);
  font-weight: 600;
  font-size: 33px;
}

.ant-pro-form-device-activated-main {
  box-shadow: var(--c-shadow);
}

.ant-pro-form-device-activated-logo {
  width: 44px;
  height: 44px;
  vertical-align: top;
}

.device-activated-content {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.device-activated-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: auto;
  background: var(--bg-color-container);
}

.device-activated-media(@width: 100%) {
  .ant-pro-form-device-activated-main {
    width: @width;
  }

  .ant-pro-form-device-activated-main-left {
    display: none;
  }

  .ant-pro-form-device-activated-main-right {
    width: 100%;
  }

  .ant-pro-form-device-activated-desc {
    display: none;
  }
}

@media (min-width: 992px) {
  .ant-pro-form-device-activated-main-left {
    width: 700px;
  }
}

@media (min-width: 768px) and (max-width: 991px) {
  .ant-pro-device-activated-divider {
    display: none;
  }

  .device-activated-media(400px);
}

@media screen and (max-width: 767px) {
  .device-activated-media(350px);

  .ant-pro-device-activated-divider {
    display: none;
  }
}
</style>
