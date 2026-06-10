<template>
  <div class="layout">
    <section class="wrapper">
      <section>
        <nav-header />
      </section>
      <section class="main-content">
        <!-- 除了顶部导航所占区域在这里统一定义 -->
        <div class="content-box">
          <!-- empty-values指定空值是什么,默认['', null, undefined],空值无法选中 -->
          <el-config-provider :value-on-clear="null" :empty-values="[undefined, null]">
            <router-view :key="currentRoute.fullPath"></router-view>
          </el-config-provider>
        </div>
      </section>
    </section>
  </div>
</template>
<script setup name='Layout' lang='ts'>
import NavHeader from '@/components/NavHeader.vue';
import { ref } from 'vue'
import { useRoute } from 'vue-router';
let currentRoute = useRoute()

</script>
<style scoped>
.content-box {
  height: 100%;
  min-height: 100%;
  padding: 14px;
  border: 1px solid rgba(227, 232, 241, 0.76);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px);
  box-shadow: 0 20px 48px rgba(37, 48, 71, 0.08);
  overflow: hidden;
}

.layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 10px 10px 12px;
}

</style>
