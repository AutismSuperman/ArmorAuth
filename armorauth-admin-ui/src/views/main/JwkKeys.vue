<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>JWK 密钥</h2>
        <div class="page-subtitle">签名密钥状态、算法轮换、废弃和删除</div>
      </div>
      <a-space wrap>
        <a-select v-model:value="rotateAlgorithm" class="algorithm-select" placeholder="签名算法">
          <a-select-option v-for="algorithm in algorithmOptions" :key="algorithm.value" :value="algorithm.value">
            {{ algorithm.label }}
          </a-select-option>
        </a-select>
        <a-popconfirm
          :title="`确认轮换为 ${rotateAlgorithm} 密钥？当前 active 密钥会变为 standby。`"
          ok-text="轮换"
          cancel-text="取消"
          @confirm="handleRotate">
          <a-button type="primary" :loading="rotateLoading">
            <template #icon><SyncOutlined /></template>
            轮换密钥
          </a-button>
        </a-popconfirm>
      </a-space>
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
      :scroll="{ x: 1300 }"
      bordered>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'kid'">
          <span class="kid">{{ record.kid }}</span>
        </template>
        <template v-if="column.key === 'algorithm'">
          <a-tag :color="algorithmColor(record.algorithm)">{{ record.algorithm || '-' }}</a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ normalizeStatus(record.status) || '-' }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-popconfirm
              v-if="canRetire(record)"
              title="确认废弃此 standby 密钥？"
              ok-text="废弃"
              cancel-text="取消"
              @confirm="handleRetire(record.kid)">
              <a style="color: #fa8c16">废弃</a>
            </a-popconfirm>
            <a-popconfirm
              v-if="canDelete(record)"
              title="确认永久删除此 JWK 密钥？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="handleDelete(record.kid)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
            <span v-if="!canRetire(record) && !canDelete(record)" class="muted">active</span>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { SyncOutlined } from '@ant-design/icons-vue'
import { deleteJwkKey, getJwkKeys, retireJwkKey, rotateJwkKey } from '../../api'

const keys = ref([])
const loading = ref(false)
const rotateLoading = ref(false)
const rotateAlgorithm = ref('RS256')

const algorithmOptions = [
  { value: 'RS256', label: 'RS256 / RSA SHA-256' },
  { value: 'RS384', label: 'RS384 / RSA SHA-384' },
  { value: 'RS512', label: 'RS512 / RSA SHA-512' },
  { value: 'PS256', label: 'PS256 / RSA-PSS SHA-256' },
  { value: 'PS384', label: 'PS384 / RSA-PSS SHA-384' },
  { value: 'PS512', label: 'PS512 / RSA-PSS SHA-512' },
  { value: 'ES256', label: 'ES256 / ECDSA P-256' },
  { value: 'ES384', label: 'ES384 / ECDSA P-384' },
  { value: 'ES512', label: 'ES512 / ECDSA P-521' }
]

const columns = [
  { title: 'Kid', dataIndex: 'kid', key: 'kid', width: 340, ellipsis: true },
  { title: '类型', dataIndex: 'keyType', key: 'keyType', width: 100 },
  { title: '算法', dataIndex: 'algorithm', key: 'algorithm', width: 150 },
  { title: '状态', key: 'status', width: 120 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 190 },
  { title: '过期时间', dataIndex: 'expiresAt', key: 'expiresAt', width: 190 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

const normalizeStatus = (status) => (status || '').toUpperCase()

const metrics = computed(() => {
  const active = keys.value.filter(item => normalizeStatus(item.status) === 'ACTIVE').length
  const standby = keys.value.filter(item => normalizeStatus(item.status) === 'STANDBY').length
  const algorithms = new Set(keys.value.map(item => item.algorithm).filter(Boolean)).size
  return [
    { key: 'total', label: '密钥总数', value: keys.value.length },
    { key: 'active', label: 'Active', value: active },
    { key: 'standby', label: 'Standby', value: standby },
    { key: 'algorithms', label: '算法种类', value: algorithms }
  ]
})

const statusColor = (status) => {
  const normalized = normalizeStatus(status)
  if (normalized === 'ACTIVE') return 'green'
  if (normalized === 'STANDBY') return 'blue'
  if (normalized === 'RETIRED') return 'default'
  return 'gold'
}

const algorithmColor = (algorithm) => {
  if ((algorithm || '').startsWith('ES')) return 'purple'
  if ((algorithm || '').startsWith('PS')) return 'geekblue'
  return 'blue'
}

const canRetire = (record) => normalizeStatus(record.status) === 'STANDBY'
const canDelete = (record) => normalizeStatus(record.status) !== 'ACTIVE'

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
    const res = await rotateJwkKey(rotateAlgorithm.value)
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

const handleDelete = async (kid) => {
  try {
    await deleteJwkKey(kid)
    message.success('密钥已删除')
    await fetchData()
  } catch (e) {
    message.error(e.message || '删除失败')
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

.algorithm-select {
  width: 220px;
}

.metric-grid {
  width: 100%;
}

.kid {
  font-family: var(--aa-font-mono);
}

.muted {
  color: var(--aa-text-muted);
}
</style>
