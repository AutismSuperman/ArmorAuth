<script setup lang="ts">
import { storeToRefs } from 'pinia';
import { useAppStore } from '@/stores/theme';
import { reactive, ref } from 'vue';
import { useI18nLocale } from '@/composables/i18n-locale';

interface DeviceCodeInfo {
  deviceVerificationEndpoint: string;
}

const { t } = useI18nLocale();
const appStore = useAppStore();
const { layoutSetting } = storeToRefs(appStore);
const userCode = ref('');
const info = reactive<DeviceCodeInfo>((window as any).deviceCodeInfo);
const activate = () => {
  const consentForm = document.createElement('form');
  consentForm.method = 'POST';
  consentForm.style.display = 'none';
  consentForm.action = info.deviceVerificationEndpoint;
  // form data user_code
  const clientIdInput = document.createElement('input');
  clientIdInput.name = 'user_code';
  clientIdInput.value = userCode.value;
  consentForm.appendChild(clientIdInput);
  document.body.appendChild(consentForm);
  consentForm.submit();
};
</script>

<template>
  <div class="device-activate-container">
    <div h-screen w-screen absolute>
      <canvas ref="bubbleCanvas" absolute z-10 />
    </div>
    <div class="device-activate-content flex-center flex-col">
      <div class="ant-pro-form-device-activate-main rounded">
        <!-- 登录头部 -->
        <div class="flex-between h-15 px-4 mb-[2px] rounded-2">
          <div>
            <span class="ant-pro-form-device-activate-logo">
              <img class="h-[55px] w-[64px]" src="@/assets/logo.svg" />
            </span>
            <span class="ant-pro-form-device-activate-title"> ArmorAuth </span>
            <span class="ant-pro-form-device-activate-desc">
              {{ t('activate.desc') }}
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
          <div class="ant-pro-form-device-activate-main-left flex-center flex-col px-5 relative z-11">
            <img src="@/assets/images/device-activate.svg" class="h-5/6 w-5/6" />
          </div>
          <a-divider m-0 type="vertical" class="ant-pro-device-activate-divider min-h-[550px]" />
          <div class="ant-pro-form-device-activate-main-right px-5 w-[360px] flex items-center flex-col relative z-11">
            <div class="text-center py-6 text-2xl">
              {{ t('activate.tips') }}
            </div>
            <div class="text-center py-6">
              {{ t('activate.remark') }}
            </div>
            <a-form class="w-[320px]">
              <a-form-item>
                <a-input v-model:value="userCode" allow-clear :placeholder="t('activate.code')" size="large" />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" class="w-full" size="large" @click="activate()">
                  {{ t('activate.button.submit') }}
                </a-button>
              </a-form-item>
            </a-form>
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

.ant-pro-form-device-activate-desc {
  color: var(--text-color-1);
  font-size: 14px;
  margin: 0 15px;
}

.ant-pro-form-device-activate-title {
  position: relative;
  top: 2px;
  color: var(--text-color);
  font-weight: 600;
  font-size: 33px;
}

.ant-pro-form-device-activate-main {
  box-shadow: var(--c-shadow);
}

.ant-pro-form-device-activate-logo {
  width: 44px;
  height: 44px;
  vertical-align: top;
}

.device-activate-content {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.device-activate-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: auto;
  background: var(--bg-color-container);
}

.device-activate-media(@width: 100%) {
  .ant-pro-form-device-activate-main {
    width: @width;
  }

  .ant-pro-form-device-activate-main-left {
    display: none;
  }

  .ant-pro-form-device-activate-main-right {
    width: 100%;
  }

  .ant-pro-form-device-activate-desc {
    display: none;
  }
}

@media (min-width: 992px) {
  .ant-pro-form-device-activate-main-left {
    width: 700px;
  }
}

@media (min-width: 768px) and (max-width: 991px) {
  .ant-pro-device-activate-divider {
    display: none;
  }

  .device-activate-media(400px);
}

@media screen and (max-width: 767px) {
  .device-activate-media(350px);

  .ant-pro-device-activate-divider {
    display: none;
  }
}
</style>
