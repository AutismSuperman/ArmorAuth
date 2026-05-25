<template>
  <a-layout class="main-layout">
    <!-- Sidebar -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      class="aa-sidebar"
      :width="240"
      :collapsed-width="64"
    >
      <div class="aa-sidebar-logo" @click="$router.push('/main/home')">
        <div class="aa-logo-icon">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L3 7v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7l-9-5z" fill="currentColor" opacity="0.15"/>
            <path d="M12 2L3 7v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7l-9-5z" stroke="currentColor" stroke-width="1.5" fill="none"/>
            <path d="M9 12l2 2 4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <transition name="fade">
          <span v-if="!collapsed" class="aa-logo-text">ArmorAuth</span>
        </transition>
      </div>

      <div class="aa-sidebar-menu">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          v-model:openKeys="openKeys"
          mode="inline"
          theme="dark"
          :inline-collapsed="collapsed"
          @click="menuClick"
        >
          <a-menu-item key="/main/home">
            <template #icon><HomeOutlined /></template>
            <span>首页</span>
          </a-menu-item>

          <a-sub-menu key="app-management">
            <template #icon><AppstoreOutlined /></template>
            <template #title>应用管理</template>
            <a-menu-item key="/main/applications">应用列表</a-menu-item>
            <a-menu-item key="/main/scopes">Scope 管理</a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="user-management">
            <template #icon><UserOutlined /></template>
            <template #title>用户与组织</template>
            <a-menu-item key="/main/users">OAuth 用户</a-menu-item>
            <a-menu-item key="/main/organizations">组织管理</a-menu-item>
            <a-menu-item key="/main/tenants">租户管理</a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="security">
            <template #icon><SafetyOutlined /></template>
            <template #title>安全</template>
            <a-menu-item key="/main/loginPolicies">登录策略</a-menu-item>
            <a-menu-item key="/main/sessions">会话管理</a-menu-item>
            <a-menu-item key="/main/secretProtection">Secret 保护</a-menu-item>
            <a-menu-item key="/main/jwkKeys">JWK 密钥</a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="identity">
            <template #icon><GlobalOutlined /></template>
            <template #title>身份源</template>
            <a-menu-item key="/main/identityProviders">身份源管理</a-menu-item>
            <a-menu-item key="/main/federatedBindings">外部账号绑定</a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="monitoring">
            <template #icon><DashboardOutlined /></template>
            <template #title>监控与审计</template>
            <a-menu-item key="/main/monitor">监控管理</a-menu-item>
            <a-menu-item key="/main/audit">审计日志</a-menu-item>
          </a-sub-menu>

          <a-menu-item key="/main/webhooks">
            <template #icon><ApiOutlined /></template>
            <span>Webhook</span>
          </a-menu-item>

          <a-menu-item key="/main/settings">
            <template #icon><SettingOutlined /></template>
            <span>设置</span>
          </a-menu-item>
        </a-menu>
      </div>
    </a-layout-sider>

    <!-- Main Content -->
    <a-layout class="aa-main-area">
      <!-- Top Header -->
      <div class="aa-header">
        <div class="aa-header-left">
          <span class="aa-collapse-trigger" @click="collapsed = !collapsed">
            <MenuUnfoldOutlined v-if="collapsed" />
            <MenuFoldOutlined v-else />
          </span>
          <a-breadcrumb class="aa-breadcrumb">
            <a-breadcrumb-item>管理控制台</a-breadcrumb-item>
            <a-breadcrumb-item>{{ currentPageTitle }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        <div class="aa-header-right">
          <a-tooltip title="刷新当前页">
            <a-button type="text" class="aa-header-btn" @click="refreshPage">
              <template #icon><ReloadOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown>
            <div class="aa-user-info">
              <a-avatar :size="32" class="aa-avatar">
                {{ avatarLetter }}
              </a-avatar>
              <span class="aa-username">管理员</span>
              <DownOutlined style="font-size: 10px; color: #9ca3af" />
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="handleLogout">
                  <template #icon><LogoutOutlined /></template>
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>

      <!-- Content -->
      <a-layout-content class="aa-content">
        <router-view :key="routeKey" />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  HomeOutlined, AppstoreOutlined, UserOutlined, SafetyOutlined,
  GlobalOutlined, DashboardOutlined, ApiOutlined, SettingOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined, ReloadOutlined,
  LogoutOutlined, DownOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const selectedKeys = ref(['/main/home'])
const openKeys = ref([])
const routeKey = ref(0)

const submenuMap = {
  '/main/applications': 'app-management',
  '/main/scopes': 'app-management',
  '/main/users': 'user-management',
  '/main/organizations': 'user-management',
  '/main/tenants': 'user-management',
  '/main/loginPolicies': 'security',
  '/main/sessions': 'security',
  '/main/secretProtection': 'security',
  '/main/jwkKeys': 'security',
  '/main/identityProviders': 'identity',
  '/main/federatedBindings': 'identity',
  '/main/monitor': 'monitoring',
  '/main/audit': 'monitoring'
}

const pageTitleMap = {
  '/main/home': '首页',
  '/main/applications': '应用管理',
  '/main/scopes': 'Scope 管理',
  '/main/users': 'OAuth 用户',
  '/main/organizations': '组织管理',
  '/main/tenants': '租户管理',
  '/main/loginPolicies': '登录策略',
  '/main/sessions': '会话管理',
  '/main/secretProtection': 'Secret 保护',
  '/main/jwkKeys': 'JWK 密钥',
  '/main/identityProviders': '身份源管理',
  '/main/federatedBindings': '外部账号绑定',
  '/main/monitor': '监控管理',
  '/main/audit': '审计日志',
  '/main/webhooks': 'Webhook 管理',
  '/main/settings': '设置'
}

const currentPageTitle = computed(() => pageTitleMap[route.path] || '首页')

const avatarLetter = computed(() => {
  const token = localStorage.getItem('admin_token') || 'A'
  try {
    const decoded = atob(token)
    return decoded.split(':')[0]?.charAt(0)?.toUpperCase() || 'A'
  } catch {
    return 'A'
  }
})

watch(
  () => route.path,
  (path) => {
    selectedKeys.value = [path]
    const parent = submenuMap[path]
    if (parent && !openKeys.value.includes(parent)) {
      openKeys.value = [...openKeys.value, parent]
    }
  },
  { immediate: true }
)

const menuClick = (data) => {
  if (data.key && !data.key.startsWith('/')) return
  if (route.path !== data.key) {
    router.push(data.key)
  }
}

const refreshPage = () => {
  routeKey.value++
}

const handleLogout = () => {
  localStorage.removeItem('admin_token')
  router.push('/login')
}
</script>

<style scoped lang="scss">
.main-layout {
  width: 100%;
  height: 100vh;
  display: flex;
  overflow: hidden;
}

.aa-sidebar {
  background: var(--aa-sidebar-bg) !important;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  z-index: 10;

  :deep(.ant-layout-sider-children) {
    display: flex;
    flex-direction: column;
  }

  :deep(.ant-layout-sider-trigger) {
    background: rgba(255, 255, 255, 0.05);
    border-top: 1px solid rgba(255, 255, 255, 0.06);
    color: rgba(255, 255, 255, 0.45);
    height: 40px;
    line-height: 40px;

    &:hover {
      color: rgba(255, 255, 255, 0.85);
      background: rgba(255, 255, 255, 0.08);
    }
  }
}

.aa-sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
  transition: padding 0.2s;

  :deep(.ant-layout-sider-collapsed) & {
    padding: 0;
    justify-content: center;
  }
}

