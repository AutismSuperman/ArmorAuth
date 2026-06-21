import axios from 'axios'

const AUTH_TOKEN_KEY = 'admin_token'
const AUTH_USER_KEY = 'admin_user'
const AUTH_LOGIN_AT_KEY = 'admin_login_at'
const AUTH_VERIFIED_AT_KEY = 'admin_verified_at'

const api = axios.create({
  baseURL: '/api/admin/v1',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

const encodeBasicToken = (username, password) => {
  const credentials = `${username}:${password}`
  const bytes = new TextEncoder().encode(credentials)
  let binary = ''
  bytes.forEach(byte => { binary += String.fromCharCode(byte) })
  return btoa(binary)
}

const decodeBasicToken = (token) => {
  const binary = atob(token)
  const bytes = Uint8Array.from(binary, char => char.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

const safeJsonParse = (value) => {
  try {
    return value ? JSON.parse(value) : null
  } catch {
    return null
  }
}

const nowIso = () => new Date().toISOString()

const saveAdminUser = (payload = {}) => {
  const current = getAdminUser()
  const next = {
    username: payload.username || current.username || getAdminUsername() || '',
    displayName: payload.displayName || current.displayName || payload.username || current.username || '',
    authorities: payload.authorities || current.authorities || [],
    loginAt: payload.loginAt || current.loginAt || localStorage.getItem(AUTH_LOGIN_AT_KEY) || nowIso(),
    verifiedAt: payload.verifiedAt || current.verifiedAt || localStorage.getItem(AUTH_VERIFIED_AT_KEY) || ''
  }
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(next))
  localStorage.setItem(AUTH_LOGIN_AT_KEY, next.loginAt)
  if (next.verifiedAt) {
    localStorage.setItem(AUTH_VERIFIED_AT_KEY, next.verifiedAt)
  }
  return next
}

export const getAdminToken = () => localStorage.getItem(AUTH_TOKEN_KEY)

export const isAdminAuthenticated = () => !!getAdminToken()

export const getAdminUsername = () => {
  const token = getAdminToken()
  if (!token) return ''
  try {
    return decodeBasicToken(token).split(':')[0] || ''
  } catch {
    return ''
  }
}

export const getAdminUser = () => {
  const stored = safeJsonParse(localStorage.getItem(AUTH_USER_KEY))
  const username = stored?.username || getAdminUsername()
  return {
    username,
    displayName: stored?.displayName || username || '管理员',
    authorities: stored?.authorities || [],
    loginAt: stored?.loginAt || localStorage.getItem(AUTH_LOGIN_AT_KEY) || '',
    verifiedAt: stored?.verifiedAt || localStorage.getItem(AUTH_VERIFIED_AT_KEY) || ''
  }
}

export const clearAdminAuth = () => {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(AUTH_USER_KEY)
  localStorage.removeItem(AUTH_LOGIN_AT_KEY)
  localStorage.removeItem(AUTH_VERIFIED_AT_KEY)
}

const emitAuthEvent = (name, detail = {}) => {
  window.dispatchEvent(new CustomEvent(name, { detail }))
}

api.interceptors.request.use(config => {
  const token = getAdminToken()
  if (token && !config.headers.Authorization && !config.headers.authorization) {
    config.headers.Authorization = 'Basic ' + token
  }
  return config
})

api.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code >= 400) {
      const error = new Error(body.message || '请求失败')
      error.status = body.code
      error.code = body.code
      error.data = body
      return Promise.reject(error)
    }
    return body
  },
  err => {
    const status = err.response?.status || err.status
    const body = err.response?.data || err.data
    let msg = body?.message || err.message || '请求失败'

    if (status === 401) {
      clearAdminAuth()
      msg = '登录已过期或凭证无效，请重新登录'
      emitAuthEvent('armorauth:auth-expired', { status, message: msg })
    } else if (status === 403) {
      msg = body?.message || '当前账号没有此操作权限'
      emitAuthEvent('armorauth:forbidden', { status, message: msg, url: err.config?.url })
    }

    const error = new Error(msg)
    error.status = status
    error.code = body?.code || status
    error.data = body
    error.url = err.config?.url
    return Promise.reject(error)
  }
)

export const validateAdminSession = async () => {
  if (!getAdminToken()) {
    throw new Error('未登录')
  }
  const response = await getAdminProfile()
  const profile = response?.data || {}
  const username = profile.username || getAdminUsername()
  return saveAdminUser({
    username,
    displayName: getAdminUser().displayName || username,
    authorities: profile.authorities || [],
    verifiedAt: nowIso()
  })
}

