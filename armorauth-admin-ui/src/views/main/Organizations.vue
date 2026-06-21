<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>组织管理</h2>
        <div class="page-subtitle">按租户查看组织、成员和组织角色</div>
      </div>
      <a-button type="primary" @click="showCreate">
        <template #icon><PlusOutlined /></template>
        创建组织
      </a-button>
    </div>

    <div class="tenant-toolbar">
      <div class="tenant-picker">
        <span class="tenant-picker-label">所属租户</span>
        <a-select
          v-model:value="selectedTenantId"
          :loading="tenantLoading"
          show-search
          option-filter-prop="label"
          placeholder="选择租户"
          class="tenant-select"
          @change="handleTenantChange">
          <a-select-option
            v-for="tenant in tenants"
            :key="tenant.id"
            :value="tenant.id"
            :label="`${tenant.tenantName} ${tenant.tenantCode}`">
            <div class="tenant-option">
              <span>{{ tenant.tenantName }}</span>
              <span>{{ tenant.tenantCode }}</span>
            </div>
          </a-select-option>
        </a-select>
      </div>
      <div v-if="selectedTenant" class="tenant-context">
        <a-tag color="blue">用户池</a-tag>
        <span class="tenant-context-name">{{ selectedTenant.tenantName }}</span>
        <span class="tenant-context-meta">{{ selectedTenant.customDomain || '未配置自定义域名' }}</span>
      </div>
    </div>

    <a-table
      :dataSource="organizations"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1160 }"
      @change="handleTableChange"
      row-key="id"
      size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'orgName'">
          <div class="org-name">{{ record.orgName }}</div>
          <div class="org-code">{{ record.orgCode }}</div>
        </template>
        <template v-if="column.key === 'tenant'">
          <div class="tenant-name">{{ tenantName(record.tenantId) }}</div>
          <div class="tenant-code">{{ tenantCode(record.tenantId) }}</div>
        </template>
        <template v-if="column.key === 'parent'">
          <span>{{ parentName(record.parentId) }}</span>
        </template>
        <template v-if="column.key === 'enabled'">
          <a-tag :color="record.enabled ? 'green' : 'red'">{{ record.enabled ? '启用' : '禁用' }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="showMembers(record)">成员</a>
            <a @click="showEdit(record)">编辑</a>
            <a-popconfirm title="确认删除此组织？" @confirm="handleDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" :title="isEdit ? '编辑组织' : '创建组织'"
             @ok="handleSubmit" :confirmLoading="submitting" width="640px">
      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="所属租户" required>
              <a-select
                v-model:value="form.tenantId"
                :disabled="isEdit"
                show-search
                option-filter-prop="label"
                placeholder="选择租户">
                <a-select-option
                  v-for="tenant in tenants"
                  :key="tenant.id"
                  :value="tenant.id"
                  :label="`${tenant.tenantName} ${tenant.tenantCode}`">
                  {{ tenant.tenantName }} / {{ tenant.tenantCode }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="上级组织">
              <a-select
                v-model:value="form.parentId"
                allow-clear
                placeholder="根组织"
                :options="parentOptions" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="组织编码" required>
              <a-input v-model:value="form.orgCode" :disabled="isEdit" placeholder="唯一编码，如 engineering" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="组织名称" required>
              <a-input v-model:value="form.orgName" placeholder="如 工程部" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="组织说明" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer :title="memberDrawerTitle" :open="memberDrawerVisible" @close="memberDrawerVisible = false"
              width="640">
      <a-table :dataSource="members" :columns="memberColumns" :loading="memberLoading"
               row-key="id" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'role'">
            <a-tag>{{ record.orgRole || 'MEMBER' }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-drawer>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
  getOrganizations,
  createOrganization,
  updateOrganization,
  deleteOrganization,
  getOrgMembers,
  getTenants
} from '../../api'

const route = useRoute()
const organizations = ref([])
const tenants = ref([])
const loading = ref(false)
const tenantLoading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedTenantId = ref('')

const members = ref([])
const memberLoading = ref(false)
const memberDrawerVisible = ref(false)
const memberDrawerTitle = ref('')

const defaultForm = () => ({ orgCode: '', orgName: '', tenantId: '', parentId: null, description: '' })
const form = reactive(defaultForm())

const selectedTenant = computed(() => tenants.value.find(item => item.id === selectedTenantId.value))

const parentOptions = computed(() => organizations.value
  .filter(item => item.id !== editId.value)
  .map(item => ({ label: `${item.orgName} / ${item.orgCode}`, value: item.id })))

const columns = [
  { title: '组织', key: 'orgName', width: 220 },
  { title: '所属租户', key: 'tenant', width: 220 },
  { title: '上级组织', key: 'parent', width: 180 },
  { title: '状态', key: 'enabled', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

const memberColumns = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '角色', key: 'role', width: 120 },
  { title: '加入时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 }
]

const resetForm = () => Object.assign(form, defaultForm())

const normalizePayload = (payload) => Object.fromEntries(
  Object.entries(payload).map(([key, value]) => [key, value === '' ? null : value])
)

const tenantById = (tenantId) => tenants.value.find(item => item.id === tenantId)
const tenantName = (tenantId) => tenantById(tenantId)?.tenantName || tenantId || '默认租户'
const tenantCode = (tenantId) => tenantById(tenantId)?.tenantCode || tenantId || 'default'
const parentName = (parentId) => {
  if (!parentId) return '根组织'
  const parent = organizations.value.find(item => item.id === parentId)
  return parent ? `${parent.orgName} / ${parent.orgCode}` : parentId
}

const fetchTenants = async () => {
  tenantLoading.value = true
  try {
    const res = await getTenants(0, 100)
    tenants.value = res.data?.content || []
  } catch (e) {
    message.error(e.message || '租户加载失败')
  } finally {
    tenantLoading.value = false
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getOrganizations(pagination.current - 1, pagination.pageSize, selectedTenantId.value)
    organizations.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (e) { message.error(e.message || '加载失败') }
  finally { loading.value = false }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const handleTenantChange = () => {
  pagination.current = 1
  fetchData()
}

const showCreate = () => {
  isEdit.value = false
  editId.value = null
  resetForm()
  form.tenantId = selectedTenantId.value || ''
  modalVisible.value = true
}

const showEdit = (record) => {
  isEdit.value = true
  editId.value = record.id
  resetForm()
  form.orgCode = record.orgCode
  form.orgName = record.orgName
  form.tenantId = record.tenantId || ''
  form.parentId = record.parentId || null
  form.description = record.description || ''
  modalVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const payload = normalizePayload({ ...form })
    if (isEdit.value) {
      await updateOrganization(editId.value, {
        orgName: payload.orgName,
        description: payload.description,
        parentId: payload.parentId
      })
      message.success('更新成功')
    } else {
      await createOrganization(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    await fetchData()
  } catch (e) { message.error(e.message) }
  finally { submitting.value = false }
}

const handleDelete = async (id) => {
  try { await deleteOrganization(id); message.success('删除成功'); fetchData() }
  catch (e) { message.error(e.message) }
}

const showMembers = async (record) => {
  memberDrawerTitle.value = record.orgName + ' - 成员列表'
  memberDrawerVisible.value = true; memberLoading.value = true
  try {
    const res = await getOrgMembers(record.id)
    members.value = res.data?.content || res.data || []
  } catch (e) { message.error('加载成员失败') }
  finally { memberLoading.value = false }
}

watch(
  () => route.query.tenantId,
  (tenantId) => {
    if (typeof tenantId === 'string' && tenantId && tenantId !== selectedTenantId.value) {
      selectedTenantId.value = tenantId
      pagination.current = 1
      fetchData()
    }
  }
)

onMounted(async () => {
  await fetchTenants()
  const queryTenantId = typeof route.query.tenantId === 'string' ? route.query.tenantId : ''
  selectedTenantId.value = queryTenantId || tenants.value[0]?.id || ''
  await fetchData()
})
</script>

<style scoped>
.tenant-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  background: var(--aa-card-bg);
  border: 1px solid var(--aa-border-light);
  border-radius: var(--aa-radius);
  box-shadow: var(--aa-shadow);
}

.tenant-picker {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 320px;
}

.tenant-picker-label {
  color: var(--aa-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.tenant-select {
  width: 320px;
}

.tenant-option {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.tenant-option span:last-child {
  color: var(--aa-text-muted);
  font-family: var(--aa-font-mono);
  font-size: 12px;
}

.tenant-context {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.tenant-context-name {
  color: var(--aa-text-primary);
  font-weight: 600;
}

.tenant-context-meta,
.org-code,
.tenant-code {
  color: var(--aa-text-secondary);
  font-size: 12px;
}

.org-name,
.tenant-name {
  color: var(--aa-text-primary);
  font-weight: 600;
}

.org-code,
.tenant-code {
  margin-top: 4px;
  font-family: var(--aa-font-mono);
}

@media (max-width: 720px) {
  .page-header,
  .tenant-toolbar,
  .tenant-picker {
    align-items: stretch;
    flex-direction: column;
  }

  .tenant-picker,
  .tenant-select {
    width: 100%;
    min-width: 0;
  }
}
</style>
