<template>
  <div class="page-container">
    <div class="page-header">
      <h2>应用管理</h2>
      <a-space wrap>
        <a-select
          v-model:value="selectedTenantId"
          allow-clear
          placeholder="全部租户"
          style="width: 220px"
          @change="handleTenantFilterChange">
          <a-select-option v-for="tenant in tenantOptions" :key="tenant.id" :value="tenant.id">
            {{ tenant.tenantName }} / {{ tenant.tenantCode }}
          </a-select-option>
        </a-select>
        <a-button type="primary" @click="showCreate">
          <template #icon><PlusOutlined /></template>
          创建应用
        </a-button>
      </a-space>
    </div>

    <a-table :dataSource="applications" :columns="columns" :loading="loading"
             :pagination="pagination" @change="handleTableChange" row-key="id" size="middle"
             :scroll="{ x: 2100 }">
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
        <template v-if="column.key === 'tenant'">
          <a-space size="small" wrap>
            <a-tag color="blue">{{ record.tenantCode || record.tenantId || 'default' }}</a-tag>
            <a-tag v-if="record.issuerPath" color="geekblue">{{ record.issuerPath }}</a-tag>
          </a-space>
        </template>
        <template v-if="column.key === 'source'">
          <a-space size="small" wrap>
            <a-tag :color="record.registrationSource === 'DYNAMIC_REGISTRATION' ? 'purple' : 'default'">
              {{ registrationSourceLabel(record.registrationSource) }}
            </a-tag>
            <a-tag v-if="record.dynamicClientRegistrar" color="cyan">DCR Registrar</a-tag>
          </a-space>
        </template>
        <template v-if="column.key === 'dpop'">
          <a-space size="small" wrap>
            <a-tag :color="record.clientSettings?.dpopEnabled ? 'green' : 'default'">
              {{ record.clientSettings?.dpopEnabled ? '启用' : '关闭' }}
            </a-tag>
            <a-tag v-if="record.clientSettings?.dpopRequired" color="orange">Required</a-tag>
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
            <a @click="showEndpointDetails(record)"><ApiOutlined /> 端点详情</a>
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
        <a-form-item label="归属租户" required>
          <a-select
            v-model:value="form.tenantId"
            :disabled="isEdit"
            placeholder="选择租户">
            <a-select-option value="tenant-default">Default / default</a-select-option>
            <a-select-option v-for="tenant in tenantOptions" :key="tenant.id" :value="tenant.id">
              {{ tenant.tenantName }} / {{ tenant.tenantCode }}
            </a-select-option>
          </a-select>
        </a-form-item>

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
          <a-form-item v-if="usesJwkSetUrl" label="JWK Set URL">
            <a-input v-model:value="form.clientSettings.jwkSetUrl"
                     placeholder="https://client.example.com/.well-known/jwks.json" />
          </a-form-item>
          <a-form-item v-if="usesTlsClientAuth" label="X.509 Subject DN">
            <a-input v-model:value="form.clientSettings.x509CertificateSubjectDN"
                     placeholder="CN=client.example.com,O=Example" />
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

        <a-form-item label="DCR">
          <a-checkbox v-model:checked="form.dynamicClientRegistrar" @change="handleDcrRegistrarChange">
            作为动态客户端注册管理客户端
          </a-checkbox>
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
          <a-checkbox v-model:checked="form.clientSettings.dpopEnabled" @change="handleDpopEnabledChange">
            启用 DPoP
          </a-checkbox>
          <a-checkbox v-if="form.clientSettings.dpopEnabled" v-model:checked="form.clientSettings.dpopRequired">
            要求 DPoP Proof
          </a-checkbox>
          <a-checkbox v-if="usesMtlsAuth" v-model:checked="form.tokenSettings.x509CertificateBoundAccessTokens">
            证书绑定 Access Token
          </a-checkbox>
          <a-checkbox v-model:checked="form.mfaRequired">强制 MFA</a-checkbox>
        </div>
        <a-form-item v-if="form.clientSettings.dpopEnabled" label="DPoP 允许算法">
          <a-select v-model:value="form.clientSettings.dpopAllowedAlgorithms" mode="multiple" placeholder="默认允许服务器支持的算法">
            <a-select-option v-for="algorithm in dpopAlgorithmOptions" :key="algorithm" :value="algorithm">
              {{ algorithm }}
            </a-select-option>
          </a-select>
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

    <a-modal v-model:open="endpointVisible" title="SAS 端点详情" :footer="null" width="980px">
      <div v-if="endpointRecord" class="endpoint-detail">
        <div class="endpoint-summary">
          <div>
            <div class="endpoint-kicker">Issuer</div>
            <a-typography-paragraph class="endpoint-issuer" copyable>
              {{ endpointIssuer(endpointRecord) }}
            </a-typography-paragraph>
          </div>
          <a-space size="small" wrap>
            <a-tag color="blue">{{ endpointRecord.tenantCode || endpointRecord.tenantId || 'default' }}</a-tag>
            <a-tag :color="endpointRecord.registrationSource === 'DYNAMIC_REGISTRATION' ? 'purple' : 'default'">
              {{ registrationSourceLabel(endpointRecord.registrationSource) }}
            </a-tag>
            <a-tag v-if="endpointRecord.dynamicClientRegistrar" color="cyan">DCR Registrar</a-tag>
            <a-tag v-if="endpointRecord.clientSettings?.dpopEnabled" color="green">DPoP</a-tag>
            <a-tag v-if="endpointRecord.tokenSettings?.x509CertificateBoundAccessTokens" color="lime">x5t#S256</a-tag>
            <a-tag v-if="endpointRecord.issuerPath" :color="endpointTenantIssuerEnabled(endpointRecord) ? 'geekblue' : 'red'">
              {{ endpointTenantIssuerEnabled(endpointRecord) ? '租户 Issuer 可用' : '租户 Issuer 未启用' }}
            </a-tag>
          </a-space>
        </div>

        <a-alert
          v-if="!endpointTenantIssuerEnabled(endpointRecord)"
          type="warning"
          show-icon
          class="endpoint-notes"
          message="当前应用归属租户，但租户 path issuer 未启用或租户已禁用。SAS 会拒绝该 /t/{tenantCode} 协议端点，请先在租户管理确认租户启用，并开启 multiple-issuers。"
        />

        <a-descriptions :column="{ xs: 1, sm: 1, md: 2 }" bordered size="small" class="endpoint-config">
          <a-descriptions-item label="Client ID">
            <a-typography-paragraph copyable>{{ endpointRecord.clientId }}</a-typography-paragraph>
          </a-descriptions-item>
          <a-descriptions-item label="Client Name">{{ endpointRecord.clientName }}</a-descriptions-item>
          <a-descriptions-item label="认证方式">
            <a-space size="small" wrap>
              <a-tag v-for="method in recordAuthMethods(endpointRecord)" :key="method" :color="authMethodColor(method)">
                {{ authMethodLabel(method) }}
              </a-tag>
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item label="授权类型">
            <a-space size="small" wrap>
              <a-tag v-for="grant in parseList(endpointRecord.authorizationGrantTypes)" :key="grant">
                {{ grantLabel(grant) }}
              </a-tag>
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item label="Scopes" :span="2">
            <a-space size="small" wrap>
              <a-tag v-for="scope in parseList(endpointRecord.scopes)" :key="scope">{{ scope }}</a-tag>
              <span v-if="parseList(endpointRecord.scopes).length === 0" class="endpoint-muted">未配置</span>
            </a-space>
          </a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="endpointPolicyNotes(endpointRecord).length"
          type="info"
          show-icon
          class="endpoint-notes">
          <template #message>
            <a-space size="small" wrap>
              <a-tag v-for="note in endpointPolicyNotes(endpointRecord)" :key="note" color="processing">
                {{ note }}
              </a-tag>
            </a-space>
          </template>
        </a-alert>

        <div class="endpoint-sections">
          <section v-for="section in endpointSections(endpointRecord)" :key="section.key" class="endpoint-section">
            <div class="endpoint-section-title">
              <strong>{{ section.title }}</strong>
              <span>{{ section.description }}</span>
            </div>

            <div class="endpoint-list">
              <article v-for="endpoint in section.endpoints" :key="endpoint.key" class="endpoint-row">
                <a-tag class="endpoint-method" :color="methodColor(endpoint.method)">
                  {{ endpoint.method }}
                </a-tag>
                <div class="endpoint-row-main">
                  <div class="endpoint-row-head">
                    <strong>{{ endpoint.title }}</strong>
                    <a-space size="small" wrap>
                      <a-tag v-for="tag in endpoint.tags" :key="tag" color="default">{{ tag }}</a-tag>
                    </a-space>
                  </div>
                  <a-typography-paragraph class="endpoint-url" copyable>
                    {{ endpoint.url }}
                  </a-typography-paragraph>
                  <p class="endpoint-desc">{{ endpoint.description }}</p>
                  <a-typography-paragraph v-if="endpoint.curl" class="endpoint-curl" copyable>
                    {{ endpoint.curl }}
                  </a-typography-paragraph>
                </div>
              </article>
            </div>
          </section>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ApiOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { getApplications, createApplication, updateApplication, deleteApplication, rotateSecret, updateAppStatus, getTenants } from '../../api'

