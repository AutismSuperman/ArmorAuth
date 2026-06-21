<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>租户管理</h2>
        <div class="page-subtitle">用户池、品牌配置和组织边界</div>
      </div>
      <a-button type="primary" @click="showCreate">
        <template #icon><PlusOutlined /></template>
        创建租户
      </a-button>
    </div>

    <div class="tenant-summary">
      <div class="summary-item">
        <div class="summary-label">用户池</div>
        <div class="summary-value">{{ pagination.total }}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">当前页启用</div>
        <div class="summary-value">{{ enabledTenantCount }}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">自定义域名</div>
        <div class="summary-value">{{ customDomainCount }}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">Path Issuer</div>
        <div class="summary-value">{{ issuerEnabledCount }}</div>
      </div>
    </div>

    <a-table
      row-key="id"
      size="middle"
      :columns="columns"
      :dataSource="tenants"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1240 }"
      @change="handleTableChange"
      bordered>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'tenantName'">
          <div class="tenant-title">
            <a-tag color="blue">用户池</a-tag>
            <span class="tenant-name">{{ record.tenantName }}</span>
          </div>
          <div class="tenant-code">{{ record.tenantCode }}</div>
        </template>
        <template v-if="column.key === 'primaryColor'">
          <span class="color-swatch" :style="{ backgroundColor: record.primaryColor || '#d1d5db' }"></span>
          <span>{{ record.primaryColor || '-' }}</span>
        </template>
        <template v-if="column.key === 'issuer'">
          <a-space size="small" wrap>
            <a-tag :color="record.pathIssuerEnabled ? 'green' : 'default'">
              {{ record.pathIssuerEnabled ? '启用' : '关闭' }}
            </a-tag>
            <a-tag v-if="record.issuerPath" color="geekblue">{{ record.issuerPath }}</a-tag>
          </a-space>
          <div v-if="record.customDomainIssuer" class="tenant-code">{{ record.customDomainIssuer }}</div>
        </template>
        <template v-if="column.key === 'enabled'">
          <a-switch :checked="record.enabled" size="small" @change="checked => handleStatus(record, checked)" />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="viewOrganizations(record)">组织</a>
            <a @click="showEdit(record)">编辑</a>
            <a-popconfirm title="确认删除此租户？" ok-text="删除" cancel-text="取消" @confirm="handleDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑租户' : '创建租户'"
      :confirmLoading="submitting"
      width="680px"
      @ok="handleSubmit">
      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="租户编码" required>
              <a-input v-model:value="form.tenantCode" :disabled="isEdit" placeholder="如 acme" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="租户名称" required>
              <a-input v-model:value="form.tenantName" placeholder="如 Acme" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="租户说明" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="Logo">
              <a-input v-model:value="form.logo" placeholder="https://example.com/logo.png" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="主题色">
              <div class="color-editor">
                <input v-model="form.primaryColor" class="color-input" type="color" />
                <a-input v-model:value="form.primaryColor" placeholder="#215ae5" />
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="isEdit" label="自定义域名">
          <a-input v-model:value="form.customDomain" placeholder="login.example.com" />
        </a-form-item>
        <a-form-item label="登录页标题">
          <a-input v-model:value="form.loginPageTitle" placeholder="Welcome" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="隐私政策 URL">
              <a-input v-model:value="form.privacyPolicyUrl" placeholder="https://example.com/privacy" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="服务条款 URL">
              <a-input v-model:value="form.termsOfServiceUrl" placeholder="https://example.com/terms" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { createTenant, deleteTenant, getTenants, updateTenant, updateTenantStatus } from '../../api'

const router = useRouter()
const tenants = ref([])
const loading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const defaultForm = () => ({
  tenantCode: '',
  tenantName: '',
  description: '',
  logo: '',
  primaryColor: '#215ae5',
  customDomain: '',
  loginPageTitle: '',
  privacyPolicyUrl: '',
  termsOfServiceUrl: ''
})

