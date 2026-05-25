<template>
  <div class="page-container">
    <div class="page-header">
      <h2>外部账号绑定</h2>
      <a-button @click="fetchData">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <div class="toolbar">
      <a-input v-model:value="filters.userId" allow-clear placeholder="按 User ID 过滤" @pressEnter="search" />
      <a-input v-model:value="filters.registrationId" allow-clear placeholder="按 Registration ID 过滤" @pressEnter="search" />
      <a-button type="primary" @click="search">
        <template #icon><SearchOutlined /></template>
        查询
      </a-button>
      <a-button @click="resetFilters">重置</a-button>
    </div>

    <a-table :dataSource="bindings" :columns="columns" :loading="loading"
             :pagination="pagination" @change="handleTableChange" row-key="id" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'localUser'">
          <div class="local-user">
            <a
              v-if="canOpenUser(record)"
              class="local-user__name"
              href="#"
              @click.prevent="openUserDetail(record)">
              {{ localUserName(record) }}
            </a>
            <span v-else class="local-user__name local-user__name--missing">{{ localUserName(record) }}</span>
            <div v-if="record.email" class="local-user__email">{{ record.email }}</div>
            <a-typography-text class="local-user__id" :copyable="{ text: record.userId }">
              {{ record.userId }}
            </a-typography-text>
          </div>
        </template>
        <template v-if="column.key === 'registrationId'">
          <a-tag color="blue">{{ record.registrationId }}</a-tag>
        </template>
        <template v-if="column.key === 'time'">
          <div>创建：{{ formatTime(record.createTime) }}</div>
          <div>最近：{{ formatTime(record.lastLoginTime) }}</div>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="showAttributes(record)">属性</a>
            <a-popconfirm title="确认解除此外部账号绑定？" @confirm="handleDelete(record.id)">
              <a style="color: #ff4d4f">解除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="attributesVisible" title="Provider Attributes" :footer="null" width="720px">
      <pre class="json-preview">{{ selectedAttributes }}</pre>
    </a-modal>

    <a-drawer
      v-model:open="userDetailVisible"
      title="本地用户详情"
      width="560"
      placement="right">
      <a-skeleton :loading="userDetailLoading" active>
        <div v-if="selectedUser" class="user-detail">
          <div class="user-detail__header">
            <a-avatar :src="selectedUser.avatar || undefined" :size="48" class="user-detail__avatar">
              {{ avatarLetter(selectedUser) }}
            </a-avatar>
            <div class="user-detail__identity">
              <h3>{{ selectedUser.displayName || selectedUser.username || '-' }}</h3>
              <span>{{ selectedUser.username || '-' }}</span>
            </div>
            <a-tag :color="statusColor(selectedUser.status)">
              {{ statusLabel(selectedUser.status) }}
            </a-tag>
          </div>

          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="Subject">
              <a-typography-text :copyable="{ text: selectedUser.id }">{{ selectedUser.id }}</a-typography-text>
            </a-descriptions-item>
            <a-descriptions-item label="邮箱">
              {{ selectedUser.email || '-' }}
              <a-tag v-if="selectedUser.email" :color="selectedUser.emailVerified ? 'green' : 'default'">
                {{ selectedUser.emailVerified ? '已验证' : '未验证' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="手机号">
              {{ selectedUser.phone || '-' }}
              <a-tag v-if="selectedUser.phone" :color="selectedUser.phoneVerified ? 'green' : 'default'">
                {{ selectedUser.phoneVerified ? '已验证' : '未验证' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="角色">
              <a-space v-if="selectedUser.roles?.length" wrap size="small">
                <a-tag v-for="role in selectedUser.roles" :key="role" color="blue">{{ role }}</a-tag>
              </a-space>
              <span v-else>-</span>
            </a-descriptions-item>
            <a-descriptions-item label="锁定">
              <a-tag v-if="isLocked(selectedUser)" color="orange">锁定至 {{ formatTime(selectedUser.lockedUntil) }}</a-tag>
              <span v-else>正常</span>
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatTime(selectedUser.createTime) }}</a-descriptions-item>
            <a-descriptions-item label="最近登录">{{ formatTime(selectedUser.lastLoginTime) }}</a-descriptions-item>
          </a-descriptions>

          <div class="detail-section">
            <div class="detail-section__title">Profile JSON</div>
            <pre class="json-preview user-profile-preview">{{ formatJson(selectedUser.profile) }}</pre>
          </div>
        </div>
        <a-empty v-else description="未找到本地用户" />
      </a-skeleton>
    </a-drawer>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { deleteFederatedBinding, getFederatedBindings, getUser } from '../../api'

const bindings = ref([])
const loading = ref(false)
const attributesVisible = ref(false)
const userDetailVisible = ref(false)
const userDetailLoading = ref(false)
const selectedAttributes = ref('')
const selectedUser = ref(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const filters = reactive({ userId: '', registrationId: '' })

const columns = [
  { title: '本地用户', key: 'localUser', width: 260 },
  { title: '身份源', dataIndex: 'registrationId', key: 'registrationId', width: 160 },
  { title: 'Provider User ID', dataIndex: 'providerUserId', key: 'providerUserId', ellipsis: true },
  { title: 'Provider Username', dataIndex: 'providerUsername', key: 'providerUsername', width: 180 },
  { title: '时间', key: 'time', width: 230 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getFederatedBindings(pagination.current - 1, pagination.pageSize, filters)
    bindings.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (e) {
    message.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const search = () => {
  pagination.current = 1
  fetchData()
}

const resetFilters = () => {
  filters.userId = ''
  filters.registrationId = ''
  search()
}

const showAttributes = (record) => {
  try {
    selectedAttributes.value = JSON.stringify(JSON.parse(record.providerAttributes || '{}'), null, 2)
  } catch (e) {
    selectedAttributes.value = record.providerAttributes || '{}'
  }
  attributesVisible.value = true
}

const openUserDetail = async (record) => {
  if (!record?.userId) {
    return
  }
  selectedUser.value = {
    id: record.userId,
    username: record.username,
    displayName: record.displayName,
    email: record.email
  }
  userDetailVisible.value = true
  userDetailLoading.value = true
  try {
    const res = await getUser(record.userId)
    selectedUser.value = res.data || selectedUser.value
  } catch (e) {
    message.error('用户详情加载失败: ' + e.message)
  } finally {
    userDetailLoading.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await deleteFederatedBinding(id)
    message.success('绑定已解除')
    fetchData()
  } catch (e) {
    message.error(e.message)
  }
}

const formatTime = value => value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'
const localUserName = record => record.displayName || record.username || '未匹配到本地用户'
const canOpenUser = record => !!record.userId && !!(record.username || record.displayName || record.email)
const avatarLetter = record => (record.displayName || record.username || 'U').charAt(0).toUpperCase()
const isLocked = record => !!record.lockedUntil && dayjs(record.lockedUntil).isAfter(dayjs())
const statusLabel = status => ({
  0: '启用',
  1: '冻结',
  2: '禁用',
  3: '注销'
})[status] || '未知'
const statusColor = status => ({
  0: 'green',
  1: 'orange',
  2: 'red',
  3: 'default'
})[status] || 'default'
const formatJson = value => {
  const text = String(value || '').trim()
  if (!text) {
    return '{}'
  }
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 16px;
}
.local-user {
  min-width: 0;
}
.local-user__name {
  display: block;
  color: var(--aa-text-primary);
  font-weight: 600;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.local-user__name:hover {
  color: var(--aa-primary);
}
.local-user__name--missing,
.local-user__name--missing:hover {
  color: var(--aa-text-secondary);
  cursor: default;
}
.local-user__email,
.local-user__id {
  color: var(--aa-text-secondary);
  display: block;
  font-size: 12px;
  line-height: 1.55;
  max-width: 100%;
}
.local-user__email {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.local-user__id {
  word-break: break-all;
}
.user-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.user-detail__header {
  align-items: center;
  border-bottom: 1px solid var(--aa-border);
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
}
.user-detail__avatar {
  background: linear-gradient(135deg, #0f766e, #2563eb);
  color: #fff;
  flex: 0 0 auto;
  font-weight: 700;
}
.user-detail__identity {
  flex: 1;
  min-width: 0;
}
.user-detail__identity h3 {
  color: var(--aa-text-primary);
  font-size: 18px;
  line-height: 1.35;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-detail__identity span {
  color: var(--aa-text-secondary);
  display: block;
  font-size: 13px;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.detail-section__title {
  color: var(--aa-text-primary);
  font-weight: 600;
}
.user-profile-preview {
  max-height: 260px;
}
.json-preview {
  background: var(--aa-bg-light);
  border: 1px solid var(--aa-border);
  border-radius: var(--aa-radius-sm);
  margin: 0;
  max-height: 420px;
  overflow: auto;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--aa-font-mono);
  font-size: 13px;
}
</style>
