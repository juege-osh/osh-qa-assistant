<template>
  <section class="workspace-status-page">
    <div class="workspace-status-panel workspace-section-card">
      <el-result title="404" sub-title="这个页面暂时没有找到，可能已经移动、失效，或者当前地址不完整。">
        <template #extra>
          <div class="workspace-status-actions">
            <el-button class="workspace-btn workspace-btn--ghost" @click="router.replace('/')">返回首页</el-button>
            <el-button class="workspace-btn workspace-btn--primary" type="primary" @click="router.go(-1)">返回上一步</el-button>
          </div>
        </template>
      </el-result>
    </div>
  </section>
</template>
<script setup name='NotFound' lang='ts'>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>
<style scoped>
.workspace-status-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.workspace-status-panel {
  width: min(680px, 100%);
  padding: 18px;
}

.workspace-status-actions {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

@media (max-width: 640px) {
  .workspace-status-page {
    padding: 12px;
  }

  .workspace-status-actions {
    width: 100%;
  }
}
</style>
