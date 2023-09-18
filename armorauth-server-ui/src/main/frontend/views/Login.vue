<script setup lang="ts">
import FooterLinks from '@/components/footer-links.vue';
import GlobalFooter from '@/components/global-footer/index.vue';
import SelectLang from '@/components/select-lang/index.vue';
import { storeToRefs } from 'pinia';
import { useAppStore } from '@/stores/theme';
import {
  QqOutlined,
  WechatOutlined,
  GithubOutlined,
  WindowsOutlined,
  AliyunOutlined,
  AlipayCircleFilled,
  LockOutlined,
  MobileOutlined,
  TaobaoCircleFilled,
  UserOutlined,
  WeiboCircleFilled,
} from '@ant-design/icons-vue';
import { reactive, ref, shallowRef } from 'vue';
import { useI18nLocale } from '@/composables/i18n-locale';
import { useInterval } from '@vueuse/core';
import { LoginCaptchaParams, LoginParams, login, loginCaptcha } from '@/api/login';
import { message } from 'ant-design-vue';
import { AxiosError } from 'axios';

const appStore = useAppStore();
const { layoutSetting } = storeToRefs(appStore);
const { t } = useI18nLocale();
const formRef = shallowRef();
const codeLoading = shallowRef(false);
const resetCounter = 60;
const submitLoading = shallowRef(false);
const errorAlert = shallowRef(false);
const bubbleCanvas = ref<HTMLCanvasElement>();
const { counter, pause, reset, resume, isActive } = useInterval(1000, {
  controls: true,
  immediate: false,
  callback(count) {
    if (count) {
      if (count === resetCounter) pause();
    }
  },
});
const getCode = async () => {
  codeLoading.value = true;
  try {
    await formRef.value.validate(['account']);
    setTimeout(() => {
      reset();
      resume();
      codeLoading.value = false;
      message.success('验证码是: 1234');
    }, 1000);
  } catch (error) {
    codeLoading.value = false;
  }
};

const errorMsg = ref('');

const loginModel = reactive({
  username: 'admin',
  password: '123456',
  account: '13103777777',
  captcha: '1234',
  type: 'username',
  remember: true,
});

const submit = async () => {
  submitLoading.value = true;
  try {
    await formRef.value?.validate();
    let data;
    if (loginModel.type === 'username') {
      let loginParams = {
        username: loginModel.username,
        password: loginModel.password,
        'remember-me': loginModel.remember,
      } as unknown as LoginParams;
      data = await login(loginParams);
    } else if (loginModel.type === 'account') {
      let loginCaptchaParams = {
        account: loginModel.account,
        captcha: loginModel.captcha,
        'remember-me': loginModel.remember,
      } as unknown as LoginCaptchaParams;
      data = await loginCaptcha(loginCaptchaParams);
    }
    message.success({
      content: data.description,
      duration: 1.5,
      onClose: () => {
        // 获取当前是否存在重定向的链接，如果存在就走重定向的地址
        window.location.href = data.redirectUri;
      },
    });
  } catch (e) {
    submitLoading.value = false;
    if (e instanceof AxiosError) {
      errorMsg.value = e.response.data.message;
      errorAlert.value = true;
    }
  }
};
</script>