const applications = ref([])
const tenants = ref([])
const selectedTenantId = ref(undefined)
const loading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const secretVisible = ref(false)
const secretData = ref({})
const endpointVisible = ref(false)
const endpointRecord = ref(null)

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
const dpopAlgorithmOptions = ['ES256', 'ES384', 'ES512', 'RS256', 'RS384', 'RS512', 'PS256', 'PS384', 'PS512']
const authMethodMap = Object.fromEntries(authMethodOptions.map(item => [item.value, item]))
const grantTypeMap = Object.fromEntries(grantTypeOptions.map(item => [item.value, item]))
const DEFAULT_TENANT_ID = 'tenant-default'
const tenantOptions = computed(() => tenants.value.filter(tenant => tenant.id !== DEFAULT_TENANT_ID))

const form = reactive({
  tenantId: DEFAULT_TENANT_ID,
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
    signingAlgorithm: '',
    x509CertificateSubjectDN: '',
    dpopEnabled: false,
    dpopRequired: false,
    dpopAllowedAlgorithms: []
  },
  tokenSettings: {
    idTokenSignatureAlgorithm: 'RS256',
    tokenFormat: 'self-contained',
    x509CertificateBoundAccessTokens: false
  },
  mfaRequired: false,
  dynamicClientRegistrar: false
})

