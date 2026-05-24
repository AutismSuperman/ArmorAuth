<template>
  <div class="page-container">
    <div class="page-header">
      <h2>应用管理</h2>
      <a-button type="primary" @click="showCreate">
        <template #icon><PlusOutlined /></template>
        创建应用
      </a-button>
    </div>

    <a-table :dataSource="applications" :columns="columns" :loading="loading"
             :pagination="pagination" @change="handleTableChange" row-key="id" size="middle"
             :scroll="{ x: 1180 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'auth'">
          <a-space size="small" wrap>
            <a-tag v-for="method in recordAuthMethods(record)" :key="method" :color="authMethodColor(method)">
              {{ authMethodLabel(method) }}
            </a-tag>
          </a-space>
        </template>
        <template v-if="column.key === 'grant'">
          <a-space size="small" wrap>
            <a-tag v-for="grant in parseList(record.authorizationGrantTypes)" :key="grant">
              {{ grantLabel(grant) }}
            </a-tag>
          </a-space>
        </template>
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
            <a v-if="hasSecretMethod(record)" @click="handleRotateSecret(record)">重置密钥</a>
            <a-popconfirm title="确认删除此应用？" @confirm="handleDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" :title="isEdit ? '编辑应用' : '创建应用'"
             @ok="handleSubmit" :confirmLoading="submitting" width="760px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="应用名称" required>
          <a-input v-model:value="form.clientName" placeholder="请输入应用名称" />
        </a-form-item>

        <a-form-item label="客户端认证方式" required>
          <a-select
            v-model:value="form.clientAuthenticationMethods"
            mode="multiple"
            placeholder="选择客户端认证方式"
            :max-tag-count="3"
            @change="handleAuthMethodsChange">
            <a-select-option v-for="method in authMethodOptions" :key="method.value" :value="method.value">
              {{ method.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <div class="setting-grid">
          <a-form-item v-if="usesPrivateKeyJwt" label="JWK Set URL">
            <a-input v-model:value="form.clientSettings.jwkSetUrl"
                     placeholder="https://client.example.com/.well-known/jwks.json" />
          </a-form-item>
          <a-form-item v-if="usesJwtAuth" label="Token Endpoint 签名算法">
            <a-select v-model:value="form.clientSettings.signingAlgorithm" placeholder="选择客户端认证 JWT 签名算法">
              <a-select-option v-for="algorithm in signingAlgorithmOptions" :key="algorithm.value" :value="algorithm.value">
                {{ algorithm.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>

        <a-form-item label="授权类型" required>
          <a-select v-model:value="form.authorizationGrantTypes" mode="multiple" placeholder="选择授权类型">
            <a-select-option v-for="grant in grantTypeOptions" :key="grant.value" :value="grant.value">
              {{ grant.label }}
            </a-select-option>
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
          <a-select v-model:value="form.scopes" mode="tags" placeholder="输入 scope 后回车" />
        </a-form-item>

        <a-row :gutter="12">
          <a-col :xs="24" :md="12">
            <a-form-item label="ID Token 签名算法">
              <a-select v-model:value="form.tokenSettings.idTokenSignatureAlgorithm" placeholder="选择 ID Token 签名算法">
                <a-select-option v-for="algorithm in idTokenAlgorithmOptions" :key="algorithm.value" :value="algorithm.value">
                  {{ algorithm.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="Access Token 格式">
              <a-select v-model:value="form.tokenSettings.tokenFormat" placeholder="选择 Token 格式">
                <a-select-option value="self-contained">Self-contained JWT</a-select-option>
                <a-select-option value="reference">Reference Token</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <div class="switch-row">
          <a-checkbox v-model:checked="form.clientSettings.requireProofKey">要求 PKCE</a-checkbox>
          <a-checkbox v-model:checked="form.clientSettings.requireAuthorizationConsent">要求授权确认</a-checkbox>
          <a-checkbox v-model:checked="form.mfaRequired">强制 MFA</a-checkbox>
        </div>
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
import { computed, ref, reactive, onMounted } from 'vue'
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

const authMethodOptions = [
  { value: 'client_secret_basic', label: 'Client Secret Basic', color: 'blue' },
  { value: 'client_secret_post', label: 'Client Secret Post', color: 'cyan' },
  { value: 'client_secret_jwt', label: 'Client Secret JWT', color: 'geekblue' },
  { value: 'private_key_jwt', label: 'Private Key JWT', color: 'purple' },
  { value: 'tls_client_auth', label: 'TLS Client Auth', color: 'green' },
  { value: 'self_signed_tls_client_auth', label: 'Self-signed TLS Client Auth', color: 'lime' },
  { value: 'none', label: 'None / Public Client', color: 'default' }
]

const grantTypeOptions = [
  { value: 'authorization_code', label: 'Authorization Code' },
  { value: 'client_credentials', label: 'Client Credentials' },
  { value: 'refresh_token', label: 'Refresh Token' },
  { value: 'urn:ietf:params:oauth:grant-type:device_code', label: 'Device Code' },
  { value: 'urn:ietf:params:oauth:grant-type:jwt-bearer', label: 'JWT Bearer' },
  { value: 'urn:ietf:params:oauth:grant-type:token-exchange', label: 'Token Exchange' }
]

const secretJwtAlgorithms = ['HS256', 'HS384', 'HS512']
const asymmetricJwtAlgorithms = ['RS256', 'RS384', 'RS512', 'PS256', 'PS384', 'PS512', 'ES256', 'ES384', 'ES512']
const idTokenAlgorithmOptions = asymmetricJwtAlgorithms.map(value => ({ value, label: value }))
const authMethodMap = Object.fromEntries(authMethodOptions.map(item => [item.value, item]))
const grantTypeMap = Object.fromEntries(grantTypeOptions.map(item => [item.value, item]))

const form = reactive({
  clientName: '',
  clientAuthenticationMethods: ['client_secret_basic'],
  authorizationGrantTypes: ['authorization_code', 'refresh_token'],
  redirectUris: '',
  postLogoutRedirectUris: '',
  scopes: ['openid', 'profile', 'email'],
  clientSettings: {
    jwkSetUrl: '',
    requireAuthorizationConsent: false,
    requireProofKey: false,
    signingAlgorithm: ''
  },
  tokenSettings: {
    idTokenSignatureAlgorithm: 'RS256',
    tokenFormat: 'self-contained'
  },
  mfaRequired: false
})

const columns = [
  { title: '应用名称', dataIndex: 'clientName', key: 'name', width: 180 },
  { title: 'Client ID', dataIndex: 'clientId', key: 'clientId', ellipsis: true, width: 240 },
  { title: '认证方式', dataIndex: 'clientAuthenticationMethods', key: 'auth', width: 260 },
  { title: '授权类型', dataIndex: 'authorizationGrantTypes', key: 'grant', width: 260 },
  { title: '状态', key: 'status', width: 100 },
  { title: 'MFA', key: 'mfaRequired', width: 80 },
  { title: '操作', key: 'action', width: 210, fixed: 'right' }
]

const usesSecretJwt = computed(() => form.clientAuthenticationMethods.includes('client_secret_jwt'))
const usesPrivateKeyJwt = computed(() => form.clientAuthenticationMethods.includes('private_key_jwt'))
const usesJwtAuth = computed(() => usesSecretJwt.value || usesPrivateKeyJwt.value)
const signingAlgorithmOptions = computed(() => {
  const algorithms = new Set()
  if (usesSecretJwt.value) {
    secretJwtAlgorithms.forEach(item => algorithms.add(item))
  }
  if (usesPrivateKeyJwt.value) {
    asymmetricJwtAlgorithms.forEach(item => algorithms.add(item))
  }
  return Array.from(algorithms).map(value => ({ value, label: value }))
})

const parseList = (value) => {
  if (Array.isArray(value)) {
    return value.filter(Boolean)
  }
  return String(value || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

const normalizeTextList = (value) => String(value || '')
  .split('\n')
  .map(item => item.trim())
  .filter(Boolean)
  .join(',')

const nullableText = (value) => {
  const text = String(value || '').trim()
  return text || null
}

const recordAuthMethods = (record) => parseList(record.clientAuthenticationMethods)

const authMethodLabel = (method) => authMethodMap[method]?.label || method
const authMethodColor = (method) => authMethodMap[method]?.color || 'default'
const grantLabel = (grant) => grantTypeMap[grant]?.label || grant
const hasSecretMethod = (record) => recordAuthMethods(record).some(method => method.startsWith('client_secret'))

const ensureSigningAlgorithm = () => {
  if (!usesJwtAuth.value) {
    form.clientSettings.signingAlgorithm = ''
    return
  }
  const allowed = signingAlgorithmOptions.value.map(item => item.value)
  if (!allowed.includes(form.clientSettings.signingAlgorithm)) {
    form.clientSettings.signingAlgorithm = allowed[0] || ''
  }
}

const handleAuthMethodsChange = (methods) => {
  const selected = Array.isArray(methods) ? methods : []
  if (selected.includes('none') && selected.length > 1) {
    form.clientAuthenticationMethods = selected[selected.length - 1] === 'none'
      ? ['none']
      : selected.filter(method => method !== 'none')
    message.info('Public Client 不能与其他认证方式混用')
  }
  ensureSigningAlgorithm()
}

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
  form.clientAuthenticationMethods = ['client_secret_basic']
  form.authorizationGrantTypes = ['authorization_code', 'refresh_token']
  form.redirectUris = ''
  form.postLogoutRedirectUris = ''
  form.scopes = ['openid', 'profile', 'email']
  form.clientSettings.jwkSetUrl = ''
  form.clientSettings.requireAuthorizationConsent = false
  form.clientSettings.requireProofKey = false
  form.clientSettings.signingAlgorithm = ''
  form.tokenSettings.idTokenSignatureAlgorithm = 'RS256'
  form.tokenSettings.tokenFormat = 'self-contained'
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
  form.clientAuthenticationMethods = parseList(record.clientAuthenticationMethods)
  form.authorizationGrantTypes = parseList(record.authorizationGrantTypes)
  form.redirectUris = parseList(record.redirectUris).join('\n')
  form.postLogoutRedirectUris = parseList(record.postLogoutRedirectUris).join('\n')
  form.scopes = parseList(record.scopes)
  form.clientSettings.jwkSetUrl = record.clientSettings?.jwkSetUrl || ''
  form.clientSettings.requireAuthorizationConsent = !!record.clientSettings?.requireAuthorizationConsent
  form.clientSettings.requireProofKey = !!record.clientSettings?.requireProofKey
  form.clientSettings.signingAlgorithm = record.clientSettings?.signingAlgorithm || ''
  form.tokenSettings.idTokenSignatureAlgorithm = record.tokenSettings?.idTokenSignatureAlgorithm || 'RS256'
  form.tokenSettings.tokenFormat = record.tokenSettings?.tokenFormat || 'self-contained'
  form.mfaRequired = record.mfaRequired || false
  ensureSigningAlgorithm()
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!form.clientName || form.clientAuthenticationMethods.length === 0 || form.authorizationGrantTypes.length === 0) {
    message.warning('应用名称、认证方式和授权类型不能为空')
    return
  }
  submitting.value = true
  try {
    const data = {
      clientName: form.clientName,
      clientAuthenticationMethods: form.clientAuthenticationMethods.join(','),
      authorizationGrantTypes: form.authorizationGrantTypes.join(','),
      redirectUris: normalizeTextList(form.redirectUris),
      postLogoutRedirectUris: normalizeTextList(form.postLogoutRedirectUris),
      scopes: form.scopes,
      clientSettings: {
        jwkSetUrl: nullableText(form.clientSettings.jwkSetUrl),
        requireAuthorizationConsent: form.clientSettings.requireAuthorizationConsent,
        requireProofKey: form.clientSettings.requireProofKey,
        signingAlgorithm: nullableText(form.clientSettings.signingAlgorithm)
      },
      tokenSettings: {
        idTokenSignatureAlgorithm: nullableText(form.tokenSettings.idTokenSignatureAlgorithm),
        tokenFormat: nullableText(form.tokenSettings.tokenFormat)
      },
      mfaRequired: form.mfaRequired
    }
    if (isEdit.value) {
      await updateApplication(editId.value, data)
      message.success('更新成功')
    } else {
      const res = await createApplication(data)
      if (res.data?.clientSecret && hasSecretMethod(data)) {
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
.setting-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 280px);
  column-gap: 12px;
}

.switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  padding: 2px 0 8px;
}

@media (max-width: 720px) {
  .setting-grid {
    grid-template-columns: 1fr;
  }
}
</style>
