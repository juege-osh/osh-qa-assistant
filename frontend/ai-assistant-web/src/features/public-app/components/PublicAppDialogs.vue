<template>
  <el-dialog v-model="showAppInfoDialog" title="应用信息" width="560px">
    <div class="app-info-grid">
      <div class="app-info-item">
        <span class="app-info-label">应用名称</span>
        <strong class="app-info-value">{{ detail.appName || '-' }}</strong>
      </div>
      <div class="app-info-item">
        <span class="app-info-label">访问方式</span>
        <strong class="app-info-value">{{ accessModeLabel }}</strong>
      </div>
      <div class="app-info-item app-info-item--full">
        <span class="app-info-label">应用描述</span>
        <strong class="app-info-value">{{ detail.appDesc || '未填写描述' }}</strong>
      </div>
      <div class="app-info-item app-info-item--full">
        <span class="app-info-label">历史记录策略</span>
        <strong class="app-info-value">当前公开页历史记录保存在本地浏览器，并按应用与访问身份隔离。</strong>
      </div>
    </div>
  </el-dialog>

  <el-dialog
    v-model="renameDialogVisible"
    title="重命名会话"
    width="420px"
    @closed="resetRenameDialogState"
  >
    <el-input
      v-model="renameDraft"
      maxlength="40"
      placeholder="请输入会话名称"
      @keydown.enter.prevent="confirmRenameSession"
    ></el-input>
    <template #footer>
      <el-button @click="renameDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmRenameSession">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { usePublicAppFeatureModel } from '../composables/usePublicAppFeature'

const model = usePublicAppFeatureModel()
const {
  showAppInfoDialog,
  renameDialogVisible,
  renameDraft,
  detail,
  accessModeLabel,
  resetRenameDialogState,
  confirmRenameSession
} = model
</script>

<style scoped>
:deep(.el-dialog) {
  border-radius: 18px;
  border: 1px solid var(--public-border, #e6ebf2);
  box-shadow: var(--public-shadow, 0 12px 32px rgba(15, 23, 42, 0.04));
}

:deep(.el-dialog__header) {
  margin-right: 0;
  padding: 18px 20px 12px;
  border-bottom: 1px solid var(--public-border, #e6ebf2);
}

:deep(.el-dialog__body) {
  padding: 18px 20px 20px;
}

:deep(.el-dialog__footer) {
  padding: 0 20px 18px;
}

.app-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.app-info-item {
  padding: 14px;
  border-radius: 14px;
  background: var(--public-panel-muted, #f9fbff);
  border: 1px solid var(--public-border, #e6ebf2);
}

.app-info-item--full {
  grid-column: 1 / -1;
}

.app-info-label {
  display: block;
  color: var(--public-text, #101828);
  font-size: 13px;
  font-weight: 700;
}

.app-info-value {
  display: block;
  margin-top: 8px;
  color: var(--public-text-muted, #667085);
  font-size: 14px;
  line-height: 1.65;
}

@media (max-width: 900px) {
  .app-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