const columns = [
  { title: '应用名称', dataIndex: 'clientName', key: 'name', width: 220 },
  { title: 'Client ID', dataIndex: 'clientId', key: 'clientId', ellipsis: true, width: 260 },
  { title: '租户', key: 'tenant', width: 200 },
  { title: '来源', key: 'source', width: 160 },
  { title: '认证方式', dataIndex: 'clientAuthenticationMethods', key: 'auth', width: 300 },
  { title: '授权类型', dataIndex: 'authorizationGrantTypes', key: 'grant', width: 320 },
  { title: 'DPoP', key: 'dpop', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: 'MFA', key: 'mfaRequired', width: 80 },
  { title: '操作', key: 'action', width: 340, fixed: 'right' }
]

const usesSecretJwt = computed(() => form.clientAuthenticationMethods.includes('client_secret_jwt'))
const usesPrivateKeyJwt = computed(() => form.clientAuthenticationMethods.includes('private_key_jwt'))
const usesTlsClientAuth = computed(() => form.clientAuthenticationMethods.includes('tls_client_auth'))
const usesSelfSignedTlsClientAuth = computed(() => form.clientAuthenticationMethods.includes('self_signed_tls_client_auth'))
const usesJwtAuth = computed(() => usesSecretJwt.value || usesPrivateKeyJwt.value)
const usesMtlsAuth = computed(() => usesTlsClientAuth.value || usesSelfSignedTlsClientAuth.value)
const usesJwkSetUrl = computed(() => usesPrivateKeyJwt.value || usesSelfSignedTlsClientAuth.value)
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
const registrationSourceLabel = (source) => source === 'DYNAMIC_REGISTRATION' ? 'DCR' : 'Admin'
const hasSecretMethod = (record) => recordAuthMethods(record).some(method => method.startsWith('client_secret'))

const endpointTenant = (record) => tenants.value.find(item => item.id === record?.tenantId)

const endpointTenantIssuerEnabled = (record) => {
  if (!record?.issuerPath) {
    return true
  }
  const tenant = endpointTenant(record)
  return tenant?.pathIssuerEnabled !== false
}

