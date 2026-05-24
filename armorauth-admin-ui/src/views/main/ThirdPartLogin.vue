<template>
  <div class="page-container">
    <div class="page-header">
      <h2>第三方登录</h2>
    </div>
    <a-alert message="请前往「身份源管理」页面配置第三方登录身份源" type="info" show-icon>
      <template #action>
        <a-button size="small" type="primary" @click="$router.push('/main/identityProviders')">
          前往身份源管理
        </a-button>
      </template>
    </a-alert>
  </div>
</template>

<script setup>
</script>

<style scoped>
</style>
