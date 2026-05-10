<template>
  <div class="layout">
    <section class="wrapper">
      <section>
        <nav-header />
      </section>
      <section class="main-content">
        <!-- 除了顶部导航所占区域在这里统一定义 -->
        <el-row>
          <el-col :span="20" :offset="2" class="content-box">
            <!-- empty-values指定空值是什么,默认['', null, undefined],空值无法选中 -->
            <el-config-provider :value-on-clear="null" :empty-values="[undefined, null]">
              <router-view :key="currentRoute.fullPath"></router-view>
            </el-config-provider>
          </el-col>
        </el-row>
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
  padding: 18px;
  border: 1px solid var(--space-border);
  border-radius: 26px;
  background: rgba(8, 15, 38, 0.62);
  box-shadow: var(--space-shadow);
  backdrop-filter: blur(18px);
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
  padding: 22px 0 18px;
}

.main-content .el-row {
  height: 100%;
}

</style>
