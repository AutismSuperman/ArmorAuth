<script setup lang="ts">
import { CopyrightCircleOutlined } from '@ant-design/icons-vue';
import { shallowRef } from 'vue';
defineProps<{
  copyright?: string;
}>();

const prefixCls = shallowRef('ant-pro-global-footer');
</script>

<template>
  <div :class="prefixCls">
    <div v-if="$slots.renderFooterLinks" :class="`${prefixCls}-links`">
      <slot name="renderFooterLinks" />
    </div>
    <div v-if="copyright" :class="`${prefixCls}-copyright`"><CopyrightCircleOutlined /> {{ copyright }}</div>
  </div>
</template>

<style lang="less">
.ant-pro-global-footer {
  // margin: 48px 0 24px;
  padding: 0 16px;
  text-align: center;

  &-links {
    margin-bottom: 8px;

    a {
      color: rgba(0, 0, 0, 0.45);
      transition: all 0.3s;

      &:not(:last-child) {
        margin-right: 40px;
      }

      &:hover {
        color: rgba(0, 0, 0, 0.85);
      }
    }
  }

  &-copyright {
    color: rgba(0, 0, 0, 0.45);
    font-size: 14px;
  }
}

[data-theme='dark'] .ant-pro-global-footer {
  &-links {
    a {
      color: rgba(229, 224, 216, 0.45);

      &:hover {
        color: rgba(229, 224, 216, 0.85);
      }
    }
  }

  &-copyright {
    color: rgba(229, 224, 216, 0.45);
  }
}
</style>
