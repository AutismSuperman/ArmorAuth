<template>
  <div class="not-found">
    <div class="not-found-content">
      <div class="not-found-code">404</div>
      <h2>页面未找到</h2>
      <p>你访问的页面不存在或已被移除</p>
      <a-button type="primary" @click="$router.push('/main/home')">
        返回首页
      </a-button>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped lang="scss">
.not-found {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--aa-bg);
}

.not-found-content {
  text-align: center;
}

.not-found-code {
  font-size: 120px;
  font-weight: 900;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 16px;
  letter-spacing: -0.04em;
}

h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--aa-text-primary);
  margin: 0 0 8px;
}

p {
  color: var(--aa-text-secondary);
  font-size: 14px;
  margin: 0 0 28px;
}
</style>
