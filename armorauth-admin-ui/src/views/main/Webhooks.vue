<template>
  <div class="page-container">
    <div class="page-header">
      <h2>Webhook 管理</h2>
      <a-button type="primary" @click="showCreate">
        <template #icon><PlusOutlined /></template>
        添加 Webhook
      </a-button>
    </div>

    <a-table :dataSource="webhooks" :columns="columns" :loading="loading"
             :pagination="pagination" @change="handleTableChange" row-key="id" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'events'">
          <a-tag v-for="ev in (record.eventTypes || '').split(',')" :key="ev" style="margin: 2px">{{ ev }}</a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="record.enabled ? 'green' : 'default'">
            {{ record.enabled ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-popconfirm title="确认删除此 Webhook？" @confirm="handleDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" title="添加 Webhook" @ok="handleSubmit"
             :confirmLoading="submitting" width="520px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="名称" required>
          <a-input v-model:value="form.name" placeholder="如 登录事件通知" />
        </a-form-item>
        <a-form-item label="Webhook URL" required>
          <a-input v-model:value="form.url" placeholder="https://your-server.com/webhook" />
        </a-form-item>
        <a-form-item label="签名密钥">
          <a-input v-model:value="form.secret" placeholder="用于 HMAC-SHA256 签名验证（可选）" />
        </a-form-item>
        <a-form-item label="事件类型" required>
          <a-select v-model:value="form.eventTypes" mode="multiple" placeholder="选择要订阅的事件">
            <a-select-option v-for="t in eventTypes" :key="t" :value="t">{{ t }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getWebhooks, createWebhook, deleteWebhook } from '../../api'

const webhooks = ref([])
const loading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const eventTypes = [
  'user.created', 'user.updated', 'user.deleted',
  'login.success', 'login.failure',
  'token.issued', 'token.refreshed', 'token.revoked',
  'client.secret_rotated', 'mfa.bound', 'federated.binding_created'
]

const form = reactive({ name: '', url: '', secret: '', eventTypes: [] })

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name', width: 160 },
  { title: 'URL', dataIndex: 'url', key: 'url', ellipsis: true },
  { title: '事件类型', key: 'events', width: 300 },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getWebhooks(pagination.current - 1, pagination.pageSize)
    webhooks.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (e) { message.error('加载失败') }
  finally { loading.value = false }
}

const handleTableChange = (pag) => { pagination.current = pag.current; fetchData() }

const showCreate = () => {
  form.name = ''; form.url = ''; form.secret = ''; form.eventTypes = []
  modalVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    await createWebhook({ ...form, eventTypes: form.eventTypes.join(',') })
    message.success('创建成功'); modalVisible.value = false; fetchData()
  } catch (e) { message.error(e.message) }
  finally { submitting.value = false }
}

const handleDelete = async (id) => {
  try { await deleteWebhook(id); message.success('删除成功'); fetchData() }
  catch (e) { message.error(e.message) }
}

onMounted(fetchData)
</script>

<style scoped>
</style>