const form = reactive(defaultForm())

const enabledTenantCount = computed(() => tenants.value.filter(item => item.enabled).length)
const customDomainCount = computed(() => tenants.value.filter(item => item.customDomain).length)
const issuerEnabledCount = computed(() => tenants.value.filter(item => item.pathIssuerEnabled).length)

const columns = [
  { title: '租户', key: 'tenantName', width: 190 },
  { title: 'Issuer', key: 'issuer', width: 210 },
  { title: '自定义域名', dataIndex: 'customDomain', key: 'customDomain', width: 190, ellipsis: true },
  { title: '主题色', key: 'primaryColor', width: 130 },
  { title: '状态', key: 'enabled', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

const resetForm = () => Object.assign(form, defaultForm())

const normalizePayload = (payload) => Object.fromEntries(
  Object.entries(payload).map(([key, value]) => [key, value === '' ? null : value])
)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTenants(pagination.current - 1, pagination.pageSize)
    tenants.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (e) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const showCreate = () => {
  isEdit.value = false
  editId.value = null
  resetForm()
  modalVisible.value = true
}

const showEdit = (record) => {
  isEdit.value = true
  editId.value = record.id
  resetForm()
  Object.assign(form, {
    tenantCode: record.tenantCode || '',
    tenantName: record.tenantName || '',
    description: record.description || '',
    logo: record.logo || '',
    primaryColor: record.primaryColor || '#215ae5',
    customDomain: record.customDomain || '',
    loginPageTitle: record.loginPageTitle || '',
    privacyPolicyUrl: record.privacyPolicyUrl || '',
    termsOfServiceUrl: record.termsOfServiceUrl || ''
  })
  modalVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const payload = normalizePayload({ ...form })
    if (isEdit.value) {
      delete payload.tenantCode
      await updateTenant(editId.value, payload)
      message.success('更新成功')
    } else {
      delete payload.customDomain
      await createTenant(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    await fetchData()
  } catch (e) {
    message.error(e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const handleStatus = async (record, enabled) => {
  try {
    await updateTenantStatus(record.id, enabled)
    record.enabled = enabled
    message.success(enabled ? '已启用' : '已禁用')
  } catch (e) {
    message.error(e.message || '状态更新失败')
    await fetchData()
  }
}

const viewOrganizations = (record) => {
  router.push({ path: '/main/organizations', query: { tenantId: record.id } })
}

const handleDelete = async (id) => {
  try {
    await deleteTenant(id)
    message.success('删除成功')
    await fetchData()
  } catch (e) {
    message.error(e.message || '删除失败')
  }
}

onMounted(fetchData)
</script>

<style scoped>
.tenant-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  min-height: 76px;
  padding: 14px 16px;
  background: var(--aa-card-bg);
  border: 1px solid var(--aa-border-light);
  border-radius: var(--aa-radius);
  box-shadow: var(--aa-shadow);
}

.summary-label {
  color: var(--aa-text-secondary);
  font-size: 13px;
}

.summary-value {
  margin-top: 8px;
  color: var(--aa-text-primary);
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}

.page-subtitle,
.tenant-code {
  margin-top: 6px;
  color: var(--aa-text-secondary);
  font-size: 13px;
}

.tenant-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.tenant-name {
  color: var(--aa-text-primary);
  font-weight: 600;
}

.color-swatch {
  width: 18px;
  height: 18px;
  display: inline-block;
  margin-right: 8px;
  border: 1px solid var(--aa-border);
  border-radius: 50%;
  vertical-align: -4px;
}

.color-editor {
  display: flex;
  gap: 8px;
}

.color-input {
  width: 42px;
  height: 32px;
  padding: 0;
  border: 1px solid var(--aa-border);
  border-radius: var(--aa-radius-sm);
  background: #fff;
}

@media (max-width: 720px) {
  .tenant-summary {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
  }
}
</style>
