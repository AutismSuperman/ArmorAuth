<template>
  <div class="page-container">
    <div class="page-header">
      <h2>登录策略</h2>
      <a-button @click="fetchData">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <a-table
      :dataSource="policies"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1500 }"
      row-key="id"
      size="middle"
      @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'clientId'">
          <a-typography-text copyable>{{ record.clientId }}</a-typography-text>
        </template>
        <template v-if="column.key === 'mfaRequired'">
          <a-switch
            :checked="record.mfaRequired"
            :loading="updatingId === record.id"
            checked-children="启用"
            un-checked-children="关闭"
            @change="checked => toggleMfa(record, checked)" />
        </template>
        <template v-if="column.key === 'roleMfaRequired'">
          <a-space wrap size="small">
            <a-tag v-for="role in record.roleMfaRequired || []" :key="role" color="orange">
              {{ formatRoleLabel(role) }}
            </a-tag>
            <span v-if="!record.roleMfaRequired?.length">-</span>
          </a-space>
        </template>
        <template v-if="column.key === 'enforcement'">
          <a-space wrap size="small">
            <a-tag v-if="record.mfaRequired" color="red">全员</a-tag>
            <a-tag v-if="record.roleMfaRequired?.length" color="gold">角色命中</a-tag>
            <a-tag v-if="record.mfaRequired || record.roleMfaRequired?.length" color="blue">认证成功后</a-tag>
            <span v-if="!record.mfaRequired && !record.roleMfaRequired?.length">-</span>
          </a-space>
        </template>
        <template v-if="column.key === 'updatedAt'">
          {{ formatTime(record.updatedAt) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="text" size="small" @click="showPolicyEditor(record)">
            <template #icon><SettingOutlined /></template>
            配置
          </a-button>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="editorVisible"
      title="配置登录策略"
      width="640px"
      :confirmLoading="saving"
      @ok="savePolicy">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="应用">
          <a-input :value="editingRecord?.clientName" disabled />
        </a-form-item>
        <a-form-item label="应用 MFA">
          <a-switch
            v-model:checked="editForm.mfaRequired"
            checked-children="启用"
            un-checked-children="关闭" />
        </a-form-item>
        <a-form-item label="角色 MFA">
          <a-select
            v-model:value="editForm.roleMfaRequired"
            mode="multiple"
            :options="roleSelectOptions"
            :max-tag-count="4"
            placeholder="选择需要二次验证的角色" />
        </a-form-item>
        <a-form-item label="处罚点">
          <div class="enforcement-line">
            <a-tag color="blue">账号验证成功后</a-tag>
            <a-tag color="purple">OIDC Client 命中</a-tag>
            <a-tag color="gold">无因子先进入 MFA 绑定</a-tag>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SettingOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { getLoginPolicies, updateLoginPolicy, getRoles } from '../../api'

const policies = ref([])
const roles = ref([])
const loading = ref(false)
const saving = ref(false)
const updatingId = ref('')
const editorVisible = ref(false)
const editingRecord = ref(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const editForm = reactive({
  mfaRequired: false,
  roleMfaRequired: []
})

const columns = [
  { title: '应用名称', dataIndex: 'clientName', key: 'clientName', width: 220 },
  { title: 'Client ID', dataIndex: 'clientId', key: 'clientId', ellipsis: true, width: 260 },
  { title: '应用 MFA', key: 'mfaRequired', width: 130 },
  { title: '角色 MFA', key: 'roleMfaRequired', width: 280 },
  { title: '处罚点', key: 'enforcement', width: 260 },
  { title: '更新时间', key: 'updatedAt', width: 190 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]

const roleSelectOptions = computed(() => roles.value.map(role => ({
  value: role.roleCode,
  label: `${role.roleName || role.roleCode} (${role.roleCode})`
})))

const roleNameMap = computed(() => Object.fromEntries(
  roles.value.map(role => [role.roleCode, role.roleName || role.roleCode])
))

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
    const res = await getLoginPolicies(pagination.current - 1, pagination.pageSize)
    policies.value = (res.data?.content || []).map(policy => ({
      ...policy,
      roleMfaRequired: policy.roleMfaRequired || []
    }))
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
    Object.assign(record, {
      ...(res.data || {}),
      mfaRequired: res.data?.mfaRequired ?? checked,
      roleMfaRequired: res.data?.roleMfaRequired || record.roleMfaRequired || []
    })
    message.success(checked ? '已启用应用 MFA' : '已关闭应用 MFA')
  } catch (e) {
    message.error(e.message)
  } finally {
    updatingId.value = ''
  }
}

const showPolicyEditor = (record) => {
  editingRecord.value = record
  editForm.mfaRequired = !!record.mfaRequired
  editForm.roleMfaRequired = [...(record.roleMfaRequired || [])]
  editorVisible.value = true
}

const savePolicy = async () => {
  if (!editingRecord.value) {
    return
  }
  saving.value = true
  try {
    const res = await updateLoginPolicy(editingRecord.value.id, {
      mfaRequired: editForm.mfaRequired,
      roleMfaRequired: editForm.roleMfaRequired
    })
    Object.assign(editingRecord.value, {
      ...(res.data || {}),
      roleMfaRequired: res.data?.roleMfaRequired || []
    })
    editorVisible.value = false
    message.success('登录策略已保存')
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
}

const formatRoleLabel = roleCode => roleNameMap.value[roleCode]
  ? `${roleNameMap.value[roleCode]} / ${roleCode}`
  : roleCode

const formatTime = value => value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'

onMounted(() => {
  fetchRoles()
  fetchData()
})
</script>

<style scoped>
.enforcement-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
  align-items: center;
}
</style>