<template>
  <div class="login-container">
    <div h-screen w-screen absolute>
      <canvas ref="bubbleCanvas" absolute z-10 />
    </div>
    <div class="login-content flex-center flex-col">
      <div class="ant-pro-form-login-main rounded">
        <!-- 登录头部 -->
        <div class="flex-between h-15 px-4 mb-[2px] rounded-2">
          <div>
            <span class="ant-pro-form-login-logo">
              <img class="h-[55px] w-[64px]" src="@/assets/logo.svg" />
            </span>
            <span class="ant-pro-form-login-title"> ArmorAuth </span>
            <span class="ant-pro-form-login-desc">
              {{ t('login.layouts.title') }}
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
        <!-- 登录主体 -->
        <div class="box-border flex min-h-[550px]">
          <!-- 登录框左侧 -->
          <div class="ant-pro-form-login-main-left min-h-[550px] flex-center rounded-2 bg-[var(--bg-color-container)]">
            <img src="@/assets/images/login-left.svg" class="h-5/6 w-5/6" />
          </div>
          <a-divider m-0 type="vertical" class="ant-pro-login-divider min-h-[550px]" />
          <!-- 登录框右侧 -->
          <div class="ant-pro-form-login-main-right px-5 w-[360px] flex-center flex-col relative z-11">
            <div class="text-center py-4 text-2xl -mt-18">
              {{ t('login.tips') }}
            </div>
            <a-form ref="formRef" class="w-[320px]" :model="loginModel">
              <a-tabs v-model:activeKey="loginModel.type" centered>
                <a-tab-pane key="username" :tab="t('login.accountLogin.tab')" />
                <a-tab-pane key="account" :tab="t('login.phoneLogin.tab')" />
              </a-tabs>
              <!-- 判断是否存在error -->
              <a-alert
                v-if="errorAlert && loginModel.type === 'username'"
                mb-24px
                :message="errorMsg"
                type="error"
                show-icon
              />
              <a-alert
                v-if="errorAlert && loginModel.type === 'account'"
                mb-24px
                :message="errorMsg"
                type="error"
                show-icon
              />
              <template v-if="loginModel.type === 'username'">
                <a-form-item
                  class="h-[40px]"
                  name="username"
                  :rules="[{ required: true, message: t('login.username.required') }]"
                  @press-enter="submit"
                >
                  <a-input
                    v-model:value="loginModel.username"
                    allow-clear
                    :placeholder="t('login.username.placeholder')"
                    size="large"
                    @press-enter="submit"
                  >
                    <template #prefix>
                      <UserOutlined />
                    </template>
                  </a-input>
                </a-form-item>
                <a-form-item
                  class="h-[40px]"
                  name="password"
                  :rules="[{ required: true, message: t('login.password.required') }]"
                  @press-enter="submit"
                >
                  <a-input-password
                    v-model:value="loginModel.password"
                    allow-clear
                    :placeholder="t('login.password.placeholder')"
                    size="large"
                    @press-enter="submit"
                  >
                    <template #prefix>
                      <LockOutlined />
                    </template>
                  </a-input-password>
                </a-form-item>
              </template>
              <template v-if="loginModel.type === 'account'">
                <a-form-item
                  class="h-[40px]"
                  name="account"
                  :rules="[
                    {
                      required: true,
                      message: t('login.phoneNumber.required'),
                    },
                    {
                      pattern: /^(86)?1([38][0-9]|4[579]|5[0-35-9]|6[6]|7[0135678]|9[89])[0-9]{8}$/,
                      message: t('login.phoneNumber.invalid'),
                    },
                  ]"
                >
                  <a-input
                    v-model:value="loginModel.account"
                    allow-clear
                    :placeholder="t('login.phoneNumber.placeholder')"
                    size="large"
                    @press-enter="submit"
                  >
                    <template #prefix>
                      <MobileOutlined />
                    </template>
                  </a-input>
                </a-form-item>
                <a-form-item
                  class="h-[40px]"
                  name="captcha"
                  :rules="[{ required: true, message: t('login.captcha.required') }]"
                >
                  <div flex items-center>
                    <a-input
                      v-model:value="loginModel.captcha"
                      style="flex: 1 1 0%; transition: width 0.3s ease 0s; margin-right: 8px"
                      allow-clear
                      :placeholder="t('login.captcha.placeholder')"
                      size="large"
                      @press-enter="submit"
                    >
                      <template #prefix>
                        <LockOutlined />
                      </template>
                    </a-input>
                    <a-button :loading="codeLoading" :disabled="isActive" size="large" @click="getCode">
                      <template v-if="!isActive">
                        {{ t('login.phoneLogin.getVerificationCode') }}
                      </template>
                      <template v-else>
                        {{ resetCounter - counter }}
                        {{ t('login.getCaptchaSecondText') }}
                      </template>
                    </a-button>
                  </div>
                </a-form-item>
              </template>
              <div class="mb-24px flex-between">
                <a-checkbox v-model:checked="loginModel.remember">
                  {{ t('login.rememberMe') }}
                </a-checkbox>
                <a>{{ t('login.forgotPassword') }}</a>
              </div>
              <a-button type="primary" block :loading="submitLoading" size="large" @click="submit">
                {{ t('login.submit') }}
              </a-button>
            </a-form>
            <a-divider>
              <span class="text-slate-500">{{ t('login.loginWith') }}</span>
            </a-divider>
            <div class="ant-pro-form-login-other">
              <QqOutlined class="icon" />
              <WechatOutlined class="icon" />
              <GithubOutlined class="icon" />
              <WindowsOutlined class="icon" />
              <AliyunOutlined class="icon" />
              <AlipayCircleFilled class="icon" />
              <TaobaoCircleFilled class="icon" />
              <WeiboCircleFilled class="icon" />
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

<style lang="less" scoped>
body {
  display: flex;
}

.login-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: auto;
  background: var(--bg-color-container);
}

.login-lang {
  height: 40px;
  line-height: 44px;
}

.login-content {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.ant-pro-form-login-container {
  display: flex;
  flex: 1 1;
  flex-direction: column;
  height: 100%;
  padding: 32px 0;
  overflow: auto;
  background: inherit;
}

.ant-pro-form-login-header a {
  text-decoration: none;
}

.ant-pro-form-login-title {
  position: relative;
  top: 2px;
  color: var(--text-color);
  font-weight: 600;
  font-size: 33px;
}

.ant-pro-form-login-logo {
  width: 44px;
  height: 44px;
  vertical-align: top;
}

.ant-pro-form-login-desc {
  color: var(--text-color-1);
  font-size: 14px;
  margin: 0 15px;
}

.ant-pro-form-login-main-right {
  .ant-tabs-nav-list {
    margin: 0 auto;
    font-size: 16px;
  }

  .ant-pro-form-login-other {
    line-height: 22px;
    text-align: center;
  }
}

.ant-pro-form-login-main {
  box-shadow: var(--c-shadow);
}

.icon {
  margin-left: 8px;
  color: var(--text-color-2);
  font-size: 24px;
  vertical-align: middle;
  cursor: pointer;
  transition: color 0.3s;

  &:hover {
    color: var(--pro-ant-color-primary);
  }
}

.login-media(@width: 100%) {
  .ant-pro-form-login-main {
    width: @width;
  }

  .ant-pro-form-login-main-left {
    display: none;
  }

  .ant-pro-form-login-main-right {
    width: 100%;
  }

  .ant-pro-form-login-desc {
    display: none;
  }
}

@media (min-width: 992px) {
  .ant-pro-form-login-main-left {
    width: 700px;
  }
}

@media (min-width: 768px) and (max-width: 991px) {
  .ant-pro-login-divider {
    display: none;
  }

  .login-media(400px);
}

@media screen and (max-width: 767px) {
  .login-media(350px);

  .ant-pro-login-divider {
    display: none;
  }
}
</style>
