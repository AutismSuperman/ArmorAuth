<template>
  <div class="page-container">
    <div class="page-header">
      <h2>Scope 管理</h2>
      <a-button type="primary" @click="showCreate">
        <template #icon><PlusOutlined /></template>
        新增 Scope
      </a-button>
    </div>

    <div class="toolbar">
      <a-select
        v-model:value="filters.clientId"
        allow-clear
        show-search
        option-filter-prop="label"
        placeholder="按应用过滤"
        class="app-filter"
        @change="handleFilterChange">
        <a-select-option
          v-for="app in applicationOptions"
          :key="app.clientId"
          :value="app.clientId"
          :label="app.searchLabel">
          {{ app.clientName }} / {{ app.clientId }}
        </a-select-option>
      </a-select>
      <a-button @click="fetchData">
        <template #icon><SearchOutlined /></template>
        查询
      </a-button>
      <a-button @click="resetFilters">重置</a-button>
    </div>

    <a-spin :spinning="loading">
      <a-empty v-if="groupedScopes.length === 0" description="暂无 Scope" />
      <a-collapse v-else v-model:activeKey="activeScopePanels" class="scope-collapse">
        <a-collapse-panel v-for="group in groupedScopes" :key="group.clientId">
          <template #header>
            <div class="scope-panel-header">
              <span class="app-name">{{ group.clientName }}</span>
              <span class="client-id">{{ group.clientId }}</span>
              <a-tag color="blue">{{ group.rows.length }} scopes</a-tag>
            </div>
          </template>
          <a-table
            :dataSource="group.rows"
            :columns="columns"
            row-key="rowKey"
            size="middle"
            :scroll="{ x: 780 }"
            :pagination="false">
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
        </a-collapse-panel>
      </a-collapse>
    </a-spin>

    <div class="pagination-row" v-if="pagination.total > 0">
      <a-pagination
        :current="pagination.current"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        show-size-changer
        @change="handlePageChange"
        @showSizeChange="handlePageChange" />
    </div>

    <a-modal v-model:open="modalVisible" :title="isEdit ? '编辑 Scope' : '新增 Scope'"
             @ok="handleSubmit" :confirmLoading="submitting" width="560px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="应用" required>
          <a-select
            v-model:value="form.clientId"
            :disabled="isEdit"
            show-search
            option-filter-prop="label"
            placeholder="选择已创建的应用">
            <a-select-option
              v-for="app in applicationOptions"
              :key="app.clientId"
              :value="app.clientId"
              :label="app.searchLabel">
              {{ app.clientName }} / {{ app.clientId }}
            </a-select-option>
          </a-select>
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
import { computed, reactive, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { createScope, deleteScope, getApplications, getScopes, updateScope } from '../../api'

const scopes = ref([])
const applications = ref([])
const activeScopePanels = ref([])
const loading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const filters = reactive({ clientId: undefined })
const form = reactive({ clientId: undefined, scope: '', description: '' })

const columns = [
  { title: 'Scope', dataIndex: 'scope', key: 'scope', width: 220 },
  { title: '描述', dataIndex: 'description', key: 'description', width: 360 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' }
]

const normalizeRows = rows => rows.map(row => ({
  ...row,
  rowKey: `${row.clientId}:${row.scope}`
}))

const applicationOptions = computed(() => applications.value.map(app => ({
  ...app,
  searchLabel: `${app.clientName || ''} ${app.clientId || ''}`
})))

const applicationMap = computed(() =>
  Object.fromEntries(applications.value.map(app => [app.clientId, app.clientName || app.clientId]))
)

const groupedScopes = computed(() => {
  const map = new Map()
  scopes.value.forEach(scope => {
    if (!map.has(scope.clientId)) {
      map.set(scope.clientId, {
        clientId: scope.clientId,
        clientName: applicationMap.value[scope.clientId] || scope.clientId,
        rows: []
      })
    }
    map.get(scope.clientId).rows.push(scope)
  })
  return Array.from(map.values())
})

const syncActivePanels = () => {
  const groupIds = groupedScopes.value.map(group => group.clientId)
  activeScopePanels.value = activeScopePanels.value.filter(key => groupIds.includes(key))
  if (activeScopePanels.value.length === 0 && groupIds.length > 0) {
    activeScopePanels.value = [groupIds[0]]
  }
}

const fetchApplications = async () => {
  try {
    const res = await getApplications(0, 500)
    applications.value = res.data?.content || []
  } catch (e) {
    message.error('应用列表加载失败: ' + e.message)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getScopes(pagination.current - 1, pagination.pageSize, filters.clientId)
    scopes.value = normalizeRows(res.data?.content || [])
    pagination.total = res.data?.totalElements || 0
    syncActivePanels()
  } catch (e) {
    message.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page, pageSize) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = () => {
  pagination.current = 1
  fetchData()
}

const resetForm = () => {
  form.clientId = filters.clientId || undefined
  form.scope = ''
  form.description = ''
}

const resetFilters = () => {
  filters.clientId = undefined
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
    message.warning('应用和 Scope 不能为空')
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

onMounted(async () => {
  await fetchApplications()
  await fetchData()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
}

.app-filter {
  min-width: 320px;
}

.scope-collapse {
  background: transparent;
}

.scope-panel-header {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.app-name {
  font-weight: 600;
  color: var(--aa-text-primary);
}

.client-id {
  min-width: 0;
  overflow: hidden;
  color: var(--aa-text-secondary);
  font-family: var(--aa-font-mono);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .app-filter {
    min-width: 0;
    width: 100%;
  }
}
</style>
