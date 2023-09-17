<script setup lang="ts">
import { storeToRefs } from 'pinia';
import { useAppStore } from '@/stores/theme';
import { reactive, watch, shallowRef } from 'vue';
import { useI18nLocale } from '@/composables/i18n-locale';
import { message } from 'ant-design-vue';

interface Scope {
  scope: string;
  description: string;
}

interface ScopeInfo {
  authorizationEndpoint: string;
  clientId: string;
  clientName: string;
  state: string;
  principalName: string;
  scopes: Array<Scope>;
  previouslyApprovedScopes: Array<Scope>;
}

const formRef = shallowRef();
const { t } = useI18nLocale();
const appStore = useAppStore();
const { layoutSetting } = storeToRefs(appStore);
const info = reactive<ScopeInfo>((window as any).scopeInfo);

let scopeOptions = [];
let previouslyApprovedScopeOptionsSelected = [];
if (info?.scopes) {
  scopeOptions = info.scopes.map((scope: any) => ({
    label: scope.description,
    value: scope.scope,
  }));
  previouslyApprovedScopeOptionsSelected = info.previouslyApprovedScopes.map((scope: any) => scope.scope);
}

const state = reactive({
  indeterminate: true,
  checkAll: false,
  checkedList: previouslyApprovedScopeOptionsSelected,
});

const onCheckAllChange = (e: any) => {
  Object.assign(state, {
    checkedList: e.target.checked ? scopeOptions.map<string>(({ value }) => value) : [],
    indeterminate: false,
  });
};

watch(
  () => state.checkedList,
  (val) => {
    state.indeterminate = !!val.length && val.length < scopeOptions.length;
    state.checkAll = val.length === scopeOptions.length;
  },
);

const submitConsent = (type: string) => {
  const consentForm = document.createElement('form');
  try {
    consentForm.method = 'POST';
    consentForm.style.display = 'none';
    consentForm.action = info.authorizationEndpoint;
    // form data client_id
    const clientIdInput = document.createElement('input');
    clientIdInput.name = 'client_id';
    clientIdInput.value = info.clientId;
    consentForm.appendChild(clientIdInput);
    // form data state
    const stateInput = document.createElement('input');
    stateInput.name = 'state';
    stateInput.value = info.state;
    consentForm.appendChild(stateInput);
    if (type === 'allow') {
      for (let key in state.checkedList) {
        if (state.checkedList.hasOwnProperty(key)) {
          const input = document.createElement('input');
          input.name = 'scope';
          input.value = state.checkedList[key];
          consentForm.appendChild(input);
        }
      }
      document.body.appendChild(consentForm);
      consentForm.submit();
    }
    if (type === 'deny') {
      document.body.appendChild(consentForm);
      consentForm.submit();
    }
  } catch (e) {
    message.error({
      content: '授权出现问题请重试！',
      duration: 1,
    });
  } finally {
    document.body.removeChild(consentForm);
  }
};

const deny = () => {
  submitConsent('deny');
};

const allow = () => {
  submitConsent('allow');
};
</script>

<template>
  <div class="consent-container">
    <div h-screen w-screen absolute>
      <canvas ref="bubbleCanvas" absolute z-10 />
    </div>
    <div class="consent-content flex-center flex-col">
      <div class="ant-pro-form-consent-main rounded">
        <!-- 登录头部 -->
        <div class="flex-between h-15 px-4 mb-[2px] rounded-2">
          <div>
            <span class="ant-pro-form-consent-logo">
              <img class="h-[55px] w-[64px]" src="@/assets/logo.svg" />
            </span>
            <span class="ant-pro-form-consent-title"> ArmorAuth </span>
            <span class="ant-pro-form-consent-desc">
              {{ t('consent.layouts.title') }}
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
          <div class="ant-pro-form-consent-main-center flex-center flex-col px-5 relative z-11">
            <div class="ant-pro-form-consent-main-center-panel">
              <div class="text-16px text-center mt-2 mb-4">
                <img src="@/assets/logo.svg" class="h-[128px] w-[128px] ml-auto" />
                <span class="block">
                  <span class="text-xl font-bold">{{ info.clientName }}</span>
                  {{ t('consent.wantAccess') }}
                  <span class="text-base font-bold"> {{ t('consent.account') }}</span>
                  <span class="text-base font-bold">&nbsp;: {{ info.principalName }}</span>
                </span>
              </div>
              <div class="text-16px text-left mt-1 ml-6 mb-4">
                <span class="text-14px text-center">
                  <b> {{ t('consent.willAllow') }} {{ info.clientName }} {{ t('consent.to') }} :</b>
                </span>
              </div>
              <div class="flex ml-18 mb-2">
                <a-checkbox
                  v-model:checked="state.checkAll"
                  :indeterminate="state.indeterminate"
                  @change="onCheckAllChange"
                >
                  {{ t('consent.button.allowAll') }}
                </a-checkbox>
              </div>
              <div class="flex ml-24 mb-6">
                <a-checkbox-group
                  v-model:value="state.checkedList"
                  style="display: flex; flex-direction: column"
                  :options="scopeOptions"
                />
              </div>
              <div class="flex flex-justify-between mb-4">
                <a-button type="primary" class="block w-[80px]" @click="deny" danger size="large">
                  {{ t('consent.button.deny') }}
                </a-button>
                <a-button type="primary" class="block w-[80px]" @click="allow" size="large">
                  {{ t('consent.button.allow') }}
                </a-button>
              </div>
              <div class="text-12px text-center w-4/5 m-auto mb-2">
                <p>
                  {{ t('consent.remark') }}
                </p>
              </div>
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

.ant-pro-form-consent-desc {
  color: var(--text-color-1);
  font-size: 14px;
  margin: 0 15px;
}

.ant-pro-form-consent-title {
  position: relative;
  top: 2px;
  color: var(--text-color);
  font-weight: 600;
  font-size: 33px;
}

.ant-pro-form-consent-main {
  box-shadow: var(--c-shadow);
}

.ant-pro-form-consent-logo {
  width: 44px;
  height: 44px;
  vertical-align: top;
}

.consent-content {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.consent-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: auto;
  background: var(--bg-color-container);
}

.consent-media(@width: 100%) {
  .ant-pro-form-consent-main {
    width: @width;
  }

  .ant-pro-form-consent-main-center {
    width: 100%;
  }

  .ant-pro-form-consent-desc {
    display: none;
  }
}

@media (min-width: 992px) {
  .ant-pro-form-consent-main-center {
    width: 610px;
  }

  .ant-pro-form-consent-main-center-panel {
    width: 460px;
  }
}

@media (min-width: 768px) and (max-width: 991px) {
  .ant-pro-consent-divider {
    display: none;
  }

  .consent-media(440px);
}

@media screen and (max-width: 767px) {
  .consent-media(350px);

  .ant-pro-consent-divider {
    display: none;
  }
}
</style>
