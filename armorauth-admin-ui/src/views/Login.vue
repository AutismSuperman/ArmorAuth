<template>
  <div class="login-page">
    <div class="login-bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>
    <div class="login-card">
      <div class="login-brand">
        <div class="login-logo">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L3 7v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7l-9-5z" fill="currentColor" opacity="0.15"/>
            <path d="M12 2L3 7v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7l-9-5z" stroke="currentColor" stroke-width="1.5" fill="none"/>
            <path d="M9 12l2 2 4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h1>ArmorAuth</h1>
        <p class="login-subtitle">管理控制台</p>
      </div>

      <a-form :model="form" layout="vertical" @finish="handleLogin" class="login-form">
        <a-form-item name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="form.username" size="large" placeholder="用户名">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" size="large" placeholder="密码" @keyup.enter="handleLogin">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="loading" class="login-btn">
            登 录
          </a-button>
        </a-form-item>
      </a-form>

      <div class="login-footer">
        <span>Secured by ArmorAuth</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { loginAdmin } from '../api'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const adminUser = await loginAdmin(form.username, form.password)
    message.success(`登录成功：${adminUser.displayName || adminUser.username}`)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/main')
      ? route.query.redirect
      : '/main/home'
    router.replace(redirect)
  } catch (e) {
    message.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-bg-shapes {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
  background: #fff;
}

.shape-1 {
  width: 600px;
  height: 600px;
  top: -200px;
  right: -100px;
  animation: float 20s ease-in-out infinite;
}

.shape-2 {
  width: 400px;
  height: 400px;
  bottom: -150px;
  left: -100px;
  animation: float 15s ease-in-out infinite reverse;
}

.shape-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 20%;
  animation: float 18s ease-in-out infinite 2s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

.login-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 48px 40px 36px;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 1;
}

.login-brand {
  text-align: center;
  margin-bottom: 40px;
}

.login-logo {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.3);

  svg {
    width: 32px;
    height: 32px;
    color: #fff;
  }
}

.login-brand h1 {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 4px;
  background: linear-gradient(135deg, #1f2937, #374151);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
}

.login-subtitle {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
}

.login-form {
  :deep(.ant-form-item-label > label) {
    font-weight: 500;
    color: #374151;
  }

  :deep(.ant-input-affix-wrapper) {
    border-radius: 10px;
    border-color: #e5e7eb;
    padding: 10px 14px;

    &:hover {
      border-color: #3b82f6;
    }

    &:focus,
    &.ant-input-affix-wrapper-focused {
      border-color: #3b82f6;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }
  }

  :deep(.ant-input) {
    border-radius: 10px;
    border-color: #e5e7eb;
    padding: 10px 14px;

    &:hover {
      border-color: #3b82f6;
    }

    &:focus {
      border-color: #3b82f6;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }
  }
}

.login-btn {
  height: 46px !important;
  border-radius: 10px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  background: linear-gradient(135deg, #3b82f6, #6366f1) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35) !important;
  margin-top: 8px;

  &:hover {
    background: linear-gradient(135deg, #2563eb, #4f46e5) !important;
    box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4) !important;
    transform: translateY(-1px);
  }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;

  span {
    font-size: 12px;
    color: #9ca3af;
    letter-spacing: 0.02em;
  }
}

@media (max-width: 480px) {
  .login-card {
    width: calc(100% - 32px);
    margin: 16px;
    padding: 36px 24px 28px;
  }
}
</style>
