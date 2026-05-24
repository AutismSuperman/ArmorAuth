<template>
  <div class="home-page">
    <!-- Welcome Banner -->
    <div class="welcome-banner">
      <div class="welcome-text">
        <h2>欢迎使用 ArmorAuth</h2>
        <p>你的身份认证与访问管理平台</p>
      </div>
      <div class="welcome-art">
        <svg viewBox="0 0 120 80" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect x="10" y="20" width="28" height="40" rx="4" fill="rgba(255,255,255,0.2)"/>
          <rect x="46" y="10" width="28" height="50" rx="4" fill="rgba(255,255,255,0.25)"/>
          <rect x="82" y="30" width="28" height="30" rx="4" fill="rgba(255,255,255,0.2)"/>
          <circle cx="24" cy="28" r="4" fill="rgba(255,255,255,0.6)"/>
          <circle cx="60" cy="18" r="4" fill="rgba(255,255,255,0.6)"/>
          <circle cx="96" cy="38" r="4" fill="rgba(255,255,255,0.6)"/>
        </svg>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="stats-grid">
      <div class="stat-card" v-for="stat in statsCards" :key="stat.key">
        <div class="stat-icon" :style="{ background: stat.bg }">
          <component :is="stat.icon" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- Quick Navigation -->
    <div class="section-header">
      <h3>快捷导航</h3>
    </div>
    <div class="nav-grid">
      <div class="nav-card" v-for="item in routers" :key="item.title" @click="$router.push(item.path)">
        <div class="nav-icon" :style="{ color: item.color }">
          <component :is="item.icon" />
        </div>
        <div class="nav-info">
          <span class="nav-title">{{ item.title }}</span>
          <span class="nav-desc">{{ item.desc }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted, h } from 'vue'
import {
  AppstoreOutlined, UserOutlined, TeamOutlined, CloudOutlined,
  FileTextOutlined, BarChartOutlined, SafetyOutlined, KeyOutlined
} from '@ant-design/icons-vue'
import { getApplications, getUsers, getOrganizations, getIdentityProviders } from '../../api'

const stats = reactive({ applications: 0, providers: 0, users: 0, orgs: 0 })

const statsCards = reactive([
  { key: 'apps', label: '应用数量', value: 0, icon: () => h(AppstoreOutlined), bg: 'linear-gradient(135deg, #3b82f6, #60a5fa)' },
  { key: 'idps', label: '身份源数量', value: 0, icon: () => h(CloudOutlined), bg: 'linear-gradient(135deg, #8b5cf6, #a78bfa)' },
  { key: 'users', label: '用户总数', value: 0, icon: () => h(UserOutlined), bg: 'linear-gradient(135deg, #10b981, #34d399)' },
  { key: 'orgs', label: '组织数量', value: 0, icon: () => h(TeamOutlined), bg: 'linear-gradient(135deg, #f59e0b, #fbbf24)' }
])

const routers = [
  { title: '应用管理', desc: '管理 OAuth 应用', icon: AppstoreOutlined, path: '/main/applications', color: '#3b82f6' },
  { title: '用户管理', desc: '用户增删改查', icon: UserOutlined, path: '/main/users', color: '#10b981' },
  { title: '组织管理', desc: '组织架构管理', icon: TeamOutlined, path: '/main/organizations', color: '#f59e0b' },
  { title: '身份源管理', desc: '第三方登录配置', icon: CloudOutlined, path: '/main/identityProviders', color: '#8b5cf6' },
  { title: '安全策略', desc: 'MFA 与会话', icon: SafetyOutlined, path: '/main/loginPolicies', color: '#ef4444' },
  { title: '审计日志', desc: '操作审计追踪', icon: FileTextOutlined, path: '/main/audit', color: '#6366f1' },
  { title: '监控管理', desc: 'Token 签发统计', icon: BarChartOutlined, path: '/main/monitor', color: '#0ea5e9' },
  { title: '密钥管理', desc: 'JWK 与 Secret', icon: KeyOutlined, path: '/main/jwkKeys', color: '#ec4899' }
]

onMounted(async () => {
  try {
    const [appRes, userRes, orgRes, idpRes] = await Promise.allSettled([
      getApplications(0, 1),
      getUsers(0, 1),
      getOrganizations(0, 1),
      getIdentityProviders(0, 1)
    ])
    stats.applications = appRes.status === 'fulfilled' ? (appRes.value.data?.totalElements || 0) : 0
    stats.users = userRes.status === 'fulfilled' ? (userRes.value.data?.totalElements || 0) : 0
    stats.orgs = orgRes.status === 'fulfilled' ? (orgRes.value.data?.totalElements || 0) : 0
    stats.providers = idpRes.status === 'fulfilled' ? (idpRes.value.data?.totalElements || 0) : 0

    statsCards[0].value = stats.applications
    statsCards[1].value = stats.providers
    statsCards[2].value = stats.users
    statsCards[3].value = stats.orgs
  } catch (e) { /* ignore */ }
})
</script>

<style scoped lang="scss">
.home-page {
  max-width: 1200px;
  margin: 0 auto;
}

// ── Welcome Banner ──
.welcome-banner {
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 50%, #8b5cf6 100%);
  border-radius: 16px;
  padding: 32px 36px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.25);
}

.welcome-text {
  h2 {
    font-size: 24px;
    font-weight: 700;
    color: #fff;
    margin: 0 0 6px;
  }

  p {
    color: rgba(255, 255, 255, 0.75);
    font-size: 14px;
    margin: 0;
  }
}

.welcome-art {
  opacity: 0.8;

  svg {
    width: 140px;
    height: 100px;
  }
}

// ── Stats Grid ──
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: var(--aa-card-bg);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--aa-shadow);
  border: 1px solid var(--aa-border-light);
  transition: box-shadow 0.2s, transform 0.2s;

  &:hover {
    box-shadow: var(--aa-shadow-md);
    transform: translateY(-2px);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  :deep(.anticon) {
    font-size: 22px;
    color: #fff;
  }
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--aa-text-primary);
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.stat-label {
  font-size: 13px;
  color: var(--aa-text-secondary);
  margin-top: 2px;
}

// ── Section Header ──
.section-header {
  margin-bottom: 16px;

  h3 {
    font-size: 16px;
    font-weight: 700;
    color: var(--aa-text-primary);
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;

    &::before {
      content: '';
      width: 4px;
      height: 18px;
      background: var(--aa-primary);
      border-radius: 2px;
    }
  }
}

// ── Nav Grid ──
.nav-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.nav-card {
  background: var(--aa-card-bg);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  border: 1px solid var(--aa-border-light);
  box-shadow: var(--aa-shadow);
  transition: all 0.2s;

  &:hover {
    box-shadow: var(--aa-shadow-md);
    transform: translateY(-2px);
    border-color: var(--aa-primary);
  }
}

.nav-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: currentColor;
  flex-shrink: 0;

  :deep(.anticon) {
    font-size: 20px;
    color: #fff;
  }
}

.nav-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.nav-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--aa-text-primary);
}

.nav-desc {
  font-size: 12px;
  color: var(--aa-text-muted);
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .nav-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .welcome-banner {
    flex-direction: column;
    text-align: center;
    gap: 16px;
    padding: 24px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .nav-grid {
    grid-template-columns: 1fr;
  }
}
</style>
