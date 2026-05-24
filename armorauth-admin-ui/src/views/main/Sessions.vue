<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>会话管理</h2>
        <div class="page-subtitle">活跃会话查询和强制下线</div>
      </div>
      <a-space wrap>
        <a-input-search
          v-model:value="username"
          allow-clear
          enter-button="查询"
          placeholder="按用户名查询"
          class="user-search"
          @search="fetchData"
        />
        <a-button @click="resetQuery" :disabled="loading">
          <template #icon><ReloadOutlined /></template>
          全部
        </a-button>
      </a-space>
    </div>

    <a-row :gutter="[16, 16]" class="metric-grid">
      <a-col :xs="24" :sm="12" :lg="8" v-for="item in metrics" :key="item.key">
        <a-card class="metric-card" :bordered="false">
          <a-statistic :title="item.label" :value="item.value" />
        </a-card>
      </a-col>
    </a-row>

    <a-table
      row-key="sessionId"
      size="middle"
      :columns="columns"
      :dataSource="sessions"
      :loading="loading"
      :pagination="false"
      :scroll="{ x: 960 }"
      bordered>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sessionId'">
          <span class="session-id">{{ record.sessionId }}</span>
        </template>
        <template v-if="column.key === 'active'">
          <a-tag :color="record.active ? 'green' : 'default'">
            {{ record.active ? '活跃' : '已过期' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-popconfirm title="确认强制下线此会话？" ok-text="下线" cancel-text="取消" @confirm="handleExpire(record.sessionId)">
            <a style="color: #ff4d4f">强制下线</a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { expireSession, getSessions, getUserSessions } from '../../api'

const username = ref('')
const sessions = ref([])
const loading = ref(false)

const columns = [
  { title: 'Session ID', key: 'sessionId', width: 320, ellipsis: true },
  { title: '用户', dataIndex: 'username', key: 'username', width: 180 },
  { title: '最后访问', dataIndex: 'lastRequest', key: 'lastRequest', width: 220 },
  { title: '状态', key: 'active', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

const metrics = computed(() => {
  const active = sessions.value.filter(item => item.active).length
  const users = new Set(sessions.value.map(item => item.username).filter(Boolean)).size
  return [
    { key: 'total', label: '会话总数', value: sessions.value.length },
    { key: 'active', label: '活跃会话', value: active },
    { key: 'users', label: '涉及用户', value: users }
  ]
})

const fetchData = async () => {
  loading.value = true
  try {
    const keyword = username.value.trim()
    const res = keyword ? await getUserSessions(keyword) : await getSessions()
    sessions.value = res.data || []
  } catch (e) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  username.value = ''
  fetchData()
}

const handleExpire = async (sessionId) => {
  try {
    await expireSession(sessionId)
    message.success('会话已下线')
    await fetchData()
  } catch (e) {
    message.error(e.message || '下线失败')
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page-subtitle {
  margin-top: 6px;
  color: var(--aa-text-secondary);
  font-size: 13px;
}

.user-search {
  width: 260px;
}

.metric-grid {
  width: 100%;
}

.session-id {
  font-family: var(--aa-font-mono);
  font-size: 12px;
}

@media (max-width: 720px) {
  .page-header {
    flex-direction: column;
  }

  .user-search {
    width: 100%;
  }
}
</style>
