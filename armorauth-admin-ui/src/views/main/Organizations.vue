<template>
  <div class="page-container">
    <div class="page-header">
      <h2>组织管理</h2>
      <a-button type="primary" @click="showCreate">
        <template #icon><PlusOutlined /></template>
        创建组织
      </a-button>
    </div>

    <a-table :dataSource="organizations" :columns="columns" :loading="loading"
             :pagination="pagination" @change="handleTableChange" row-key="id" size="middle">
      <template #bodyCell="{ column, record }">
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
             @ok="handleSubmit" :confirmLoading="submitting" width="520px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="组织编码" required>
          <a-input v-model:value="form.orgCode" placeholder="唯一编码，如 engineering" />
        </a-form-item>
        <a-form-item label="组织名称" required>
          <a-input v-model:value="form.orgName" placeholder="如 工程部" />
        </a-form-item>
        <a-form-item label="租户ID">
          <a-input v-model:value="form.tenantId" placeholder="租户ID（可选）" />
        </a-form-item>
        <a-form-item label="父组织ID">
          <a-input v-model:value="form.parentId" placeholder="父组织ID（可选）" />
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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getOrganizations, createOrganization, updateOrganization, deleteOrganization, getOrgMembers } from '../../api'

const organizations = ref([])
const loading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const members = ref([])
const memberLoading = ref(false)
const memberDrawerVisible = ref(false)
const memberDrawerTitle = ref('')

const form = reactive({ orgCode: '', orgName: '', tenantId: '', parentId: '' })

const columns = [
  { title: '组织编码', dataIndex: 'orgCode', key: 'orgCode', width: 150 },
  { title: '组织名称', dataIndex: 'orgName', key: 'orgName', width: 200 },
  { title: '租户ID', dataIndex: 'tenantId', key: 'tenantId', width: 200, ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

const memberColumns = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '角色', key: 'role', width: 120 },
  { title: '加入时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getOrganizations(pagination.current - 1, pagination.pageSize)
    organizations.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (e) { message.error('加载失败') }
  finally { loading.value = false }
}

const handleTableChange = (pag) => { pagination.current = pag.current; fetchData() }

const showCreate = () => {
  isEdit.value = false; editId.value = null
  form.orgCode = ''; form.orgName = ''; form.tenantId = ''; form.parentId = ''
  modalVisible.value = true
}

const showEdit = (record) => {
  isEdit.value = true; editId.value = record.id
  form.orgCode = record.orgCode; form.orgName = record.orgName
  form.tenantId = record.tenantId || ''; form.parentId = record.parentId || ''
  modalVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateOrganization(editId.value, { orgName: form.orgName })
      message.success('更新成功')
    } else {
      await createOrganization(form)
      message.success('创建成功')
    }
    modalVisible.value = false; fetchData()
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

onMounted(fetchData)
</script>

<style scoped>
</style>
