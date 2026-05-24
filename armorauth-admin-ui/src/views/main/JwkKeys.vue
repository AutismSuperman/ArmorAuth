<template>
  <div class="main-page page-container">
    <div class="page-header">
      <div>
        <h2>JWK 密钥</h2>
        <div class="page-subtitle">签名密钥状态、轮换和废弃</div>
      </div>
      <a-popconfirm title="确认轮换 JWK 密钥？" ok-text="轮换" cancel-text="取消" @confirm="handleRotate">
        <a-button type="primary" :loading="rotateLoading">
          <template #icon><SyncOutlined /></template>
          轮换密钥
        </a-button>
      </a-popconfirm>
    </div>

    <a-row :gutter="[16, 16]" class="metric-grid">
      <a-col :xs="24" :sm="12" :lg="6" v-for="item in metrics" :key="item.key">
        <a-card class="metric-card" :bordered="false">
          <a-statistic :title="item.label" :value="item.value" />
        </a-card>
      </a-col>
    </a-row>

    <a-table
      row-key="kid"
      size="middle"
      :columns="columns"
      :dataSource="keys"
      :loading="loading"
      :pagination="false"
      :scroll="{ x: 1100 }"
      bordered>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'kid'">
          <span class="kid">{{ record.kid }}</span>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ record.status || '-' }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-popconfirm
            v-if="canRetire(record)"
            title="确认废弃此 standby 密钥？"
            ok-text="废弃"
            cancel-text="取消"
            @confirm="handleRetire(record.kid)">
            <a style="color: #ff4d4f">废弃</a>
          </a-popconfirm>
          <span v-else class="muted">-</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { SyncOutlined } from '@ant-design/icons-vue'
import { getJwkKeys, retireJwkKey, rotateJwkKey } from '../../api'

const keys = ref([])
const loading = ref(false)
const rotateLoading = ref(false)

const columns = [
  { title: 'Kid', dataIndex: 'kid', key: 'kid', ellipsis: true },
  { title: '类型', dataIndex: 'keyType', key: 'keyType', width: 100 },
  { title: '算法', dataIndex: 'algorithm', key: 'algorithm', width: 120 },
  { title: '状态', key: 'status', width: 120 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 190 },
  { title: '过期时间', dataIndex: 'expiresAt', key: 'expiresAt', width: 190 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

const normalizeStatus = (status) => (status || '').toUpperCase()

const metrics = computed(() => {
  const active = keys.value.filter(item => normalizeStatus(item.status) === 'ACTIVE').length
  const standby = keys.value.filter(item => normalizeStatus(item.status) === 'STANDBY').length
  const retired = keys.value.filter(item => normalizeStatus(item.status) === 'RETIRED').length
  return [
    { key: 'total', label: '密钥总数', value: keys.value.length },
    { key: 'active', label: 'Active', value: active },
    { key: 'standby', label: 'Standby', value: standby },
    { key: 'retired', label: 'Retired', value: retired }
  ]
})

const statusColor = (status) => {
  const normalized = normalizeStatus(status)
  if (normalized === 'ACTIVE') return 'green'
  if (normalized === 'STANDBY') return 'blue'
  if (normalized === 'RETIRED') return 'default'
  return 'gold'
}

const canRetire = (record) => normalizeStatus(record.status) === 'STANDBY'

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getJwkKeys()
    keys.value = res.data || []
  } catch (e) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleRotate = async () => {
  rotateLoading.value = true
  try {
    const res = await rotateJwkKey()
    message.success(res.data?.message || '密钥轮换成功')
    await fetchData()
  } catch (e) {
    message.error(e.message || '轮换失败')
  } finally {
    rotateLoading.value = false
  }
}

const handleRetire = async (kid) => {
  try {
    await retireJwkKey(kid)
    message.success('密钥已废弃')
    await fetchData()
  } catch (e) {
    message.error(e.message || '废弃失败')
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page-container {
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
}

.page-subtitle {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.metric-grid {
  width: 100%;
}

.metric-card {
  min-height: 104px;
  background: #f8fafc;
  border: 1px solid #edf1f7;
  border-radius: 8px;
}

.kid {
  font-family: Consolas, 'Courier New', monospace;
}

.muted {
  color: #9ca3af;
}
</style>
