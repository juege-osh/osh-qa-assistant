<template>
  <div :class="['layout', isPublicRoute ? 'layout--public' : '', isDashboardRoute ? 'layout--dashboard' : '']">
    <section class="wrapper">
      <section
        :class="[
          'main-content',
          isPublicRoute ? 'main-content--public' : '',
          isDashboardRoute ? 'main-content--dashboard' : ''
        ]"
      >
        <!-- 除了顶部导航所占区域在这里统一定义 -->
        <div
          :class="[
            'content-box',
            isPublicRoute ? 'content-box--public' : '',
            isDashboardRoute ? 'content-box--dashboard' : ''
          ]"
        >
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
const isDashboardRoute = computed(() => currentRoute.path.startsWith('/workspace') || currentRoute.path.startsWith('/admin'))

</script>
<style scoped>
.content-box {
  height: 100%;
  padding: 14px;
  border: 1px solid var(--space-border);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 251, 255, 0.94));
  box-shadow: var(--space-card-shadow);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.content-box--dashboard,
.content-box--public {
  position: relative;
  padding: 0;
  backdrop-filter: none;
  overflow: hidden;
}

.content-box--dashboard {
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.content-box--public {
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
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

.layout--dashboard {
  background:
    radial-gradient(circle at top left, rgba(210, 221, 244, 0.28), transparent 18%),
    radial-gradient(circle at 88% 12%, rgba(229, 236, 248, 0.22), transparent 18%),
    radial-gradient(circle at bottom right, rgba(233, 239, 248, 0.26), transparent 20%),
    linear-gradient(180deg, rgba(250, 252, 255, 0.94), rgba(246, 249, 252, 0.98));
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

.main-content--dashboard {
  padding: 20px 22px 24px;
}

@media (max-width: 900px) {
  .main-content--dashboard {
    padding: 12px;
  }
}

</style>
