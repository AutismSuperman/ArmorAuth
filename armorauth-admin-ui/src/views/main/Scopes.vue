<template>
  <div class="main-page page-container">
    <div class="page-header">
      <h2>Scope 管理</h2>
      <a-button type="primary" @click="showCreate">
        <template #icon><PlusOutlined /></template>
        新增 Scope
      </a-button>
    </div>

    <div class="toolbar">
      <a-input v-model:value="filters.clientId" allow-clear placeholder="按 Client ID 过滤" @pressEnter="fetchData" />
      <a-button @click="fetchData">
        <template #icon><SearchOutlined /></template>
        查询
      </a-button>
      <a-button @click="resetFilters">重置</a-button>
    </div>

    <a-table :dataSource="scopes" :columns="columns" :loading="loading"
             :pagination="pagination" @change="handleTableChange" row-key="rowKey" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'scope'">
          <a-tag color="blue">{{ record.scope }}</a-tag>
        </template>
        <template v-if="column.key === 'description'">
          {{ record.description || '-' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="showEdit(record)">编辑</a>
            <a-popconfirm title="确认删除此 scope？" @confirm="handleDelete(record)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" :title="isEdit ? '编辑 Scope' : '新增 Scope'"
             @ok="handleSubmit" :confirmLoading="submitting" width="560px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="Client ID" required>
          <a-input v-model:value="form.clientId" :disabled="isEdit" placeholder="请输入应用 Client ID" />
        </a-form-item>
        <a-form-item label="Scope" required>
          <a-input v-model:value="form.scope" :disabled="isEdit" placeholder="如 openid、profile、message.read" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="说明此 scope 的用途" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { createScope, deleteScope, getScopes, updateScope } from '../../api'

const scopes = ref([])
const loading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const filters = reactive({ clientId: '' })
const form = reactive({ clientId: '', scope: '', description: '' })

const columns = [
  { title: 'Client ID', dataIndex: 'clientId', key: 'clientId', ellipsis: true },
  { title: 'Scope', dataIndex: 'scope', key: 'scope', width: 180 },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

const normalizeRows = rows => rows.map(row => ({
  ...row,
  rowKey: `${row.clientId}:${row.scope}`
}))

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getScopes(pagination.current - 1, pagination.pageSize, filters.clientId)
    scopes.value = normalizeRows(res.data?.content || [])
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

const resetForm = () => {
  form.clientId = ''
  form.scope = ''
  form.description = ''
}

const resetFilters = () => {
  filters.clientId = ''
  pagination.current = 1
  fetchData()
}

const showCreate = () => {
  isEdit.value = false
  resetForm()
  modalVisible.value = true
}

const showEdit = (record) => {
  isEdit.value = true
  form.clientId = record.clientId
  form.scope = record.scope
  form.description = record.description || ''
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!form.clientId || !form.scope) {
    message.warning('Client ID 和 Scope 不能为空')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateScope(form.clientId, form.scope, { description: form.description })
      message.success('更新成功')
    } else {
      await createScope({ ...form })
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (e) {
    message.error(e.message)
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (record) => {
  try {
    await deleteScope(record.clientId, record.scope)
    message.success('删除成功')
    fetchData()
  } catch (e) {
    message.error(e.message)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page-container { flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 420px) auto auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 16px;
}
</style>
