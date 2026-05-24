<template>
  <div class="main-page page-container">
    <div class="page-header">
      <h2>应用管理</h2>
      <a-button type="primary" @click="showCreate">
        <template #icon><PlusOutlined /></template>
        创建应用
      </a-button>
    </div>

    <a-table :dataSource="applications" :columns="columns" :loading="loading"
             :pagination="pagination" @change="handleTableChange" row-key="id" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-switch :checked="record.enabled" @change="val => toggleStatus(record, val)"
                    checked-children="启用" un-checked-children="禁用" />
        </template>
        <template v-if="column.key === 'mfaRequired'">
          <a-tag :color="record.mfaRequired ? 'orange' : 'default'">
            {{ record.mfaRequired ? '是' : '否' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="showEdit(record)">编辑</a>
            <a @click="handleRotateSecret(record)">重置密钥</a>
            <a-popconfirm title="确认删除此应用？" @confirm="handleDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" :title="isEdit ? '编辑应用' : '创建应用'"
             @ok="handleSubmit" :confirmLoading="submitting" width="640px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="应用名称" required>
          <a-input v-model:value="form.clientName" placeholder="请输入应用名称" />
        </a-form-item>
        <a-form-item label="认证方式" required>
          <a-select v-model:value="form.clientAuthenticationMethods" placeholder="选择认证方式">
            <a-select-option value="client_secret_basic">Client Secret Basic</a-select-option>
            <a-select-option value="client_secret_post">Client Secret Post</a-select-option>
            <a-select-option value="none">None (Public Client)</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="授权类型" required>
          <a-select v-model:value="form.authorizationGrantTypes" mode="multiple" placeholder="选择授权类型">
            <a-select-option value="authorization_code">Authorization Code</a-select-option>
            <a-select-option value="client_credentials">Client Credentials</a-select-option>
            <a-select-option value="refresh_token">Refresh Token</a-select-option>
            <a-select-option value="urn:ietf:params:oauth:grant-type:device_code">Device Code</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="回调地址">
          <a-textarea v-model:value="form.redirectUris" :rows="2"
                      placeholder="每行一个回调地址，如 http://localhost:8080/callback" />
        </a-form-item>
        <a-form-item label="退出后重定向地址">
          <a-textarea v-model:value="form.postLogoutRedirectUris" :rows="2"
                      placeholder="每行一个地址" />
        </a-form-item>
        <a-form-item label="Scopes">
          <a-select v-model:value="form.scopes" mode="tags" placeholder="输入 scope 后回车">
          </a-select>
        </a-form-item>
        <a-form-item label="强制 MFA">
          <a-switch v-model:checked="form.mfaRequired" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="secretVisible" title="应用密钥" :footer="null">
      <a-alert type="warning" message="请妥善保管密钥，关闭后将无法再次查看明文密钥。" show-icon style="margin-bottom: 16px" />
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="Client ID">{{ secretData.clientId }}</a-descriptions-item>
        <a-descriptions-item label="Client Secret">
          <a-typography-paragraph copyable>{{ secretData.clientSecret }}</a-typography-paragraph>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getApplications, createApplication, updateApplication, deleteApplication, rotateSecret, updateAppStatus } from '../../api'

const applications = ref([])
const loading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const secretVisible = ref(false)
const secretData = ref({})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const form = reactive({
  clientName: '',
  clientAuthenticationMethods: 'client_secret_basic',
  authorizationGrantTypes: ['authorization_code', 'refresh_token'],
  redirectUris: '',
  postLogoutRedirectUris: '',
  scopes: ['openid', 'profile', 'email'],
  mfaRequired: false
})

const columns = [
  { title: '应用名称', dataIndex: 'clientName', key: 'name', width: 200 },
  { title: 'Client ID', dataIndex: 'clientId', key: 'clientId', ellipsis: true },
  { title: '认证方式', dataIndex: 'clientAuthenticationMethods', key: 'auth', width: 150 },
  { title: '授权类型', dataIndex: 'authorizationGrantTypes', key: 'grant', width: 200, ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: 'MFA', key: 'mfaRequired', width: 80 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getApplications(pagination.current - 1, pagination.pageSize)
    applications.value = res.data?.content || []
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
  form.clientName = ''
  form.clientAuthenticationMethods = 'client_secret_basic'
  form.authorizationGrantTypes = ['authorization_code', 'refresh_token']
  form.redirectUris = ''
  form.postLogoutRedirectUris = ''
  form.scopes = ['openid', 'profile', 'email']
  form.mfaRequired = false
  editId.value = null
}

const showCreate = () => {
  isEdit.value = false
  resetForm()
  modalVisible.value = true
}

const showEdit = (record) => {
  isEdit.value = true
  editId.value = record.id
  form.clientName = record.clientName
  form.clientAuthenticationMethods = record.clientAuthenticationMethods
  form.authorizationGrantTypes = record.authorizationGrantTypes?.split(',') || []
  form.redirectUris = record.redirectUris?.split(',').join('\n') || ''
  form.postLogoutRedirectUris = record.postLogoutRedirectUris?.split(',').join('\n') || ''
  form.scopes = record.scopes || []
  form.mfaRequired = record.mfaRequired || false
  modalVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const data = {
      ...form,
      authorizationGrantTypes: form.authorizationGrantTypes.join(','),
      redirectUris: form.redirectUris.split('\n').filter(Boolean).join(','),
      postLogoutRedirectUris: form.postLogoutRedirectUris.split('\n').filter(Boolean).join(',')
    }
    if (isEdit.value) {
      await updateApplication(editId.value, data)
      message.success('更新成功')
    } else {
      const res = await createApplication(data)
      if (res.data?.clientSecret) {
        secretData.value = { clientId: res.data.clientId, clientSecret: res.data.clientSecret }
        secretVisible.value = true
      }
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

const handleDelete = async (id) => {
  try {
    await deleteApplication(id)
    message.success('删除成功')
    fetchData()
  } catch (e) {
    message.error(e.message)
  }
}

const handleRotateSecret = async (record) => {
  try {
    const res = await rotateSecret(record.id)
    secretData.value = { clientId: record.clientId, clientSecret: res.data?.clientSecret }
    secretVisible.value = true
    message.success('密钥已重置')
  } catch (e) {
    message.error(e.message)
  }
}

const toggleStatus = async (record, enabled) => {
  try {
    await updateAppStatus(record.id, enabled)
    record.enabled = enabled
    message.success(enabled ? '已启用' : '已禁用')
  } catch (e) {
    message.error(e.message)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page-container { flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
