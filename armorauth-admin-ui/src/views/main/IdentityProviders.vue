<template>
  <div class="main-page page-container">
    <div class="page-header">
      <h2>身份源管理</h2>
      <a-button type="primary" @click="showCreate">
        <template #icon><PlusOutlined /></template>
        添加身份源
      </a-button>
    </div>

    <a-table :dataSource="providers" :columns="columns" :loading="loading"
             :pagination="pagination" @change="handleTableChange" row-key="id" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag color="blue">{{ record.providerType }}</a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-switch :checked="record.enabled" @change="val => toggleStatus(record, val)"
                    checked-children="启用" un-checked-children="禁用" />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="showEdit(record)">编辑</a>
            <a @click="handleTest(record, false)">测试</a>
            <a-popconfirm title="远程探测会访问身份源配置的外部 URL，确认执行？" @confirm="handleTest(record, true)">
              <a>远程探测</a>
            </a-popconfirm>
            <a v-if="record.providerType === 'LDAP'" @click="handleLdapSync(record, true)">同步预演</a>
            <a-popconfirm v-if="record.providerType === 'LDAP'" title="确认把 LDAP/AD 用户同步到本地目录？" @confirm="handleLdapSync(record, false)">
              <a>同步</a>
            </a-popconfirm>
            <a-popconfirm title="确认删除此身份源？" @confirm="handleDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" :title="isEdit ? '编辑身份源' : '添加身份源'"
             @ok="handleSubmit" :confirmLoading="submitting" width="640px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="提供商名称" required>
          <a-input v-model:value="form.providerName" placeholder="如 企业SSO" />
        </a-form-item>
        <a-form-item label="提供商类型" required>
          <a-select v-model:value="form.providerType" :disabled="isEdit" placeholder="选择类型">
            <a-select-option v-for="t in providerTypes" :key="t" :value="t">{{ t }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="Registration ID" required>
          <a-input v-model:value="form.registrationId" :disabled="isEdit" placeholder="唯一标识，如 enterprise-oidc" />
        </a-form-item>
        <template v-if="form.providerType === 'SAML'">
          <a-form-item label="IdP Entity ID">
            <a-input v-model:value="form.samlEntityId" placeholder="https://idp.example.com/entity-id" />
          </a-form-item>
          <a-form-item label="Metadata URL">
            <a-input v-model:value="form.samlMetadataUrl" placeholder="https://idp.example.com/metadata" />
          </a-form-item>
          <a-form-item label="SSO URL">
            <a-input v-model:value="form.samlSsoUrl" placeholder="https://idp.example.com/sso" />
          </a-form-item>
          <a-form-item label="SLO URL">
            <a-input v-model:value="form.samlSloUrl" placeholder="https://idp.example.com/logout" />
          </a-form-item>
          <a-form-item label="X.509 Certificate">
            <a-textarea v-model:value="form.samlX509Certificate" :rows="4" placeholder="-----BEGIN CERTIFICATE-----" />
          </a-form-item>
          <a-form-item label="SP Entity ID">
            <a-input v-model:value="form.samlSpEntityId" placeholder="https://auth.example.com/saml/sp" />
          </a-form-item>
          <a-form-item label="ACS URL">
            <a-input v-model:value="form.samlAcsUrl" placeholder="https://auth.example.com/login/saml2/sso/{registrationId}" />
          </a-form-item>
          <a-form-item label="NameID Format">
            <a-input v-model:value="form.samlNameIdFormat" placeholder="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress" />
          </a-form-item>
        </template>
        <template v-else-if="form.providerType === 'LDAP'">
          <a-form-item label="LDAP URL" required>
            <a-input v-model:value="form.ldapUrl" placeholder="ldap://ad.example.com:389" />
          </a-form-item>
          <a-form-item label="Base DN" required>
            <a-input v-model:value="form.ldapBaseDn" placeholder="dc=example,dc=com" />
          </a-form-item>
          <a-form-item label="Bind DN">
            <a-input v-model:value="form.ldapBindDn" placeholder="cn=reader,dc=example,dc=com" />
          </a-form-item>
          <a-form-item label="Bind Password">
            <a-input-password v-model:value="form.ldapBindPassword" />
          </a-form-item>
          <a-form-item label="User Search Base">
            <a-input v-model:value="form.ldapUserSearchBase" placeholder="ou=users" />
          </a-form-item>
          <a-form-item label="User Search Filter">
            <a-input v-model:value="form.ldapUserSearchFilter" placeholder="(objectClass=person)" />
          </a-form-item>
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item label="Username Attribute">
                <a-input v-model:value="form.ldapUsernameAttribute" placeholder="uid / sAMAccountName" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="Email Attribute">
                <a-input v-model:value="form.ldapEmailAttribute" placeholder="mail" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item label="Phone Attribute">
                <a-input v-model:value="form.ldapPhoneAttribute" placeholder="telephoneNumber" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="Display Name Attribute">
                <a-input v-model:value="form.ldapDisplayNameAttribute" placeholder="displayName" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item label="Group Attribute">
                <a-input v-model:value="form.ldapGroupAttribute" placeholder="memberOf" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="Page Size">
                <a-input-number v-model:value="form.ldapPageSize" :min="1" :max="1000" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-space>
            <a-checkbox v-model:checked="form.ldapUseSsl">LDAPS</a-checkbox>
            <a-checkbox v-model:checked="form.ldapStartTls">StartTLS</a-checkbox>
          </a-space>
        </template>
        <template v-else>
          <a-form-item label="Client ID" required>
            <a-input v-model:value="form.clientId" />
          </a-form-item>
          <a-form-item label="Client Secret">
            <a-input-password v-model:value="form.clientSecret" />
          </a-form-item>
          <a-form-item label="Authorization URI">
            <a-input v-model:value="form.authorizationUri" placeholder="https://..." />
          </a-form-item>
          <a-form-item label="Token URI">
            <a-input v-model:value="form.tokenUri" placeholder="https://..." />
          </a-form-item>
          <a-form-item label="UserInfo URI">
            <a-input v-model:value="form.userinfoUri" placeholder="https://..." />
          </a-form-item>
          <a-form-item label="JWK Set URI">
            <a-input v-model:value="form.jwkSetUri" placeholder="https://.../jwks" />
          </a-form-item>
          <a-form-item label="Scopes">
            <a-input v-model:value="form.scopes" placeholder="逗号分隔，如 openid,profile,email" />
          </a-form-item>
        </template>
        <a-form-item label="属性映射 JSON">
          <a-textarea v-model:value="form.attributeMapping" :rows="3"
                      placeholder='{"email":"email","displayName":"name"}' />
        </a-form-item>
        <a-form-item label="链接策略">
          <a-select v-model:value="form.linkingStrategy" placeholder="选择策略">
            <a-select-option value="AUTO_REGISTER">自动注册</a-select-option>
            <a-select-option value="CONFIRM">确认页</a-select-option>
            <a-select-option value="EMAIL_MATCH">邮箱匹配</a-select-option>
            <a-select-option value="NONE">禁止自动注册</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="显示顺序">
          <a-input-number v-model:value="form.displayOrder" :min="0" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="testVisible" title="身份源测试结果" :footer="null" width="640px">
      <a-alert :type="testResult.success ? 'success' : 'error'"
               :message="testResult.message || (testResult.success ? '配置检查通过' : '配置检查失败')"
               show-icon style="margin-bottom: 16px" />
      <a-descriptions bordered size="small" :column="1">
        <a-descriptions-item v-for="item in checkItems" :key="item.key" :label="item.key">
          <a-tag :color="checkColor(item.value)">{{ checkLabel(item.value) }}</a-tag>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <a-modal v-model:open="syncVisible" title="LDAP/AD 同步结果" :footer="null" width="640px">
      <a-alert :type="syncResult.failed ? 'warning' : 'success'"
               :message="syncResult.message || '同步完成'"
               show-icon style="margin-bottom: 16px" />
      <a-descriptions bordered size="small" :column="2">
        <a-descriptions-item label="模式">{{ syncResult.dryRun ? '预演' : '执行' }}</a-descriptions-item>
        <a-descriptions-item label="扫描">{{ syncResult.scanned ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="待创建">{{ syncResult.wouldCreate ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="待更新">{{ syncResult.wouldUpdate ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="已创建">{{ syncResult.created ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="已更新">{{ syncResult.updated ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="跳过">{{ syncResult.skipped ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="失败">{{ syncResult.failed ?? 0 }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
  getIdentityProviders,
  createIdentityProvider,
  updateIdentityProvider,
  deleteIdentityProvider,
  updateIdpStatus,
  testIdentityProvider,
  syncIdentityProviderUsers
} from '../../api'

const providers = ref([])
const loading = ref(false)
const modalVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const testVisible = ref(false)
const testResult = ref({ success: false, message: '', checks: {} })
const syncVisible = ref(false)
const syncResult = ref({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const providerTypes = ['OIDC', 'WECHAT', 'WECOM', 'DINGTALK', 'FEISHU', 'ALIPAY', 'QQ', 'GITEE', 'SAML', 'LDAP', 'CUSTOM']

const form = reactive({
  providerName: '', providerType: 'OIDC', registrationId: '', clientId: '', clientSecret: '',
  authorizationUri: '', tokenUri: '', userinfoUri: '', jwkSetUri: '', scopes: '',
  samlEntityId: '', samlSsoUrl: '', samlSloUrl: '', samlX509Certificate: '',
  samlMetadataUrl: '', samlSpEntityId: '', samlAcsUrl: '', samlNameIdFormat: '',
  ldapUrl: '', ldapBaseDn: '', ldapBindDn: '', ldapBindPassword: '',
  ldapUserSearchBase: '', ldapUserSearchFilter: '(objectClass=person)',
  ldapUsernameAttribute: 'uid', ldapEmailAttribute: 'mail', ldapPhoneAttribute: 'telephoneNumber',
  ldapDisplayNameAttribute: 'displayName', ldapGroupAttribute: 'memberOf',
  ldapUseSsl: false, ldapStartTls: false, ldapPageSize: 200,
  attributeMapping: '', linkingStrategy: 'AUTO_REGISTER', displayOrder: 0
})

const columns = [
  { title: '名称', dataIndex: 'providerName', key: 'name', width: 150 },
  { title: '类型', key: 'type', width: 100 },
  { title: 'Registration ID', dataIndex: 'registrationId', key: 'regId', width: 180 },
  { title: 'Client ID', dataIndex: 'clientId', key: 'clientId', ellipsis: true },
  { title: '链接策略', dataIndex: 'linkingStrategy', key: 'strategy', width: 120 },
  { title: '顺序', dataIndex: 'displayOrder', key: 'order', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' }
]

const checkItems = computed(() =>
  Object.entries(testResult.value.checks || {}).map(([key, value]) => ({ key, value }))
)

const checkColor = (value) => {
  if (value === true || value === 'ok' || value === 'optional' || value === 'skipped'
    || value === 'manual' || value === 'metadata_url') return 'green'
  if (value === 'pending') return 'gold'
  if (typeof value === 'number' && value < 500) return 'green'
  return 'red'
}

const checkLabel = (value) => {
  if (value === true) return 'ok'
  if (value === false) return 'failed'
  return String(value)
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getIdentityProviders(pagination.current - 1, pagination.pageSize)
    providers.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (e) { message.error('加载失败') }
  finally { loading.value = false }
}

const handleTableChange = (pag) => { pagination.current = pag.current; fetchData() }

const resetForm = () => {
  form.providerName = ''; form.providerType = 'OIDC'; form.registrationId = ''
  form.clientId = ''; form.clientSecret = ''; form.authorizationUri = ''
  form.tokenUri = ''; form.userinfoUri = ''; form.jwkSetUri = ''; form.scopes = ''
  form.samlEntityId = ''; form.samlSsoUrl = ''; form.samlSloUrl = ''; form.samlX509Certificate = ''
  form.samlMetadataUrl = ''; form.samlSpEntityId = ''; form.samlAcsUrl = ''; form.samlNameIdFormat = ''
  form.ldapUrl = ''; form.ldapBaseDn = ''; form.ldapBindDn = ''; form.ldapBindPassword = ''
  form.ldapUserSearchBase = ''; form.ldapUserSearchFilter = '(objectClass=person)'
  form.ldapUsernameAttribute = 'uid'; form.ldapEmailAttribute = 'mail'
  form.ldapPhoneAttribute = 'telephoneNumber'; form.ldapDisplayNameAttribute = 'displayName'
  form.ldapGroupAttribute = 'memberOf'; form.ldapUseSsl = false; form.ldapStartTls = false; form.ldapPageSize = 200
  form.attributeMapping = ''; form.linkingStrategy = 'AUTO_REGISTER'; form.displayOrder = 0
  editId.value = null
}

const showCreate = () => { isEdit.value = false; resetForm(); modalVisible.value = true }

const showEdit = (record) => {
  isEdit.value = true; editId.value = record.id
  Object.assign(form, {
    providerName: record.providerName, providerType: record.providerType,
    registrationId: record.registrationId, clientId: record.clientId,
    clientSecret: '', authorizationUri: record.authorizationUri || '',
    tokenUri: record.tokenUri || '', userinfoUri: record.userinfoUri || '',
    jwkSetUri: record.jwkSetUri || '', scopes: record.scopes || '',
    samlEntityId: record.samlEntityId || '', samlSsoUrl: record.samlSsoUrl || '',
    samlSloUrl: record.samlSloUrl || '', samlX509Certificate: record.samlX509Certificate || '',
    samlMetadataUrl: record.samlMetadataUrl || '', samlSpEntityId: record.samlSpEntityId || '',
    samlAcsUrl: record.samlAcsUrl || '', samlNameIdFormat: record.samlNameIdFormat || '',
    ldapUrl: record.ldapUrl || '', ldapBaseDn: record.ldapBaseDn || '',
    ldapBindDn: record.ldapBindDn || '', ldapBindPassword: '',
    ldapUserSearchBase: record.ldapUserSearchBase || '',
    ldapUserSearchFilter: record.ldapUserSearchFilter || '(objectClass=person)',
    ldapUsernameAttribute: record.ldapUsernameAttribute || 'uid',
    ldapEmailAttribute: record.ldapEmailAttribute || 'mail',
    ldapPhoneAttribute: record.ldapPhoneAttribute || 'telephoneNumber',
    ldapDisplayNameAttribute: record.ldapDisplayNameAttribute || 'displayName',
    ldapGroupAttribute: record.ldapGroupAttribute || 'memberOf',
    ldapUseSsl: !!record.ldapUseSsl,
    ldapStartTls: !!record.ldapStartTls,
    ldapPageSize: record.ldapPageSize || 200,
    attributeMapping: record.attributeMapping || '',
    linkingStrategy: record.linkingStrategy || 'AUTO_REGISTER',
    displayOrder: record.displayOrder || 0
  })
  modalVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateIdentityProvider(editId.value, form)
      message.success('更新成功')
    } else {
      await createIdentityProvider(form)
      message.success('创建成功')
    }
    modalVisible.value = false; fetchData()
  } catch (e) { message.error(e.message) }
  finally { submitting.value = false }
}

const handleDelete = async (id) => {
  try { await deleteIdentityProvider(id); message.success('删除成功'); fetchData() }
  catch (e) { message.error(e.message) }
}

const toggleStatus = async (record, enabled) => {
  try { await updateIdpStatus(record.id, enabled); record.enabled = enabled; message.success(enabled ? '已启用' : '已禁用') }
  catch (e) { message.error(e.message) }
}

const handleTest = async (record, probeRemote) => {
  try {
    const res = await testIdentityProvider(record.id, probeRemote)
    testResult.value = res.data || { success: false, message: '无返回结果', checks: {} }
    testVisible.value = true
  } catch (e) {
    message.error(e.message)
  }
}

const handleLdapSync = async (record, dryRun) => {
  try {
    const res = await syncIdentityProviderUsers(record.id, { dryRun, maxResults: 200 })
    syncResult.value = res.data || {}
    syncVisible.value = true
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
