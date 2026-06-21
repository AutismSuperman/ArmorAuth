<template>
  <div class="page-container">
    <div class="page-header">
      <h2>监控管理</h2>
      <a-space>
        <a-range-picker v-model:value="dateRange" @change="fetchData" />
      </a-space>
    </div>

    <a-row :gutter="[16, 16]" class="metric-grid">
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card class="metric-card" :bordered="false"><a-statistic title="登录成功" :value="stats.loginSuccess" /></a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card class="metric-card" :bordered="false"><a-statistic title="登录失败" :value="stats.loginFailure" :value-style="{ color: '#cf1322' }" /></a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card class="metric-card" :bordered="false"><a-statistic title="Token 签发" :value="stats.tokenIssued" /></a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card class="metric-card" :bordered="false"><a-statistic title="MFA 挑战" :value="stats.mfaChallenge" /></a-card>
      </a-col>
    </a-row>

    <a-card title="Token 签发统计">
      <a-table :dataSource="tokenStats" :columns="tokenColumns" :loading="loading"
               row-key="id" size="small" :pagination="false" :scroll="{ x: 960 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'count'">
            <a-tag color="blue">{{ record.count }}</a-tag>
          </template>
        </template>
      </a-table>
      <a-empty v-if="!loading && tokenStats.length === 0" description="暂无统计数据" />
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getAuditEvents, getTokenSummary } from '../../api'
import dayjs from 'dayjs'

const loading = ref(false)
const dateRange = ref([dayjs().subtract(7, 'day'), dayjs()])
const tokenStats = ref([])
const stats = reactive({ loginSuccess: 0, loginFailure: 0, tokenIssued: 0, mfaChallenge: 0 })

const tokenColumns = [
  { title: '日期', dataIndex: 'date', key: 'date', width: 120 },
  { title: 'Client ID', dataIndex: 'clientId', key: 'clientId', width: 260, ellipsis: true },
  { title: '授权类型', dataIndex: 'grantType', key: 'grantType', width: 160 },
  { title: 'Token 类型', dataIndex: 'tokenType', key: 'tokenType', width: 130 },
  { title: '签发次数', key: 'count', width: 100 },
  { title: '最近签发', dataIndex: 'lastIssuedAt', key: 'lastIssuedAt', width: 180, customRender: ({ text }) => formatTime(text) }
]

const fetchData = async () => {
  loading.value = true
  try {
    const range = dateRange.value?.[0] && dateRange.value?.[1]
      ? dateRange.value
      : [dayjs().subtract(7, 'day'), dayjs()]
    const from = range[0].format('YYYY-MM-DD')
    const to = range[1].format('YYYY-MM-DD')
    const res = await getTokenSummary(from, to)
    tokenStats.value = res.data || []
    try {
      stats.loginSuccess = (await getAuditEvents(0, 1, 'LOGIN_SUCCESS')).data?.totalElements || 0
      stats.loginFailure = (await getAuditEvents(0, 1, 'LOGIN_FAILURE')).data?.totalElements || 0
      stats.tokenIssued = (await getAuditEvents(0, 1, 'TOKEN_ISSUED')).data?.totalElements || 0
      stats.mfaChallenge = (await getAuditEvents(0, 1, 'MFA_CHALLENGE')).data?.totalElements || 0
    } catch (e) {
      message.warning('概览指标加载失败: ' + e.message)
    }
  } catch (e) {
    message.error('Token 签发统计加载失败: ' + e.message)
  }
  finally { loading.value = false }
}

const formatTime = value => value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'

onMounted(fetchData)
</script>

<style scoped>
.metric-grid {
  width: 100%;
}
</style>
