<template>
  <div class="page-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <a-button type="primary" @click="showCreate">
        <template #icon><PlusOutlined /></template>
        创建用户
      </a-button>
    </div>

    <a-table :dataSource="users" :columns="columns" :loading="loading"
             :pagination="pagination" @change="handleTableChange" row-key="id" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="isEnabled(record) ? 'green' : 'red'">
            {{ isEnabled(record) ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'locked'">
          <a-tag v-if="record.lockedUntil" color="orange">已锁定</a-tag>
          <a-tag v-else color="default">正常</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="showEdit(record)">编辑</a>
            <a @click="handleResetPwd(record)">重置密码</a>
            <a v-if="record.lockedUntil" @click="handleUnlock(record)">解锁</a>
            <a v-else @click="handleLock(record)">锁定</a>
            <a @click="toggleStatus(record)">{{ isEnabled(record) ? '禁用' : '启用' }}</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" :title="isEdit ? '编辑用户' : '创建用户'"
             @ok="handleSubmit" :confirmLoading="submitting" width="520px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="用户名" required>
          <a-input v-model:value="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item v-if="!isEdit" label="密码" required>
          <a-input-password v-model:value="form.password" placeholder="请输入密码（至少8位，含大小写+数字+特殊字符）" />
        </a-form-item>
        <a-form-item label="邮箱">
          <a-input v-model:value="form.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="手机号">
          <a-input v-model:value="form.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item label="显示名称">
          <a-input v-model:value="form.displayName" placeholder="请输入显示名称" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="resetPwdVisible" title="重置密码" @ok="handleResetPwdSubmit"
             :confirmLoading="resetting" width="400px">
      <a-form layout="vertical">
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="newPassword" placeholder="请输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getUsers, createUser, updateUser, resetPassword, lockUser, unlockUser, updateUserStatus } from '../../api'

const users = ref([])
const loading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const resetPwdVisible = ref(false)
const resetting = ref(false)
const resetUserId = ref(null)
const newPassword = ref('')

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const form = reactive({ username: '', password: '', email: '', phone: '', displayName: '' })

const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 150 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 200, ellipsis: true },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 140 },
  { title: '显示名称', dataIndex: 'displayName', key: 'displayName', width: 150 },
  { title: '状态', key: 'status', width: 80 },
  { title: '锁定', key: 'locked', width: 80 },
  { title: '最后登录', dataIndex: 'lastLoginTime', key: 'lastLogin', width: 170 },
  { title: '操作', key: 'action', width: 260, fixed: 'right' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUsers(pagination.current - 1, pagination.pageSize)
    users.value = res.data?.content || []
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
  form.username = ''; form.password = ''; form.email = ''; form.phone = ''; form.displayName = ''
  editId.value = null
}

const showCreate = () => { isEdit.value = false; resetForm(); modalVisible.value = true }

const showEdit = (record) => {
  isEdit.value = true; editId.value = record.id
  form.username = record.username; form.email = record.email || ''
  form.phone = record.phone || ''; form.displayName = record.displayName || ''
  modalVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateUser(editId.value, { email: form.email, phone: form.phone, displayName: form.displayName })
      message.success('更新成功')
    } else {
      await createUser(form)
      message.success('创建成功')
    }
    modalVisible.value = false; fetchData()
  } catch (e) { message.error(e.message) }
  finally { submitting.value = false }
}

const handleResetPwd = (record) => {
  resetUserId.value = record.id; newPassword.value = ''; resetPwdVisible.value = true
}

const handleResetPwdSubmit = async () => {
  resetting.value = true
  try {
    await resetPassword(resetUserId.value, newPassword.value)
    message.success('密码已重置'); resetPwdVisible.value = false
  } catch (e) { message.error(e.message) }
  finally { resetting.value = false }
}

const handleLock = async (record) => {
  try { await lockUser(record.id); message.success('已锁定'); fetchData() }
  catch (e) { message.error(e.message) }
}

const handleUnlock = async (record) => {
  try { await unlockUser(record.id); message.success('已解锁'); fetchData() }
  catch (e) { message.error(e.message) }
}

const isEnabled = (record) => record.status !== 2

const toggleStatus = async (record) => {
  try {
    const nextEnabled = !isEnabled(record)
    await updateUserStatus(record.id, nextEnabled)
    message.success(nextEnabled ? '已启用' : '已禁用'); fetchData()
  } catch (e) { message.error(e.message) }
}

onMounted(fetchData)
</script>

<style scoped>
</style>
