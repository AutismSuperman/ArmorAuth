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
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { deleteFederatedBinding, getFederatedBindings } from '../../api'

const bindings = ref([])
const loading = ref(false)
const attributesVisible = ref(false)
const selectedAttributes = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const filters = reactive({ userId: '', registrationId: '' })

const columns = [
  { title: 'User ID', dataIndex: 'userId', key: 'userId', ellipsis: true },
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