export const loginAdmin = async (username, password) => {
  const normalizedUsername = (username || '').trim()
  if (!normalizedUsername || !password) {
    throw new Error('请输入用户名和密码')
  }

  clearAdminAuth()
  localStorage.setItem(AUTH_TOKEN_KEY, encodeBasicToken(normalizedUsername, password))
  saveAdminUser({
    username: normalizedUsername,
    displayName: normalizedUsername,
    loginAt: nowIso()
  })

  try {
    return await validateAdminSession()
  } catch (e) {
    clearAdminAuth()
    throw e
  }
}

// 当前管理员
export const getAdminProfile = () => api.get('/me')

// 应用管理
export const getApplications = (page = 0, size = 20, tenantId) => {
  const params = { page, size }
  if (tenantId) params.tenantId = tenantId
  return api.get('/applications', { params })
}
export const getApplication = (id) => api.get(`/applications/${id}`)
export const createApplication = (data) => api.post('/applications', data)
export const updateApplication = (id, data) => api.put(`/applications/${id}`, data)
export const deleteApplication = (id) => api.delete(`/applications/${id}`)
export const rotateSecret = (id) => api.post(`/applications/${id}/secret:rotate`)
export const updateAppStatus = (id, enabled) => api.patch(`/applications/${id}/status`, { enabled })

// Scope 管理
export const getScopes = (page = 0, size = 20, clientId) => {
  const params = { page, size }
  if (clientId) params.clientId = clientId
  return api.get('/scopes', { params })
}
export const createScope = (data) => api.post('/scopes', data)
export const updateScope = (clientId, scope, data) =>
  api.put('/scopes', data, { params: { clientId, scope } })
export const deleteScope = (clientId, scope) =>
  api.delete('/scopes', { params: { clientId, scope } })

// 登录策略
export const getLoginPolicies = (page = 0, size = 20) => api.get('/login-policies', { params: { page, size } })
export const getLoginPolicy = (id) => api.get(`/login-policies/${id}`)
export const updateLoginPolicy = (id, data) => api.put(`/login-policies/${id}`, data)

// 用户管理
export const getUsers = (page = 0, size = 20, keyword) => {
  const params = { page, size }
  if (keyword) params.keyword = keyword
  return api.get('/users', { params })
}
export const getUser = (id) => api.get(`/users/${id}`)
export const createUser = (data) => api.post('/users', data)
export const updateUser = (id, data) => api.put(`/users/${id}`, data)
export const deleteUser = (id) => api.delete(`/users/${id}`)
export const resetPassword = (id, password) => api.post(`/users/${id}/password:reset`, { newPassword: password })
export const lockUser = (id, durationMinutes = 30) => api.post(`/users/${id}/lock`, { durationMinutes })
export const unlockUser = (id) => api.post(`/users/${id}/unlock`)
export const updateUserStatus = (id, enabled) => api.patch(`/users/${id}/status`, { status: enabled ? 0 : 2 })

// 角色管理
export const getRoles = (page = 0, size = 50) => api.get('/roles', { params: { page, size } })
export const createRole = (data) => api.post('/roles', data)
export const deleteRole = (id) => api.delete(`/roles/${id}`)
export const getRoleBindings = (userId) => api.get('/role-bindings', { params: { userId } })
export const createRoleBinding = (data) => api.post('/role-bindings', data)
export const deleteRoleBinding = (id) => api.delete(`/role-bindings/${id}`)

// 权限管理
export const getPermissions = (page = 0, size = 50) => api.get('/permissions', { params: { page, size } })
export const createPermission = (data) => api.post('/permissions', data)
export const deletePermission = (id) => api.delete(`/permissions/${id}`)
export const getRolePermissions = (roleId) => api.get(`/roles/${roleId}/permissions`)
export const assignPermission = (roleId, permId) => api.post(`/roles/${roleId}/permissions/${permId}`)
export const removePermission = (roleId, permId) => api.delete(`/roles/${roleId}/permissions/${permId}`)

