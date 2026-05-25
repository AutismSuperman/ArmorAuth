<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>OAuth 用户</h2>
        <div class="page-subtitle">管理参与 OAuth2 / OIDC 登录的 user_info 身份目录</div>
      </div>
      <a-button type="primary" @click="showCreate">
        <template #icon><PlusOutlined /></template>
        创建用户
      </a-button>
    </div>

    <div class="toolbar">
      <a-input-search
        v-model:value="searchKeyword"
        allow-clear
        enter-button
        placeholder="搜索用户名、显示名称、邮箱或手机号"
        style="max-width: 420px"
        @search="handleSearch" />
      <a-button @click="refreshData">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <a-table
      :dataSource="users"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1360 }"
      row-key="id"
      size="middle"
      @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'identity'">
          <div class="identity-cell">
            <a-avatar :src="record.avatar || undefined" class="user-avatar">
              {{ avatarLetter(record) }}
            </a-avatar>
            <div class="identity-meta">
              <strong>{{ record.username }}</strong>
              <small>{{ record.displayName || '-' }}</small>
            </div>
          </div>
        </template>

        <template v-if="column.key === 'contact'">
          <div class="contact-stack">
            <span>
              {{ record.email || '-' }}
              <a-tag v-if="record.email" :color="record.emailVerified ? 'green' : 'default'">
                {{ record.emailVerified ? '已验证' : '未验证' }}
              </a-tag>
            </span>
            <span>
              {{ record.phone || '-' }}
              <a-tag v-if="record.phone" :color="record.phoneVerified ? 'green' : 'default'">
                {{ record.phoneVerified ? '已验证' : '未验证' }}
              </a-tag>
            </span>
          </div>
        </template>

        <template v-if="column.key === 'roles'">
          <a-space wrap size="small">
            <a-tag v-for="role in record.roles || []" :key="role" color="blue">{{ role }}</a-tag>
            <span v-if="!record.roles?.length">-</span>
          </a-space>
        </template>

        <template v-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
        </template>

        <template v-if="column.key === 'locked'">
          <a-tag v-if="isLocked(record)" color="orange">锁定至 {{ formatTime(record.lockedUntil) }}</a-tag>
          <a-tag v-else color="default">正常</a-tag>
        </template>

        <template v-if="column.key === 'createdAt'">
          {{ formatTime(record.createTime) }}
        </template>

        <template v-if="column.key === 'lastLogin'">
          {{ formatTime(record.lastLoginTime) }}
        </template>

        <template v-if="column.key === 'action'">
          <a-space size="small" wrap>
            <a @click="showEdit(record)">编辑</a>
            <a @click="showRoles(record)">角色</a>
            <a @click="showProfile(record)">Profile</a>
            <a @click="handleResetPwd(record)">重置密码</a>
            <a v-if="isLocked(record)" @click="handleUnlock(record)">解锁</a>
            <a v-else @click="handleLock(record)">锁定</a>
            <a @click="toggleStatus(record)">{{ isEnabled(record) ? '禁用' : '启用' }}</a>
            <a-popconfirm title="确认删除此 OAuth 用户？" @confirm="handleDelete(record)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑 OAuth 用户' : '创建 OAuth 用户'"
      :confirmLoading="submitting"
      width="720px"
      @ok="handleSubmit">
      <a-form :model="form" layout="vertical">
        <a-row :gutter="12">
          <a-col :xs="24" :md="12">
            <a-form-item label="用户名" required>
              <a-input v-model:value="form.username" :disabled="isEdit" placeholder="用于登录和 subject 识别" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="显示名称" required>
              <a-input v-model:value="form.displayName" placeholder="展示给管理员和用户的名称" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item v-if="!isEdit" label="初始密码" required>
          <a-input-password v-model:value="form.password" placeholder="至少8位，含大小写、数字和特殊字符" />
        </a-form-item>

        <a-row :gutter="12">
          <a-col :xs="24" :md="12">
            <a-form-item label="邮箱">
              <a-input v-model:value="form.email" placeholder="name@example.com" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="手机号">
              <a-input v-model:value="form.phone" placeholder="手机号或登录账号" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="头像 URL">
          <a-input v-model:value="form.avatar" placeholder="https://example.com/avatar.png" />
        </a-form-item>

        <div class="switch-row">
          <a-checkbox v-model:checked="form.emailVerified">邮箱已验证</a-checkbox>
          <a-checkbox v-model:checked="form.phoneVerified">手机号已验证</a-checkbox>
        </div>

        <a-form-item label="Profile JSON">
          <a-textarea
            v-model:value="form.profile"
            :rows="5"
            placeholder='{"locale":"zh-CN","department":"研发"}' />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="roleVisible"
      title="角色绑定"
      width="620px"
      :confirmLoading="roleSaving"
      @ok="saveRoles">
      <a-form layout="vertical">
        <a-form-item label="用户">
          <a-input :value="roleUser ? `${roleUser.displayName || roleUser.username} / ${roleUser.username}` : ''" disabled />
        </a-form-item>
        <a-form-item label="角色">
          <a-select
            v-model:value="selectedRoleIds"
            mode="multiple"
            :options="roleOptions"
            :max-tag-count="4"
            placeholder="选择用户角色" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="profileVisible"
      title="OAuth Profile"
      width="680px"
      :confirmLoading="profileSaving"
      @ok="saveProfile">
      <a-descriptions :column="1" bordered size="small" style="margin-bottom: 16px">
        <a-descriptions-item label="用户">{{ profileUser?.username || '-' }}</a-descriptions-item>
        <a-descriptions-item label="Subject">{{ profileUser?.id || '-' }}</a-descriptions-item>
      </a-descriptions>
      <a-textarea v-model:value="profileText" :rows="10" placeholder='{"locale":"zh-CN"}' />
    </a-modal>

    <a-modal
      v-model:open="resetPwdVisible"
      title="重置密码"
      :confirmLoading="resetting"
      width="420px"
      @ok="handleResetPwdSubmit">
      <a-form layout="vertical">
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="newPassword" placeholder="请输入符合策略的新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  getUsers,
  createUser,
  updateUser,
  deleteUser,
  resetPassword,
  lockUser,
  unlockUser,
  updateUserStatus,
  getRoles,
  getRoleBindings,
  createRoleBinding,
  deleteRoleBinding
} from '../../api'