.aa-logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  svg {
    width: 20px;
    height: 20px;
    color: #fff;
  }
}

.aa-logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
  letter-spacing: -0.01em;
}

.aa-sidebar-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 2px;
  }
}

:deep(.ant-menu-dark) {
  background: transparent;

  .ant-menu-item {
    margin: 2px 8px;
    border-radius: 8px;
    height: 40px;
    line-height: 40px;
    color: rgba(255, 255, 255, 0.65);

    &:hover {
      color: #fff;
      background: rgba(255, 255, 255, 0.08);
    }

    &-selected {
      color: #fff !important;
      background: var(--aa-primary) !important;
      box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
    }
  }

  .ant-menu-submenu {
    margin: 2px 8px;
    border-radius: 8px;

    .ant-menu-submenu-title {
      height: 40px;
      line-height: 40px;
      border-radius: 8px;
      color: rgba(255, 255, 255, 0.65);

      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.08);
      }
    }

    &-open > .ant-menu-submenu-title {
      color: rgba(255, 255, 255, 0.85);
    }
  }

  .ant-menu-sub {
    background: transparent !important;
  }
}

.aa-main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--aa-bg);
}

.aa-header {
  height: 64px;
  background: var(--aa-header-bg);
  border-bottom: 1px solid var(--aa-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  z-index: 5;
}

.aa-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.aa-collapse-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--aa-text-secondary);
  transition: all 0.2s;

  &:hover {
    background: var(--aa-bg);
    color: var(--aa-text-primary);
  }
}

.aa-breadcrumb {
  font-size: 14px;

  :deep(.ant-breadcrumb-link) {
    color: var(--aa-text-secondary);
  }

  :deep(.ant-breadcrumb-separator) {
    color: var(--aa-text-muted);
  }

  :deep(.ant-breadcrumb-link:last-child) {
    color: var(--aa-text-primary);
    font-weight: 600;
  }
}

.aa-header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.aa-header-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: var(--aa-text-secondary);

  &:hover {
    background: var(--aa-bg);
    color: var(--aa-primary);
  }
}

.aa-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: var(--aa-bg);
  }
}

.aa-avatar {
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}

.aa-username {
  font-size: 14px;
  font-weight: 500;
  color: var(--aa-text-primary);
}

.aa-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--aa-bg);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
