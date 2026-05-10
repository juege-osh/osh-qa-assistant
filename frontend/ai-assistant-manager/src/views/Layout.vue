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
  padding: 0 22px 0 0;
  color: var(--space-text);
  background: rgba(5, 12, 32, 0.72);
  border-bottom: 1px solid var(--space-border);
  backdrop-filter: blur(18px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.26);
}

.layout,
.outer-container {
  height: 100%;
}

.layout {
  position: relative;
  overflow: hidden;
}

.layout::before {
  content: "";
  position: absolute;
  width: 420px;
  height: 420px;
  right: -120px;
  top: 72px;
  border-radius: 50%;
  background: radial-gradient(circle at 32% 28%, rgba(255, 255, 255, 0.95), rgba(100, 232, 255, 0.34) 20%, rgba(139, 92, 246, 0.16) 48%, transparent 70%);
  filter: blur(1px);
  opacity: 0.36;
  pointer-events: none;
}

.el-aside {
  background: rgba(6, 13, 35, 0.82);
  border-right: 1px solid var(--space-border);
  backdrop-filter: blur(18px);
  box-shadow: 18px 0 50px rgba(0, 0, 0, 0.22);
}

.el-main {
  position: relative;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  overflow: auto;
}

.page-content {
  flex: 1;
}

:deep(.page-content > *) {
  min-height: calc(100vh - 126px);
}
</style>
