<template>
  <div class="page-container">
    <div class="page-header">
      <h2>登录策略</h2>
      <a-button @click="fetchData">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <a-alert type="info" show-icon style="margin-bottom: 16px"
             message="应用级 MFA 可在这里直接开关；角色级 MFA 由后端内置策略控制。" />

    <a-table :dataSource="policies" :columns="columns" :loading="loading"
             :pagination="pagination" @change="handleTableChange" row-key="id" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'clientId'">
          <a-typography-text copyable>{{ record.clientId }}</a-typography-text>
        </template>
        <template v-if="column.key === 'mfaRequired'">
          <a-switch :checked="record.mfaRequired" :loading="updatingId === record.id"
                    checked-children="启用" un-checked-children="关闭"
                    @change="checked => toggleMfa(record, checked)" />
        </template>
        <template v-if="column.key === 'roleMfaRequired'">
          <a-space wrap>
            <a-tag v-for="role in record.roleMfaRequired || []" :key="role" color="orange">{{ role }}</a-tag>
            <span v-if="!record.roleMfaRequired?.length">-</span>
          </a-space>
        </template>
        <template v-if="column.key === 'updatedAt'">
          {{ formatTime(record.updatedAt) }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { getLoginPolicies, updateLoginPolicy } from '../../api'

const policies = ref([])
const loading = ref(false)
const updatingId = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '应用名称', dataIndex: 'clientName', key: 'clientName', width: 220 },
  { title: 'Client ID', dataIndex: 'clientId', key: 'clientId', ellipsis: true },
  { title: '应用 MFA', key: 'mfaRequired', width: 130 },
  { title: '角色 MFA', key: 'roleMfaRequired', width: 260 },
  { title: '更新时间', key: 'updatedAt', width: 190 }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLoginPolicies(pagination.current - 1, pagination.pageSize)
    policies.value = res.data?.content || []
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

const toggleMfa = async (record, checked) => {
  updatingId.value = record.id
  try {
    const res = await updateLoginPolicy(record.id, { mfaRequired: checked })
    Object.assign(record, res.data || { mfaRequired: checked })
    message.success(checked ? '已要求此应用登录 MFA' : '已关闭此应用登录 MFA')
  } catch (e) {
    message.error(e.message)
  } finally {
    updatingId.value = ''
  }
}

const formatTime = value => value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'

onMounted(fetchData)
</script>

<style scoped>
</style>
