<template>
  <a-layout class="main-layout">
    <a-layout-sider width="250" class="left-slider">
      <div class="logo-container">
        ArmorAuth
      </div>
      <a-menu v-model:selectedKeys="selectedKeys2" mode="inline" :items="menuArray"
              :style="{ height: '100%', borderRight: 0 }" @click="menuClick">
      </a-menu>
    </a-layout-sider>
    <a-layout style="width: calc(100% - 250px)">
      <a-layout-content class="content">
        <router-view/>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import {ref, reactive, h} from 'vue'
import {HomeFilled, AppstoreFilled, GoldenFilled, SettingFilled, BarChartOutlined, UserOutlined} from '@ant-design/icons-vue'
import {useRoute, useRouter} from 'vue-router'

const route = useRoute()
const router = useRouter()

const selectedKeys2 = ref(['/main/home'])

const menuArray = reactive([
  {
    key: '/main/home',
    icon: () => h(HomeFilled),
    label: '首页'
  },
  {
    key: '/main/applications',
    icon: () => h(AppstoreFilled),
    label: '应用管理'
  },
  {
    key: '/main/monitor',
    icon: () => h(BarChartOutlined),
    label: '监控管理'
  },
  {
    key: '/main/users',
    icon: () => h(UserOutlined),
    label: '用户管理'
  },
  {
    key: '/main/thirdPartLogin',
    icon: () => h(GoldenFilled),
    label: '第三方登陆'
  },
  {
    key: '/main/settings',
    icon: () => h(SettingFilled),
    label: '设置'
  }
])

const menuClick = data => {
  console.log(data.key)
  if (route.path !== data.key) {
    router.push(data.key)
  }
}
</script>

<style scoped lang="scss">
.main-layout {
  width: 100%;
  height: 100vh;
  display: flex;

  .logo-container {
    width: 100%;
    height: 100px;
    background: #f7f8fa;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 35px;
    text-shadow: -2px -2px 1px #000;
  }

  .content {
    background-color: #fff;
    margin: 0;
    height: 100%;
    width: 100%
  }
}

.left-slider {
  flex: 0 1 auto;
  background: #f7f8fa;
  width: 250px;
  min-width: 240px;
  border-bottom: 0;
  border-top: 0;
  height: calc(100% - 100px);
  overflow: hidden;
  box-sizing: border-box;
  position: relative;
  transition: all .2s cubic-bezier(.2, 0, 0, 1);
}

:deep(.ant-menu-light) {
  background-color: #f7f8fa;
}

.ant-menu:not(.ant-menu-horizontal) .ant-menu-item-selected {
  background-color: #e9effd;
  color: #215ae5;
}
</style>