const sasOrigin = computed(() => {
  const origin = new URL(window.location.origin)
  if (origin.hostname === 'localhost' || origin.hostname === '127.0.0.1') {
    origin.port = '9000'
  }
  return origin.origin
})

const endpointIssuer = (record) => `${sasOrigin.value}${record?.issuerPath || ''}`

const endpointUrl = (record, path) => `${endpointIssuer(record)}${path}`

const firstValue = (value) => parseList(value)[0] || ''

const scopeValue = (record) => {
  const scopes = parseList(record.scopes)
  return scopes.length ? scopes.join(' ') : 'openid'
}

const clientAuthCurl = (record) => {
  const methods = recordAuthMethods(record)
  if (methods.includes('client_secret_basic')) {
    return `-u "${record.clientId}:<client-secret>"`
  }
  if (methods.includes('tls_client_auth') || methods.includes('self_signed_tls_client_auth')) {
    return '--cert client.crt --key client.key'
  }
  return ''
}

const appendAuthBody = (record, body) => {
  const methods = recordAuthMethods(record)
  if (methods.includes('client_secret_post')) {
    return `${body}&client_id=${record.clientId}&client_secret=<client-secret>`
  }
  if (methods.includes('private_key_jwt')) {
    return `${body}&client_id=${record.clientId}&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer&client_assertion=<signed-jwt>`
  }
  if (methods.includes('client_secret_jwt')) {
    return `${body}&client_id=${record.clientId}&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer&client_assertion=<hmac-signed-jwt>`
  }
  if (methods.includes('tls_client_auth') || methods.includes('self_signed_tls_client_auth') || methods.includes('none')) {
    return `${body}&client_id=${record.clientId}`
  }
  return body
}

const curlForFormPost = (record, path, body, extraHeaders = '') => {
  const auth = clientAuthCurl(record)
  const dpopHeader = record.clientSettings?.dpopEnabled ? ' -H "DPoP: <dpop-proof-jwt>"' : ''
  const headers = `-H "Content-Type: application/x-www-form-urlencoded"${dpopHeader}${extraHeaders}`
  const authPart = auth ? `${auth} ` : ''
  return `curl ${authPart}-X POST "${endpointUrl(record, path)}" ${headers} -d "${appendAuthBody(record, body)}"`
}

const authorizationUrl = (record) => {
  const redirectUri = firstValue(record.redirectUris) || 'http://127.0.0.1:3000/callback'
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: record.clientId,
    redirect_uri: redirectUri,
    scope: scopeValue(record),
    state: 'demo-state'
  })
  if (record.clientSettings?.requireProofKey) {
    params.set('code_challenge', '<code-challenge>')
    params.set('code_challenge_method', 'S256')
  }
  return `${endpointUrl(record, '/oauth2/authorize')}?${params.toString()}`
}

const endpointPolicyNotes = (record) => {
  const notes = []
  if (record.clientSettings?.requireProofKey) notes.push('授权码流程要求 PKCE')
  if (record.clientSettings?.requireAuthorizationConsent) notes.push('需要用户授权确认')
  if (record.clientSettings?.dpopEnabled) notes.push(record.clientSettings?.dpopRequired ? 'Token 请求必须带 DPoP proof' : 'Token 请求可带 DPoP proof')
  if (record.clientSettings?.dpopAllowedAlgorithms) notes.push(`DPoP alg: ${record.clientSettings.dpopAllowedAlgorithms}`)
  if (record.tokenSettings?.x509CertificateBoundAccessTokens) notes.push('Access Token 绑定客户端证书')
  if (record.dynamicClientRegistrar) notes.push('具备 DCR registrar scope')
  return notes
}

const methodColor = (method) => {
  if (method === 'GET') return 'green'
  if (method === 'POST') return 'orange'
  return 'blue'
}

