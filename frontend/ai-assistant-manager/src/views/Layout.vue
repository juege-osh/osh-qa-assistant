<template>
  <div class="layout">
    <el-container class="outer-container">
      <el-header>
        <nav-header></nav-header>
      </el-header>
      <el-container>
        <el-aside :width="asideWidth">
          <side-bar @update:asideWidth="updateAsideWidth"></side-bar>
        </el-aside>
        <el-main>
          <tabs></tabs>
          <section class="page-content">
            <!-- empty-values指定空值是什么,默认['', null, undefined],空值无法选中 -->
            <el-config-provider :value-on-clear="null" :empty-values="[undefined, null]">
              <router-view :key="currentRoute.fullPath"></router-view>
            </el-config-provider>
          </section>
        </el-main>
      </el-container>
    </el-container>
    
  </div>
</template>
<script setup name='Layout' lang='ts'>
   import NavHeader from '@/components/NavHeader.vue';
   import SideBar from '@/components/SideBar.vue';
   import Tabs from '@/components/Tabs.vue';
   import {ref} from 'vue'
   import { useRoute } from 'vue-router';
   let currentRoute = useRoute()
   let asideWidth = ref('200px')

   // 更新aside的宽度
   function updateAsideWidth(width:string) {
    asideWidth.value = width
   }
</script>
<style scoped>
.el-header {
  height: 64px;
  padding: 0 14px 0 0;
  color: var(--space-text);
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1px solid rgba(227, 232, 241, 0.8);
  backdrop-filter: blur(14px);
  box-shadow: 0 10px 28px rgba(37, 48, 71, 0.05);
}

.layout,
.outer-container {
  height: 100%;
}

.layout {
  position: relative;
  overflow: hidden;
}

.el-aside {
  background: rgba(255, 255, 255, 0.54);
  border-right: 1px solid rgba(227, 232, 241, 0.72);
  backdrop-filter: blur(10px);
}

.el-main {
  position: relative;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
}

.page-content {
  flex: 1;
  padding: 10px;
  border: 1px solid rgba(227, 232, 241, 0.76);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px);
  box-shadow: 0 20px 48px rgba(37, 48, 71, 0.08);
}

:deep(.page-content > *) {
  min-height: calc(100vh - 110px);
}
</style>
