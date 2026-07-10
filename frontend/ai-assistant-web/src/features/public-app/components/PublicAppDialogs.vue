<template>
  <el-dialog v-model="showAppInfoDialog" title="应用信息" width="560px" class="public-app-dialog">
    <div class="public-dialog-copy">
      快速查看当前公开应用的核心信息，确认访问方式、标识和当前访问身份。
    </div>
    <section class="public-dialog-panel">
      <div class="app-info-grid">
        <div class="app-info-item">
          <span class="app-info-label">应用名称</span>
          <strong class="app-info-value">{{ detail.appName || '-' }}</strong>
        </div>
        <div class="app-info-item">
          <span class="app-info-label">访问标识</span>
          <strong class="app-info-value app-info-value--mono">{{ detail.slug || routeSlug || '-' }}</strong>
        </div>
        <div class="app-info-item">
          <span class="app-info-label">访问方式</span>
          <strong class="app-info-value">{{ accessModeLabel }}</strong>
        </div>
        <div class="app-info-item">
          <span class="app-info-label">当前身份</span>
          <strong class="app-info-value">{{ identityLabel }}</strong>
        </div>
        <div class="app-info-item app-info-item--full">
          <span class="app-info-label">应用描述</span>
          <strong class="app-info-value">{{ detail.appDesc || '未填写描述' }}</strong>
        </div>
      </div>
    </section>
    <div class="public-dialog-note">
      如果当前是公开访问场景，建议优先确认访问标识和应用描述是否足够清晰，方便后续分享和识别。
    </div>
  </el-dialog>

  <el-dialog
    v-model="renameDialogVisible"
    title="重命名会话"
    width="420px"
    class="public-app-dialog"
    @closed="resetRenameDialogState"
  >
    <div class="public-dialog-copy">给这段公开对话起一个更容易识别的名字，后续回看最近对话时会更方便。</div>
    <section class="public-dialog-panel">
      <div class="rename-dialog-info-grid">
        <div class="app-info-item">
          <span class="app-info-label">当前名称</span>
          <strong class="app-info-value">{{ renameOriginTitle || '未命名会话' }}</strong>
        </div>
        <div class="app-info-item">
          <span class="app-info-label">会话 ID</span>
          <strong class="app-info-value app-info-value--mono">{{ renameSessionId || '-' }}</strong>
        </div>
      </div>
      <div class="rename-dialog-panel">
        <el-input
          v-model="renameDraft"
          maxlength="40"
          placeholder="请输入会话名称"
          @keydown.enter.prevent="confirmRenameSession"
        ></el-input>
      </div>
      <div class="public-dialog-note public-dialog-note--compact">
        建议直接写主题、问题背景或业务对象，后续在最近对话里会更容易快速定位。
      </div>
    </section>
    <template #footer>
      <div class="public-dialog-footer">
        <el-button class="public-dialog-btn public-dialog-btn--ghost" @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" class="public-dialog-btn public-dialog-btn--primary" @click="confirmRenameSession">保存</el-button>
      </div>
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
  renameSessionId,
  renameOriginTitle,
  detail,
  routeSlug,
  accessModeLabel,
  identityLabel,
  resetRenameDialogState,
  confirmRenameSession
} = model
</script>

<style scoped>
:deep(.public-app-dialog .el-dialog) {
  border-radius: 18px;
  border: 1px solid var(--public-border, #e6ebf2);
  box-shadow: var(--public-shadow, 0 12px 32px rgba(15, 23, 42, 0.04));
}

:deep(.public-app-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 20px 20px 10px;
}

:deep(.public-app-dialog .el-dialog__body) {
  padding: 0 20px 20px;
}

:deep(.public-app-dialog .el-dialog__footer) {
  padding: 0 20px 20px;
}

:deep(.public-app-dialog .el-input__wrapper) {
  min-height: 44px;
  border-radius: 14px !important;
  border-color: var(--public-border, #e6ebf2) !important;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.94)) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82), 0 8px 18px rgba(15, 23, 42, 0.04) !important;
}

.public-dialog-copy {
  margin-bottom: 14px;
  color: var(--public-text-muted, #667085);
  font-size: 13px;
  line-height: 1.7;
}

.public-dialog-panel {
  padding: 14px;
  border: 1px solid var(--public-border, #e6ebf2);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(249, 251, 255, 0.95));
}

.rename-dialog-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.rename-dialog-panel {
  margin-top: 14px;
}

.app-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.app-info-item {
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(249, 251, 255, 0.96), rgba(255, 255, 255, 0.94));
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

.app-info-value--mono {
  font-family: "SFMono-Regular", "Menlo", "Monaco", "Consolas", "Liberation Mono", monospace;
  font-size: 13px;
  word-break: break-all;
}

.public-dialog-note {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(239, 241, 255, 0.72);
  color: var(--public-text-soft, #344054);
  font-size: 13px;
  line-height: 1.7;
}

.public-dialog-note--compact {
  margin-top: 12px;
  margin-bottom: 0;
}

.public-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.public-dialog-btn {
  min-height: 40px;
  padding: 0 18px;
  border-radius: 14px;
}

.public-dialog-btn--ghost {
  border-color: var(--public-border, #e6ebf2);
  background: rgba(255, 255, 255, 0.96);
  color: var(--public-text, #101828);
}

.public-dialog-btn--primary {
  border: none;
  background: linear-gradient(180deg, var(--public-accent, #635bff) 0%, var(--public-accent-strong, #4f46e5) 100%);
  box-shadow: 0 12px 24px rgba(99, 91, 255, 0.2);
}

@media (max-width: 900px) {
  .rename-dialog-info-grid,
  .app-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