const endpointSections = (record) => {
  if (!record) return []
  const grants = parseList(record.authorizationGrantTypes)
  const scopes = parseList(record.scopes)
  const confidential = !recordAuthMethods(record).includes('none')
  const sections = [
    {
      key: 'metadata',
      title: '元数据与公钥',
      description: '按应用所属 issuer 生成',
      endpoints: [
        {
          key: 'oidc-discovery',
          method: 'GET',
          title: 'OIDC Discovery',
          url: endpointUrl(record, '/.well-known/openid-configuration'),
          description: '客户端自动发现授权端点、token 端点、JWKS、注销和 PAR/DCR 元数据。',
          tags: ['public']
        },
        {
          key: 'oauth-metadata',
          method: 'GET',
          title: 'OAuth2 Authorization Server Metadata',
          url: endpointUrl(record, '/.well-known/oauth-authorization-server'),
          description: 'OAuth 2.0 授权服务器元数据，适合非 OIDC 客户端接入。',
          tags: ['public']
        },
        {
          key: 'jwks',
          method: 'GET',
          title: 'JWKS',
          url: endpointUrl(record, '/oauth2/jwks'),
          description: '资源服务器或客户端用于校验 JWT 签名的公开密钥集合。',
          tags: ['public']
        }
      ]
    }
  ]

  const authorizationEndpoints = []
  if (grants.includes('authorization_code')) {
    authorizationEndpoints.push(
      {
        key: 'authorize',
        method: 'GET',
        title: 'Authorization Endpoint',
        url: authorizationUrl(record),
        description: '按当前 clientId、redirectUri、scope 生成授权码流程入口。',
        tags: record.clientSettings?.requireProofKey ? ['PKCE'] : []
      },
      {
        key: 'token-code',
        method: 'POST',
        title: 'Token Endpoint - Authorization Code',
        url: endpointUrl(record, '/oauth2/token'),
        description: '用授权码换取 token；如果开启 DPoP，可在请求头加入 DPoP proof。',
        tags: ['authorization_code'],
        curl: curlForFormPost(record, '/oauth2/token', 'grant_type=authorization_code&code=<code>&redirect_uri=' + encodeURIComponent(firstValue(record.redirectUris) || 'http://127.0.0.1:3000/callback'))
      },
      {
        key: 'par',
        method: 'POST',
        title: 'Pushed Authorization Request',
        url: endpointUrl(record, '/oauth2/par'),
        description: '先把授权参数推送到服务端，再用返回的 request_uri 发起授权。',
        tags: ['PAR'],
        curl: curlForFormPost(record, '/oauth2/par', `response_type=code&client_id=${record.clientId}&redirect_uri=${encodeURIComponent(firstValue(record.redirectUris) || 'http://127.0.0.1:3000/callback')}&scope=${encodeURIComponent(scopeValue(record))}&state=demo-state`)
      }
    )
  }
  if (grants.includes('client_credentials')) {
    authorizationEndpoints.push({
      key: 'token-client-credentials',
      method: 'POST',
      title: 'Token Endpoint - Client Credentials',
      url: endpointUrl(record, '/oauth2/token'),
      description: '服务间调用获取 access token。',
      tags: ['client_credentials'],
      curl: curlForFormPost(record, '/oauth2/token', `grant_type=client_credentials&scope=${encodeURIComponent(scopeValue(record))}`)
    })
  }
  if (grants.includes('refresh_token')) {
    authorizationEndpoints.push({
      key: 'token-refresh',
      method: 'POST',
      title: 'Token Endpoint - Refresh Token',
      url: endpointUrl(record, '/oauth2/token'),
      description: '用 refresh_token 续期 access token。',
      tags: ['refresh_token'],
      curl: curlForFormPost(record, '/oauth2/token', 'grant_type=refresh_token&refresh_token=<refresh-token>')
    })
  }
  if (grants.includes('urn:ietf:params:oauth:grant-type:device_code')) {
    authorizationEndpoints.push(
      {
        key: 'device-authorization',
        method: 'POST',
        title: 'Device Authorization',
        url: endpointUrl(record, '/oauth2/device_authorization'),
        description: '设备端获取 device_code、user_code 和 verification_uri。',
        tags: ['device_code'],
        curl: curlForFormPost(record, '/oauth2/device_authorization', `scope=${encodeURIComponent(scopeValue(record))}`)
      },
      {
        key: 'device-verification',
        method: 'GET/POST',
        title: 'Device Verification',
        url: endpointUrl(record, '/oauth2/device_verification'),
        description: '用户输入 user_code 并完成授权确认的验证端点。',
        tags: ['device_code']
      },
      {
        key: 'activate',
        method: 'GET',
        title: 'Device Activation Page',
        url: endpointUrl(record, '/activate'),
        description: 'ArmorAuth 托管的设备激活页面。',
        tags: ['hosted-page']
      },
      {
        key: 'token-device',
        method: 'POST',
        title: 'Token Endpoint - Device Code',
        url: endpointUrl(record, '/oauth2/token'),
        description: '设备轮询 device_code，授权完成后换取 token。',
        tags: ['device_code'],
        curl: curlForFormPost(record, '/oauth2/token', 'grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=<device-code>')
      }
    )
  }
  if (grants.includes('urn:ietf:params:oauth:grant-type:jwt-bearer')) {
    authorizationEndpoints.push({
      key: 'token-jwt-bearer',
      method: 'POST',
      title: 'Token Endpoint - JWT Bearer',
      url: endpointUrl(record, '/oauth2/token'),
      description: 'JWT Bearer 扩展授权类型 token 入口。',
      tags: ['jwt-bearer'],
      curl: curlForFormPost(record, '/oauth2/token', 'grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=<jwt-assertion>')
    })
  }
  if (grants.includes('urn:ietf:params:oauth:grant-type:token-exchange')) {
    authorizationEndpoints.push({
      key: 'token-exchange',
      method: 'POST',
      title: 'Token Endpoint - Token Exchange',
      url: endpointUrl(record, '/oauth2/token'),
      description: 'Token Exchange 扩展授权类型 token 入口。',
      tags: ['token-exchange'],
      curl: curlForFormPost(record, '/oauth2/token', 'grant_type=urn:ietf:params:oauth:grant-type:token-exchange&subject_token=<token>&subject_token_type=urn:ietf:params:oauth:token-type:access_token')
    })
  }
  if (authorizationEndpoints.length) {
    sections.push({
      key: 'authorization',
      title: '授权与 Token',
      description: '仅显示此应用授权类型能使用的入口',
      endpoints: authorizationEndpoints
    })
  }

  const oidcEndpoints = []
  if (scopes.includes('openid')) {
    oidcEndpoints.push(
      {
        key: 'userinfo',
        method: 'GET',
        title: 'UserInfo',
        url: endpointUrl(record, '/userinfo'),
        description: '使用包含 openid scope 的 access token 查询用户信息。',
        tags: ['OIDC']
      },
      {
        key: 'logout',
        method: 'GET',
        title: 'OIDC Logout',
        url: endpointUrl(record, '/connect/logout'),
        description: '使用 id_token_hint 和可选 post_logout_redirect_uri 发起 OIDC RP-Initiated Logout。',
        tags: ['OIDC']
      }
    )
  }
  if (oidcEndpoints.length) {
    sections.push({
      key: 'oidc',
      title: 'OIDC 用户端点',
      description: '由 openid scope 决定是否展示',
      endpoints: oidcEndpoints
    })
  }

  const opsEndpoints = []
  if (confidential) {
    opsEndpoints.push(
      {
        key: 'introspect',
        method: 'POST',
        title: 'Token Introspection',
        url: endpointUrl(record, '/oauth2/introspect'),
        description: '校验 opaque/reference token 或查询 token 活跃状态。',
        tags: ['confidential'],
        curl: curlForFormPost(record, '/oauth2/introspect', 'token=<access-token>')
      },
      {
        key: 'revoke',
        method: 'POST',
        title: 'Token Revocation',
        url: endpointUrl(record, '/oauth2/revoke'),
        description: '撤销 access token 或 refresh token。',
        tags: ['confidential'],
        curl: curlForFormPost(record, '/oauth2/revoke', 'token=<token>')
      }
    )
  }
  if (record.dynamicClientRegistrar) {
    opsEndpoints.push({
      key: 'dcr',
      method: 'POST',
      title: 'Dynamic Client Registration',
      url: endpointUrl(record, '/connect/register'),
      description: '当前应用具备 client.create/client.read registrar 能力时，用它的 access token 注册新客户端。',
      tags: ['DCR'],
      curl: `curl -X POST "${endpointUrl(record, '/connect/register')}" -H "Authorization: Bearer <registrar-access-token>" -H "Content-Type: application/json" -d '{"client_name":"demo","redirect_uris":["http://127.0.0.1:3000/callback"],"grant_types":["authorization_code"],"response_types":["code"],"scope":"openid profile"}'`
    })
  }
  if (opsEndpoints.length) {
    sections.push({
      key: 'operations',
      title: '运维与注册',
      description: '按客户端认证方式和 DCR registrar 配置展示',
      endpoints: opsEndpoints
    })
  }

  return sections
}

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

