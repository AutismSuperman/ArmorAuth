<template>
  <div class="page-container">
    <div class="page-header">
      <h2>审计日志</h2>
      <a-space>
        <a-select v-model:value="filterEventType" placeholder="事件类型" allowClear style="width: 200px"
                  @change="fetchData">
          <a-select-option v-for="t in eventTypes" :key="t" :value="t">{{ t }}</a-select-option>
        </a-select>
        <a-input-search v-model:value="filterPrincipal" placeholder="操作者" style="width: 200px"
                        @search="fetchData" allowClear />
      </a-space>
    </div>

    <a-table :dataSource="events" :columns="columns" :loading="loading"
             :pagination="pagination" :scroll="{ x: 1360 }"
             @change="handleTableChange" row-key="id" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag :color="getEventColor(record.eventType)">{{ record.eventType }}</a-tag>
        </template>
        <template v-if="column.key === 'detail'">
          <a-tooltip :title="record.detail">
            <span class="detail-text">{{ record.detail }}</span>
          </a-tooltip>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getAuditEvents } from '../../api'

const events = ref([])
const loading = ref(false)
const filterEventType = ref(undefined)
const filterPrincipal = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const eventTypes = [
  'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGOUT', 'MFA_CHALLENGE', 'MFA_SUCCESS', 'MFA_FAILURE',
  'TOKEN_ISSUED', 'TOKEN_REFRESHED', 'TOKEN_REVOKED',
  'APPLICATION_CREATED', 'APPLICATION_UPDATED', 'APPLICATION_DELETED', 'APPLICATION_STATUS_CHANGED', 'APPLICATION_SECRET_ROTATED',
  'USER_CREATED', 'USER_UPDATED', 'USER_DELETED', 'USER_STATUS_CHANGED', 'USER_PASSWORD_RESET',
  'IDENTITY_PROVIDER_CREATED', 'IDENTITY_PROVIDER_UPDATED', 'IDENTITY_PROVIDER_DELETED',
  'ROLE_CREATED', 'ROLE_DELETED', 'ROLE_BINDING_CREATED', 'ROLE_BINDING_DELETED',
  'PERMISSION_CREATED', 'PERMISSION_DELETED', 'ROLE_PERMISSION_ASSIGNED', 'ROLE_PERMISSION_REMOVED'
]

const columns = [
  { title: '时间', dataIndex: 'createdAt', key: 'time', width: 180 },
  { title: '事件类型', key: 'type', width: 200 },
  { title: '操作者', dataIndex: 'principalName', key: 'principal', width: 150 },
  { title: '资源类型', dataIndex: 'resourceType', key: 'resource', width: 120 },
  { title: '资源ID', dataIndex: 'resourceId', key: 'resourceId', width: 200, ellipsis: true },
  { title: '详情', key: 'detail', width: 360, ellipsis: true },
  { title: 'IP', dataIndex: 'ipAddress', key: 'ip', width: 140 }
]

const getEventColor = (type) => {
  if (type?.includes('FAILURE')) return 'red'
  if (type?.includes('SUCCESS')) return 'green'
  if (type?.includes('CREATED')) return 'blue'
  if (type?.includes('DELETED')) return 'orange'
  if (type?.includes('UPDATED')) return 'cyan'
  return 'default'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAuditEvents(pagination.current - 1, pagination.pageSize,
        filterEventType.value, filterPrincipal.value || undefined)
    events.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (e) { message.error('加载失败') }
  finally { loading.value = false }
}

const handleTableChange = (pag) => { pagination.current = pag.current; fetchData() }

onMounted(fetchData)
</script>

<style scoped>
.detail-text { max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: inline-block; }
</style>