const users = ref([])
const roles = ref([])
const loading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const resetPwdVisible = ref(false)
const resetting = ref(false)
const resetUserId = ref(null)
const newPassword = ref('')
const searchKeyword = ref('')
const appliedKeyword = ref('')
const roleVisible = ref(false)
const roleSaving = ref(false)
const roleUser = ref(null)
const selectedRoleIds = ref([])
const existingBindings = ref([])
const profileVisible = ref(false)
const profileSaving = ref(false)
const profileUser = ref(null)
const profileText = ref('')

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const form = reactive({
  username: '',
  password: '',
  email: '',
  phone: '',
  displayName: '',
  avatar: '',
  emailVerified: false,
  phoneVerified: false,
  profile: ''
})

const columns = [
  { title: '身份', key: 'identity', width: 240 },
  { title: '联系方式', key: 'contact', width: 300 },
  { title: '角色', key: 'roles', width: 240 },
  { title: '状态', key: 'status', width: 90 },
  { title: '锁定', key: 'locked', width: 190 },
  { title: '创建时间', key: 'createdAt', width: 170 },
  { title: '最后登录', key: 'lastLogin', width: 170 },
  { title: '操作', key: 'action', width: 360, fixed: 'right' }
]

const roleOptions = computed(() => roles.value.map(role => ({
  value: role.id,
  label: `${role.roleName || role.roleCode} (${role.roleCode})`
})))

const roleIdByCode = computed(() => Object.fromEntries(roles.value.map(role => [role.roleCode, role.id])))