const ensureMtlsSettings = () => {
  if (!usesTlsClientAuth.value) {
    form.clientSettings.x509CertificateSubjectDN = ''
  }
  if (!usesMtlsAuth.value) {
    form.tokenSettings.x509CertificateBoundAccessTokens = false
  }
}

const ensureDcrRegistrarSettings = () => {
  if (!form.dynamicClientRegistrar) {
    form.scopes = form.scopes.filter(scope => scope !== 'client.create' && scope !== 'client.read')
    return
  }
  if (!form.authorizationGrantTypes.includes('client_credentials')) {
    form.authorizationGrantTypes.push('client_credentials')
  }
  for (const scope of ['client.create', 'client.read']) {
    if (!form.scopes.includes(scope)) {
      form.scopes.push(scope)
    }
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
  ensureMtlsSettings()
}

const handleDcrRegistrarChange = () => {
  ensureDcrRegistrarSettings()
}

const handleDpopEnabledChange = () => {
  if (!form.clientSettings.dpopEnabled) {
    form.clientSettings.dpopRequired = false
    form.clientSettings.dpopAllowedAlgorithms = []
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getApplications(pagination.current - 1, pagination.pageSize, selectedTenantId.value)
    applications.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (e) {
    message.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const fetchTenants = async () => {
  try {
    const res = await getTenants(0, 200)
    tenants.value = res.data?.content || []
  } catch (e) {
    message.error('加载租户失败: ' + e.message)
  }
}

const handleTenantFilterChange = () => {
  pagination.current = 1
  fetchData()
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const resetForm = () => {
  form.tenantId = selectedTenantId.value || DEFAULT_TENANT_ID
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
  form.clientSettings.x509CertificateSubjectDN = ''
  form.clientSettings.dpopEnabled = false
  form.clientSettings.dpopRequired = false
  form.clientSettings.dpopAllowedAlgorithms = []
  form.tokenSettings.idTokenSignatureAlgorithm = 'RS256'
  form.tokenSettings.tokenFormat = 'self-contained'
  form.tokenSettings.x509CertificateBoundAccessTokens = false
  form.mfaRequired = false
  form.dynamicClientRegistrar = false
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
  form.tenantId = record.tenantId || DEFAULT_TENANT_ID
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
  form.clientSettings.x509CertificateSubjectDN = record.clientSettings?.x509CertificateSubjectDN || ''
  form.clientSettings.dpopEnabled = !!record.clientSettings?.dpopEnabled
  form.clientSettings.dpopRequired = !!record.clientSettings?.dpopRequired
  form.clientSettings.dpopAllowedAlgorithms = parseList(record.clientSettings?.dpopAllowedAlgorithms)
  form.tokenSettings.idTokenSignatureAlgorithm = record.tokenSettings?.idTokenSignatureAlgorithm || 'RS256'
  form.tokenSettings.tokenFormat = record.tokenSettings?.tokenFormat || 'self-contained'
  form.tokenSettings.x509CertificateBoundAccessTokens = !!record.tokenSettings?.x509CertificateBoundAccessTokens
  form.mfaRequired = record.mfaRequired || false
  form.dynamicClientRegistrar = !!record.dynamicClientRegistrar
  ensureSigningAlgorithm()
  ensureMtlsSettings()
  modalVisible.value = true
}

const showEndpointDetails = (record) => {
  endpointRecord.value = record
  endpointVisible.value = true
}

const handleSubmit = async () => {
  if (!form.tenantId || !form.clientName || form.clientAuthenticationMethods.length === 0 || form.authorizationGrantTypes.length === 0) {
    message.warning('归属租户、应用名称、认证方式和授权类型不能为空')
    return
  }
  if (usesTlsClientAuth.value && !nullableText(form.clientSettings.x509CertificateSubjectDN)) {
    message.warning('TLS Client Auth 需要配置 X.509 Subject DN')
    return
  }
  if (usesSelfSignedTlsClientAuth.value && !nullableText(form.clientSettings.jwkSetUrl)) {
    message.warning('Self-signed TLS Client Auth 需要配置 JWK Set URL')
    return
  }
  if (form.tokenSettings.x509CertificateBoundAccessTokens && !usesMtlsAuth.value) {
    message.warning('证书绑定 Access Token 只能用于 mTLS 客户端')
    return
  }
  if (form.clientSettings.dpopRequired && !form.clientSettings.dpopEnabled) {
    message.warning('要求 DPoP Proof 需要先启用 DPoP')
    return
  }
  if (form.dynamicClientRegistrar && !form.authorizationGrantTypes.includes('client_credentials')) {
    message.warning('DCR Registrar 需要 client_credentials 授权类型')
    return
  }
  submitting.value = true
  try {
    ensureDcrRegistrarSettings()
    const data = {
      tenantId: form.tenantId,
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
        signingAlgorithm: nullableText(form.clientSettings.signingAlgorithm),
        x509CertificateSubjectDN: nullableText(form.clientSettings.x509CertificateSubjectDN),
        dpopEnabled: form.clientSettings.dpopEnabled,
        dpopRequired: form.clientSettings.dpopRequired,
        dpopAllowedAlgorithms: form.clientSettings.dpopAllowedAlgorithms.join(',')
      },
      tokenSettings: {
        idTokenSignatureAlgorithm: nullableText(form.tokenSettings.idTokenSignatureAlgorithm),
        tokenFormat: nullableText(form.tokenSettings.tokenFormat),
        x509CertificateBoundAccessTokens: form.tokenSettings.x509CertificateBoundAccessTokens
      },
      mfaRequired: form.mfaRequired,
      dynamicClientRegistrar: form.dynamicClientRegistrar
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

onMounted(async () => {
  await fetchTenants()
  await fetchData()
})
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

.endpoint-detail {
  display: grid;
  gap: 14px;
}

.endpoint-summary {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--aa-border-light);
  border-radius: 8px;
  background: linear-gradient(180deg, #f8fafc, #ffffff);
}

.endpoint-kicker {
  margin-bottom: 6px;
  color: var(--aa-text-secondary);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.endpoint-issuer {
  margin-bottom: 0 !important;
  font-family: var(--aa-font-mono);
  font-size: 13px;
  word-break: break-all;
}

.endpoint-config :deep(.ant-typography) {
  margin-bottom: 0;
}

.endpoint-notes {
  border-radius: 8px;
}

.endpoint-sections {
  display: grid;
  gap: 12px;
  max-height: 56vh;
  padding-right: 4px;
  overflow-y: auto;
}

.endpoint-section {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--aa-border-light);
  border-radius: 8px;
  background: #fff;
}

.endpoint-section-title {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.endpoint-section-title strong {
  font-size: 14px;
}

.endpoint-section-title span,
.endpoint-muted,
.endpoint-desc {
  color: var(--aa-text-secondary);
  font-size: 12px;
}

.endpoint-list {
  display: grid;
  gap: 8px;
}

.endpoint-row {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr);
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--aa-border-light);
  border-radius: 8px;
  background: #fbfdff;
}

.endpoint-method {
  width: fit-content;
  height: 24px;
  margin-top: 2px;
  text-align: center;
}

.endpoint-row-main {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.endpoint-row-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.endpoint-row-head strong {
  min-width: 0;
  font-size: 13px;
}

.endpoint-url,
.endpoint-curl {
  margin-bottom: 0 !important;
  padding: 7px 9px;
  border-radius: 6px;
  background: #f1f5f9;
  font-family: var(--aa-font-mono);
  font-size: 12px;
  line-height: 1.5;
  word-break: break-all;
}

.endpoint-curl {
  background: #111827;
  color: #e5e7eb;
}

.endpoint-desc {
  margin: 0;
  line-height: 1.5;
}

@media (max-width: 720px) {
  .setting-grid {
    grid-template-columns: 1fr;
  }

  .endpoint-summary,
  .endpoint-section-title,
  .endpoint-row-head {
    display: grid;
  }

  .endpoint-row {
    grid-template-columns: 1fr;
  }
}
</style>
