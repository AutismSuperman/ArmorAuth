<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>设置</h2>
        <div class="page-subtitle">当前管理账号、登录校验和接口权限状态</div>
      </div>
      <a-space wrap>
        <a-button @click="handleValidate" :loading="validating">
          <template #icon><ReloadOutlined /></template>
          重新校验登录
        </a-button>
        <a-button danger @click="handleLogout">
          <template #icon><LogoutOutlined /></template>
          退出登录
        </a-button>
      </a-space>
    </div>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="10">
        <a-card title="当前账号" :bordered="false">
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="用户名">
              {{ adminUser.username || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="显示名称">
              {{ adminUser.displayName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="认证方式">
              HTTP Basic
            </a-descriptions-item>
            <a-descriptions-item label="登录时间">
              {{ formatDateTime(adminUser.loginAt) }}
            </a-descriptions-item>
            <a-descriptions-item label="最近校验">
              {{ formatDateTime(adminUser.verifiedAt) }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="14">
        <a-card title="权限探测" :bordered="false">
          <template #extra>
            <a-button type="primary" size="small" @click="runPermissionProbe" :loading="probing">
              检测接口权限
            </a-button>
          </template>
          <a-table
            row-key="key"
            size="small"
            :columns="probeColumns"
            :dataSource="probeRows"
            :pagination="false"
            :loading="probing"
            :scroll="{ x: 760 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'resource'">
                <div class="probe-name">{{ record.name }}</div>
                <div class="probe-path">{{ record.method }} {{ record.path }}</div>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="probeStatusColor(record.status)">
                  {{ probeStatusLabel(record.status) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'message'">
                <span class="probe-message">{{ record.message || '-' }}</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LogoutOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import {
  clearAdminAuth,
  getAdminUser,
  getApplications,
  getAuditEvents,
  getIdentityProviders,
  getJwkKeys,
  getSessions,
  getTenants,
  getUsers,
  getWebhooks,
  validateAdminSession
} from '../../api'

const router = useRouter()
const adminUserRef = ref(getAdminUser())
const validating = ref(false)
const probing = ref(false)

const adminUser = computed(() => adminUserRef.value)

const probeDefinitions = [
  { key: 'jwkKeys', name: 'JWK 密钥', method: 'GET', path: '/api/admin/v1/jwk-keys', run: () => getJwkKeys() },
  { key: 'users', name: '用户管理', method: 'GET', path: '/api/admin/v1/users', run: () => getUsers(0, 1) },
  { key: 'applications', name: '应用管理', method: 'GET', path: '/api/admin/v1/applications', run: () => getApplications(0, 1) },
  { key: 'tenants', name: '租户管理', method: 'GET', path: '/api/admin/v1/tenants', run: () => getTenants(0, 1) },
  { key: 'identityProviders', name: '身份源管理', method: 'GET', path: '/api/admin/v1/identity-providers', run: () => getIdentityProviders(0, 1) },
  { key: 'sessions', name: '会话管理', method: 'GET', path: '/api/admin/v1/sessions', run: () => getSessions() },
  { key: 'auditEvents', name: '审计日志', method: 'GET', path: '/api/admin/v1/audit-events', run: () => getAuditEvents(0, 1) },
  { key: 'webhooks', name: 'Webhook 管理', method: 'GET', path: '/api/admin/v1/webhooks', run: () => getWebhooks(0, 1) }
]

const probeRows = ref(probeDefinitions.map(item => ({
  ...item,
  status: 'idle',
  message: ''
})))

const probeColumns = [
  { title: '资源', key: 'resource', width: 300 },
  { title: '状态', key: 'status', width: 110 },
  { title: '结果', key: 'message', width: 320 }
]

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const probeStatusColor = (status) => {
  if (status === 'allowed') return 'green'
  if (status === 'denied') return 'red'
  if (status === 'error') return 'gold'
  return 'default'
}

const probeStatusLabel = (status) => {
  if (status === 'allowed') return '可访问'
  if (status === 'denied') return '无权限'
  if (status === 'error') return '异常'
  return '未检测'
}

const handleValidate = async () => {
  validating.value = true
  try {
    adminUserRef.value = await validateAdminSession()
    message.success('登录状态有效')
  } catch (e) {
    message.error(e.message || '登录校验失败')
  } finally {
    validating.value = false
  }
}

const handleLogout = () => {
  clearAdminAuth()
  router.replace('/login')
}

const runPermissionProbe = async () => {
  probing.value = true
  try {
    const nextRows = []
    for (const item of probeDefinitions) {
      const row = { ...item, status: 'idle', message: '' }
      try {
        await item.run()
        row.status = 'allowed'
        row.message = '接口可访问'
      } catch (e) {
        row.status = e.status === 403 ? 'denied' : 'error'
        row.message = e.message || '检测失败'
      }
      nextRows.push(row)
      probeRows.value = nextRows.concat(
        probeDefinitions.slice(nextRows.length).map(pending => ({ ...pending, status: 'idle', message: '' }))
      )
    }
  } finally {
    probing.value = false
  }
}
</script>

<style scoped>
.probe-name {
  color: var(--aa-text-primary);
  font-weight: 600;
}

.probe-path {
  margin-top: 4px;
  color: var(--aa-text-secondary);
  font-family: var(--aa-font-mono);
  font-size: 12px;
}

.probe-message {
  color: var(--aa-text-secondary);
}
</style>
