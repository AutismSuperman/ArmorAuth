<template>
  <div class="main-page this-page">
    <div class="top-tips">
      欢迎使用 ArmorAuth
    </div>

    <div class="summary-container">
      <a-card :bordered="false">
        <template #cover>
          <img alt="example" :src="img01"/>
        </template>
        <a-card-meta :title="stats.applications + '个'" style="text-align: center">
          <template #description>已添加应用数量</template>
        </a-card-meta>
      </a-card>
      <a-card :bordered="false">
        <template #cover>
          <img alt="example" :src="img04"/>
        </template>
        <a-card-meta :title="stats.providers + '个'" style="text-align: center">
          <template #description>已配置身份源数量</template>
        </a-card-meta>
      </a-card>
      <a-card :bordered="false">
        <template #cover>
          <img alt="example" :src="img03"/>
        </template>
        <a-card-meta :title="stats.users + '个'" style="text-align: center">
          <template #description>平台用户总数</template>
        </a-card-meta>
      </a-card>
      <a-card :bordered="false">
        <template #cover>
          <img alt="example" :src="img02"/>
        </template>
        <a-card-meta :title="stats.orgs + '个'" style="text-align: center">
          <template #description>组织数量</template>
        </a-card-meta>
      </a-card>
    </div>

    <h1 class="title">
      <span class="line"></span>快捷导航
    </h1>
    <div class="router-container">
      <div class="gutter-row" v-for="item in routers" :key="item.title" @click="$router.push(item.path)">
        <component :is="item.icon" style="font-size: 28px; color: #1890ff" />
        <span>{{ item.title }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import img01 from '../../assets/01.svg'
import img02 from '../../assets/02.svg'
import img03 from '../../assets/03.svg'
import img04 from '../../assets/04.svg'
import { reactive, onMounted } from 'vue'
import { AppstoreFilled, UserOutlined, TeamOutlined, CloudOutlined, FileTextOutlined, BarChartOutlined } from '@ant-design/icons-vue'
import { getApplications, getUsers, getOrganizations, getIdentityProviders } from '../../api'

const stats = reactive({ applications: 0, providers: 0, users: 0, orgs: 0 })

const routers = [
  { title: '应用管理', icon: AppstoreFilled, path: '/main/applications' },
  { title: '用户管理', icon: UserOutlined, path: '/main/users' },
  { title: '组织管理', icon: TeamOutlined, path: '/main/organizations' },
  { title: '身份源管理', icon: CloudOutlined, path: '/main/identityProviders' },
  { title: '审计日志', icon: FileTextOutlined, path: '/main/audit' },
  { title: '监控管理', icon: BarChartOutlined, path: '/main/monitor' }
]

onMounted(async () => {
  try {
    stats.applications = (await getApplications(0, 1)).data?.totalElements || 0
    stats.users = (await getUsers(0, 1)).data?.totalElements || 0
    stats.orgs = (await getOrganizations(0, 1)).data?.totalElements || 0
    stats.providers = (await getIdentityProviders(0, 1)).data?.totalElements || 0
  } catch (e) { /* ignore */ }
})
</script>

<style scoped lang="scss">
.top-tips {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 170px;
  width: calc(100% - 40px);
  margin-top: 50px;
  padding: 10px 0;
  background-image: linear-gradient(to right, rgb(59, 197, 124), rgb(30, 183, 109));
  border-radius: 20px;
  font-size: 32px;
  font-weight: bold;
  color: #fff;
}

.this-page {
  flex-direction: column;

  .title {
    width: 100%;
    height: 30px;
    font-weight: bold !important;
    font-size: 18px;
    margin: 20px 0;
    display: flex;
    align-items: center;

    .line {
      width: 4px;
      height: 20px;
      background-color: #1890ff;
      margin-right: 10px;
    }
  }
}

.summary-container {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  grid-gap: 30px;
  align-items: center;
  width: calc(100% - 40px);
  margin-top: 40px;
}

.router-container {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr 1fr;
  grid-gap: 20px;
  align-items: center;
  width: calc(100% - 40px);
}

.gutter-row {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 130px;
  width: 100%;
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  cursor: pointer;

  &:hover {
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
    transform: translateY(-2px);
  }

  span {
    margin-top: 20px;
    font-size: 14px;
    font-weight: bold;
  }
}
</style>
