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
.app-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.app-info-item {
  padding: 14px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #eef1f5;
}

.app-info-item--full {
  grid-column: 1 / -1;
}

.app-info-label {
  display: block;
  color: #111827;
  font-size: 13px;
  font-weight: 700;
}

.app-info-value {
  display: block;
  margin-top: 8px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.65;
}

@media (max-width: 900px) {
  .app-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