const fetchRoles = async () => {
  try {
    const res = await getRoles()
    roles.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    message.error('角色加载失败: ' + e.message)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUsers(pagination.current - 1, pagination.pageSize, appliedKeyword.value)
    users.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (e) {
    message.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  fetchRoles()
  fetchData()
}

const handleSearch = (value) => {
  appliedKeyword.value = String(value || '').trim()
  pagination.current = 1
  fetchData()
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const resetForm = () => {
  form.username = ''
  form.password = ''
  form.email = ''
  form.phone = ''
  form.displayName = ''
  form.avatar = ''
  form.emailVerified = false
  form.phoneVerified = false
  form.profile = ''
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
  form.username = record.username
  form.email = record.email || ''
  form.phone = record.phone || ''
  form.displayName = record.displayName || ''
  form.avatar = record.avatar || ''
  form.emailVerified = !!record.emailVerified
  form.phoneVerified = !!record.phoneVerified
  form.profile = formatProfile(record.profile)
  modalVisible.value = true
}

const buildPayload = () => ({
  username: form.username,
  password: form.password,
  email: nullableText(form.email),
  phone: nullableText(form.phone),
  displayName: form.displayName,
  avatar: nullableText(form.avatar),
  emailVerified: form.emailVerified,
  phoneVerified: form.phoneVerified,
  profile: normalizeProfile(form.profile)
})

const handleSubmit = async () => {
  if (!form.username || !form.displayName || (!isEdit.value && !form.password)) {
    message.warning('用户名、显示名称和初始密码不能为空')
    return
  }
  let data
  try {
    data = buildPayload()
  } catch (e) {
    message.error(e.message)
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateUser(editId.value, {
        displayName: data.displayName,
        email: data.email,
        phone: data.phone,
        avatar: data.avatar,
        emailVerified: data.emailVerified,
        phoneVerified: data.phoneVerified,
        profile: data.profile
      })
      message.success('OAuth 用户已更新')
    } else {
      await createUser(data)
      message.success('OAuth 用户已创建')
    }
    modalVisible.value = false
    fetchData()
  } catch (e) {
    message.error(e.message)
  } finally {
    submitting.value = false
  }
}

const showRoles = async (record) => {
  roleUser.value = record
  selectedRoleIds.value = (record.roles || []).map(roleCode => roleIdByCode.value[roleCode]).filter(Boolean)
  roleVisible.value = true
  try {
    const res = await getRoleBindings(record.id)
    existingBindings.value = Array.isArray(res.data) ? res.data : []
    selectedRoleIds.value = existingBindings.value.map(binding => binding.roleId)
  } catch (e) {
    message.error('角色绑定加载失败: ' + e.message)
  }
}

const saveRoles = async () => {
  if (!roleUser.value) {
    return
  }
  roleSaving.value = true
  try {
    const currentRoleIds = new Set(existingBindings.value.map(binding => binding.roleId))
    const nextRoleIds = new Set(selectedRoleIds.value)
    const removed = existingBindings.value.filter(binding => !nextRoleIds.has(binding.roleId))
    const added = selectedRoleIds.value.filter(roleId => !currentRoleIds.has(roleId))

    await Promise.all(removed.map(binding => deleteRoleBinding(binding.id)))
    await Promise.all(added.map(roleId => createRoleBinding({ userId: roleUser.value.id, roleId })))

    roleVisible.value = false
    message.success('角色绑定已保存')
    fetchData()
  } catch (e) {
    message.error(e.message)
  } finally {
    roleSaving.value = false
  }
}

const showProfile = (record) => {
  profileUser.value = record
  profileText.value = formatProfile(record.profile)
  profileVisible.value = true
}

const saveProfile = async () => {
  if (!profileUser.value) {
    return
  }
  let profile
  try {
    profile = normalizeProfile(profileText.value)
  } catch (e) {
    message.error(e.message)
    return
  }
  profileSaving.value = true
  try {
    await updateUser(profileUser.value.id, { profile })
    profileVisible.value = false
    message.success('Profile 已保存')
    fetchData()
  } catch (e) {
    message.error(e.message)
  } finally {
    profileSaving.value = false
  }
}

const handleResetPwd = (record) => {
  resetUserId.value = record.id
  newPassword.value = ''
  resetPwdVisible.value = true
}

const handleResetPwdSubmit = async () => {
  if (!newPassword.value) {
    message.warning('请输入新密码')
    return
  }
  resetting.value = true
  try {
    await resetPassword(resetUserId.value, newPassword.value)
    message.success('密码已重置')
    resetPwdVisible.value = false
  } catch (e) {
    message.error(e.message)
  } finally {
    resetting.value = false
  }
}

const handleLock = async (record) => {
  try {
    await lockUser(record.id)
    message.success('已锁定 30 分钟')
    fetchData()
  } catch (e) {
    message.error(e.message)
  }
}

const handleUnlock = async (record) => {
  try {
    await unlockUser(record.id)
    message.success('已解锁')
    fetchData()
  } catch (e) {
    message.error(e.message)
  }
}

const handleDelete = async (record) => {
  try {
    await deleteUser(record.id)
    message.success('OAuth 用户已删除')
    fetchData()
  } catch (e) {
    message.error(e.message)
  }
}

const isEnabled = (record) => record.status !== 2 && record.status !== 3
const isLocked = (record) => !!record.lockedUntil

const toggleStatus = async (record) => {
  try {
    const nextEnabled = !isEnabled(record)
    await updateUserStatus(record.id, nextEnabled)
    message.success(nextEnabled ? '已启用' : '已禁用')
    fetchData()
  } catch (e) {
    message.error(e.message)
  }
}

const statusLabel = (status) => ({
  0: '启用',
  1: '冻结',
  2: '禁用',
  3: '注销'
})[status] || '未知'

const statusColor = (status) => ({
  0: 'green',
  1: 'orange',
  2: 'red',
  3: 'default'
})[status] || 'default'

const avatarLetter = (record) => (record.displayName || record.username || 'U').charAt(0).toUpperCase()

const formatTime = value => value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'

const nullableText = value => {
  const text = String(value || '').trim()
  return text || null
}

const normalizeProfile = value => {
  const text = String(value || '').trim()
  if (!text) {
    return null
  }
  try {
    return JSON.stringify(JSON.parse(text))
  } catch {
    throw new Error('Profile 必须是合法 JSON')
  }
}

const formatProfile = value => {
  const text = String(value || '').trim()
  if (!text) {
    return ''
  }
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}

onMounted(() => {
  fetchRoles()
  fetchData()
})
</script>

<style scoped>
.identity-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.user-avatar {
  background: linear-gradient(135deg, #0f766e, #2563eb);
  color: #fff;
  font-weight: 700;
  flex: 0 0 auto;
}

.identity-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.identity-meta strong {
  font-size: 14px;
  color: var(--aa-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.identity-meta small {
  color: var(--aa-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--aa-text-secondary);
}

.switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  padding: 0 0 18px;
}
</style>
