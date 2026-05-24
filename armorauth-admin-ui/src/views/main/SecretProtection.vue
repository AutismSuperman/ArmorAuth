<template>
  <div class="main-page page-container">
    <div class="page-header">
      <div>
        <h2>Secret 保护</h2>
        <div class="page-subtitle">敏感字段加密状态和 key 轮换重加密</div>
      </div>
      <a-space wrap>
        <a-button @click="runRekey(true)" :loading="dryRunLoading">
          <template #icon><ReloadOutlined /></template>
          Dry-run
        </a-button>
        <a-popconfirm
          title="确认执行 Secret 重加密？"
          ok-text="执行"
          cancel-text="取消"
          :disabled="executeDisabled"
          @confirm="runRekey(false)">
          <a-button type="primary" danger :disabled="executeDisabled" :loading="executeLoading">
            <template #icon><ThunderboltOutlined /></template>
            执行重加密
          </a-button>
        </a-popconfirm>
      </a-space>
    </div>

    <a-alert
      class="risk-alert"
      type="warning"
      show-icon
      message="执行前请确认旧 key 仍在 key-ring；Dry-run 不会修改数据。"
    />

    <a-row :gutter="[16, 16]" class="metric-grid">
      <a-col :xs="24" :sm="12" :lg="6" v-for="item in metricCards" :key="item.key">
        <a-card class="metric-card" :bordered="false">
          <a-statistic :title="item.label" :value="item.value" />
        </a-card>
      </a-col>
    </a-row>

    <div class="key-band">
      <span class="key-label">Configured keys</span>
      <a-space wrap>
        <a-tag v-for="keyId in configuredKeyIds" :key="keyId" :color="keyId === activeKeyId ? 'blue' : 'default'">
          {{ keyId }}
        </a-tag>
        <span v-if="configuredKeyIds.length === 0" class="muted">-</span>
      </a-space>
      <a-tag v-if="result" :color="result.dryRun ? 'gold' : 'green'" class="mode-tag">
        {{ result.dryRun ? 'DRY-RUN' : 'EXECUTED' }}
      </a-tag>
    </div>

    <a-table
      row-key="key"
      size="middle"
      :columns="columns"
      :dataSource="resourceRows"
      :loading="tableLoading"
      :pagination="false"
      bordered>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div class="resource-name">{{ record.name }}</div>
          <div class="resource-desc">{{ record.description }}</div>
        </template>
        <template v-if="column.key === 'health'">
          <a-tag :color="record.failed > 0 ? 'red' : record.wouldRekey > 0 ? 'gold' : 'green'">
            {{ healthText(record) }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import { rekeySecrets } from '../../api'

const emptyStats = {
  scanned: 0,
  blank: 0,
  alreadyActive: 0,
  plaintext: 0,
  differentKey: 0,
  wouldRekey: 0,
  rekeyed: 0,
  failed: 0
}

const result = ref(null)
const dryRunLoading = ref(false)
const executeLoading = ref(false)

const columns = [
  { title: '资源', key: 'name', width: 230 },
  { title: '扫描', dataIndex: 'scanned', key: 'scanned', width: 86, align: 'right' },
  { title: '空值', dataIndex: 'blank', key: 'blank', width: 86, align: 'right' },
  { title: '当前 key', dataIndex: 'alreadyActive', key: 'alreadyActive', width: 100, align: 'right' },
  { title: '旧 key', dataIndex: 'differentKey', key: 'differentKey', width: 86, align: 'right' },
  { title: '明文', dataIndex: 'plaintext', key: 'plaintext', width: 86, align: 'right' },
  { title: '待处理', dataIndex: 'wouldRekey', key: 'wouldRekey', width: 100, align: 'right' },
  { title: '已处理', dataIndex: 'rekeyed', key: 'rekeyed', width: 100, align: 'right' },
  { title: '失败', dataIndex: 'failed', key: 'failed', width: 86, align: 'right' },
  { title: '状态', key: 'health', width: 100 }
]

const resourceMeta = [
  { key: 'identityProviders', name: '身份源', description: 'OAuth/OIDC client secret' },
  { key: 'webhookEndpoints', name: 'Webhook', description: '签名密钥' },
  { key: 'authFactors', name: '认证因子', description: 'MFA secret' },
  { key: 'jwkKeys', name: 'JWK Key', description: '私钥材料' }
]

const total = computed(() => result.value?.total || emptyStats)
const activeKeyId = computed(() => result.value?.activeKeyId || '-')
const configuredKeyIds = computed(() => result.value?.configuredKeyIds || [])
const tableLoading = computed(() => dryRunLoading.value || executeLoading.value)
const executeDisabled = computed(() =>
  tableLoading.value || !result.value || !result.value.dryRun || total.value.wouldRekey === 0
)

const metricCards = computed(() => [
  { key: 'activeKeyId', label: 'Active key', value: activeKeyId.value },
  { key: 'scanned', label: '扫描总数', value: total.value.scanned },
  { key: 'wouldRekey', label: '待重加密', value: total.value.wouldRekey },
  { key: 'failed', label: '失败', value: total.value.failed }
])

const resourceRows = computed(() => resourceMeta.map(item => ({
  ...item,
  ...(result.value?.[item.key] || emptyStats)
})))

const healthText = (record) => {
  if (record.failed > 0) return '异常'
  if (record.wouldRekey > 0) return '待处理'
  return '正常'
}

const runRekey = async (dryRun = true) => {
  if (dryRun) dryRunLoading.value = true
  else executeLoading.value = true

  try {
    const res = await rekeySecrets(dryRun)
    result.value = res.data
    message.success(dryRun ? 'Dry-run 完成' : '重加密完成')
  } catch (e) {
    message.error(e.message || '操作失败')
  } finally {
    dryRunLoading.value = false
    executeLoading.value = false
  }
}

onMounted(() => runRekey(true))
</script>

<style scoped>
.page-container {
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
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

.risk-alert {
  border-radius: 6px;
}

.metric-grid {
  width: 100%;
}

.metric-card {
  min-height: 108px;
  background: #f8fafc;
  border: 1px solid #edf1f7;
  border-radius: 8px;
}

.key-band {
  min-height: 48px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fbfcff;
  border: 1px solid #edf1f7;
  border-radius: 8px;
}

.key-label {
  color: #4b5563;
  font-weight: 600;
}

.mode-tag {
  margin-left: auto;
}

.resource-name {
  font-weight: 600;
  color: #111827;
}

.resource-desc,
.muted {
  color: #6b7280;
  font-size: 12px;
}

@media (max-width: 720px) {
  .page-header,
  .key-band {
    align-items: stretch;
    flex-direction: column;
  }

  .mode-tag {
    margin-left: 0;
    width: fit-content;
  }
}
</style>
