<template>
  <div :class="['layout', isPublicRoute ? 'layout--public' : '']">
    <section class="wrapper">
      <section :class="['main-content', isPublicRoute ? 'main-content--public' : '']">
        <!-- 除了顶部导航所占区域在这里统一定义 -->
        <div :class="['content-box', isPublicRoute ? 'content-box--public' : '']">
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
import { computed } from 'vue'
import { useRoute } from 'vue-router';
let currentRoute = useRoute()
const isPublicRoute = computed(() => currentRoute.path.startsWith('/public/app/'))

</script>
<style scoped>
.content-box {
  height: 100%;
  padding: 14px;
  border: 1px solid rgba(227, 232, 241, 0.76);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px);
  box-shadow: 0 20px 48px rgba(37, 48, 71, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.content-box--public {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  backdrop-filter: none;
  box-shadow: none;
  overflow: auto;
}

.layout {
  height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.layout--public {
  background: transparent;
}

.wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.main-content {
  flex: 1;
  padding: 10px 10px 12px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.main-content--public {
  padding: 0;
}

</style>