// 组织管理
export const getOrganizations = (page = 0, size = 20, tenantId) => {
  const url = tenantId ? `/tenants/${encodeURIComponent(tenantId)}/organizations` : '/organizations'
  return api.get(url, { params: { page, size } })
}
export const getOrganization = (id) => api.get(`/organizations/${id}`)
export const createOrganization = (data) => api.post('/organizations', data)
export const updateOrganization = (id, data) => api.put(`/organizations/${id}`, data)
export const deleteOrganization = (id) => api.delete(`/organizations/${id}`)
export const getOrgMembers = (orgId, page = 0, size = 20) => api.get(`/organizations/${orgId}/members`, { params: { page, size } })
export const addOrgMember = (orgId, data) => api.post(`/organizations/${orgId}/members`, data)
export const updateOrgMember = (orgId, userId, data) => api.put(`/organizations/${orgId}/members/${userId}`, data)
export const removeOrgMember = (orgId, userId) => api.delete(`/organizations/${orgId}/members/${userId}`)

// 身份源管理
export const getIdentityProviders = (page = 0, size = 20, source) => {
  const params = { page, size }
  if (source && source !== 'ALL') params.source = source
  return api.get('/identity-providers', { params })
}
export const getIdentityProvider = (id) => api.get(`/identity-providers/${id}`)
export const createIdentityProvider = (data) => api.post('/identity-providers', data)
export const updateIdentityProvider = (id, data) => api.put(`/identity-providers/${id}`, data)
export const deleteIdentityProvider = (id) => api.delete(`/identity-providers/${id}`)
export const updateIdpStatus = (id, enabled) => api.patch(`/identity-providers/${id}/status`, null, { params: { enabled } })
export const updateIdpLoginDisplay = (id, displayOnLogin) =>
  api.patch(`/identity-providers/${encodeURIComponent(id)}/login-display`, { displayOnLogin })
export const testIdentityProvider = (id, probeRemote = false) =>
  api.post(`/identity-providers/${id}:test`, null, { params: { probeRemote } })
export const syncIdentityProviderUsers = (id, data = { dryRun: true, maxResults: 200 }) =>
  api.post(`/identity-providers/${id}:sync-users`, data)

// 外部账号绑定
export const getFederatedBindings = (page = 0, size = 20, filters = {}) => {
  const params = { page, size }
  if (filters.userId) params.userId = filters.userId
  if (filters.registrationId) params.registrationId = filters.registrationId
  return api.get('/federated-bindings', { params })
}
export const deleteFederatedBinding = (id) => api.delete(`/federated-bindings/${id}`)

// 审计日志
export const getAuditEvents = (page = 0, size = 20, eventType, principalName) => {
  const params = { page, size }
  if (eventType) params.eventType = eventType
  if (principalName) params.principalName = principalName
  return api.get('/audit-events', { params })
}

// Token 统计
export const getTokenStatistics = (clientId, from, to) =>
  api.get('/token-statistics', { params: { clientId, from, to } })
export const getTokenSummary = (from, to) =>
  api.get('/token-statistics/summary', { params: { from, to } })

// Webhook 管理
export const getWebhooks = (page = 0, size = 20) => api.get('/webhooks', { params: { page, size } })
export const createWebhook = (data) => api.post('/webhooks', data)
export const updateWebhook = (id, data) => api.put(`/webhooks/${id}`, data)
export const deleteWebhook = (id) => api.delete(`/webhooks/${id}`)

// Secret 保护运维
export const rekeySecrets = (dryRun = true) => api.post('/secret-protection/rekey', { dryRun })

// JWK 密钥管理
export const getJwkKeys = () => api.get('/jwk-keys')
export const rotateJwkKey = (algorithm) => api.post('/jwk-keys/rotate', algorithm ? { algorithm } : {})
export const retireJwkKey = (kid) => api.post(`/jwk-keys/${encodeURIComponent(kid)}/retire`)
export const deleteJwkKey = (kid) => api.delete(`/jwk-keys/${encodeURIComponent(kid)}`)

// 会话管理
export const getSessions = () => api.get('/sessions')
export const getUserSessions = (username) => api.get(`/sessions/${encodeURIComponent(username)}`)
export const expireSession = (sessionId) => api.delete(`/sessions/${encodeURIComponent(sessionId)}`)

// 租户管理
export const getTenants = (page = 0, size = 20) => api.get('/tenants', { params: { page, size } })
export const getTenant = (id) => api.get(`/tenants/${id}`)
export const createTenant = (data) => api.post('/tenants', data)
export const updateTenant = (id, data) => api.put(`/tenants/${id}`, data)
export const updateTenantStatus = (id, enabled) => api.patch(`/tenants/${id}/status`, null, { params: { enabled } })
export const deleteTenant = (id) => api.delete(`/tenants/${id}`)

export default api
