<template>
  <div class="main-page this-page">
    <div class="top-tips">
      欢迎使用ArmorAuth
    </div>

    <div class="summary-container">
      <a-card :bordered="false">
        <template #cover>
          <img alt="example" :src="img01"/>
        </template>
        <a-card-meta title="5个" style="text-align: center">
          <template #description>已添加应用池数量</template>
        </a-card-meta>
      </a-card>
      <a-card :bordered="false">
        <template #cover>
          <img alt="example" :src="img04"/>
        </template>
        <a-card-meta title="5个" style="text-align: center">
          <template #description>已添加第三方登陆数量</template>
        </a-card-meta>
      </a-card>
      <a-card :bordered="false">
        <template #cover>
          <img alt="example" :src="img03"/>
        </template>
        <a-card-meta title="5个" style="text-align: center">
          <template #description>平台用户总数</template>
        </a-card-meta>
      </a-card>
      <a-card :bordered="false">
        <template #cover>
          <img alt="example" :src="img02"/>
        </template>
        <a-card-meta title="5个" style="text-align: center">
          <template #description>今日授权登录次数</template>
        </a-card-meta>
      </a-card>
    </div>

    <h1 class="title">
      <span class="line"></span>快捷导航
    </h1>
    <div class="router-container">
      <div class="gutter-row" v-for="item in routers" :key="item.title">
        <icon-font :type="item.icon" style="font-size: 24px"/>
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
import {createFromIconfontCN} from '@ant-design/icons-vue';

const IconFont = createFromIconfontCN({
  scriptUrl: 'https://at.alicdn.com/t/c/font_4201298_575fj6emtmp.js?spm=a313x.manage_type_myprojects.1998910419.54.27bf3a81dFngLa&file=font_4201298_575fj6emtmp.js',
});

const routers = [
  {
    title: '官方文档',
    icon: 'icon-document',
    target: ''
  },
  {
    title: 'Spring技术支持',
    icon: 'icon-spring',
    target: ''
  },
  {
    title: '官方Github仓库',
    icon: 'icon-github',
    target: ''
  },
  {
    title: '官方码云仓库',
    icon: 'icon-gitee',
    target: ''
  }, {
    title: '开源中国主页',
    icon: 'icon-oschina',
    target: ''
  },
  {
    title: '官方技术博客',
    icon: 'icon-csdn',
    target: ''
  }
]

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
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr;
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
  }

  span {
    margin-top: 20px;
    font-size: 16px;
    font-weight: bold;
  }
}
</style>