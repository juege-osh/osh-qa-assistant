<template>
  <div class="page-shell workspace-chat-list-page">
    <section class="workspace-context-strip">
      <el-button text class="workspace-btn workspace-btn--text" @click="router.push('/workspace/app/manage')">返回应用</el-button>
      <span class="workspace-context-note">回到应用列表可以切换别的应用。</span>
    </section>

    <section class="workspace-section-card session-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">会话管理</div>
          <div class="panel-desc">共 {{ pageData.chats.length }} 个会话，最近更新：{{ latestUpdateTime }}</div>
        </div>
        <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="addChat">
          <el-icon><Plus /></el-icon>
          新开聊天
        </el-button>
      </div>

      <div class="session-search">
        <el-input v-model="sessionKeyword" placeholder="搜索会话名称" size="large" clearable>
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="session-grid">
        <div v-for="session in filteredChats" :key="session.id" class="session-item" @click="openChat(session.id)">
          <div class="session-body">
            <div class="session-name">{{ session.chatName }}</div>
            <div class="session-meta">
              <span>会话 ID：{{ session.id }}</span>
              <span v-if="formatTime(session.createTime || '')">创建：{{ formatTime(session.createTime || '') }}</span>
            </div>
          </div>
          <div class="session-actions">
            <el-button text class="workspace-icon-btn" @click.stop="openRenameDialog(session)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button text type="danger" class="workspace-icon-btn workspace-icon-btn--danger" @click.stop="deleteChat(session.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <div v-if="!pageData.chats.length" class="workspace-empty-state">
          <el-empty description="还没有聊天会话">
            <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="addChat">创建第一个会话</el-button>
          </el-empty>
        </div>
      </div>
    </section>

    <el-dialog v-model="renameDialogVisible" title="重命名会话" width="500px">
      <el-form>
        <el-form-item label="会话名称">
          <el-input v-model="renameChatName" placeholder="请输入会话名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { useWorkspaceChatListFeature } from '../composables/useWorkspaceChatListFeature'

const {
  router,
  sessionKeyword,
  renameDialogVisible,
  renameChatName,
  pageData,
  filteredChats,
  latestUpdateTime,
  formatTime,
  addChat,
  openChat,
  openRenameDialog,
  confirmRename,
  deleteChat
} = useWorkspaceChatListFeature()
</script>

<style scoped>
.workspace-chat-list-page {
  gap: 14px;
}

.session-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-header,
.session-item,
.session-actions,
.session-meta {
  display: flex;
}

.panel-header {
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-title {
  color: var(--space-text);
  font-size: 20px;
  font-weight: 800;
}

.panel-desc {
  margin-top: 6px;
  color: var(--space-text-soft);
  font-size: 13px;
}

.session-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.session-item {
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid rgba(227, 232, 241, 0.92);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.94));
  cursor: pointer;
  transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;
}

.session-item:hover {
  transform: translateY(-1px);
  border-color: rgba(64, 158, 255, 0.22);
  box-shadow: 0 18px 32px rgba(37, 48, 71, 0.08);
}

.session-body {
  flex: 1;
  min-width: 0;
}

.session-name {
  color: var(--space-text);
  font-size: 15px;
  font-weight: 700;
}

.session-meta {
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 8px;
  color: var(--space-muted);
  font-size: 12px;
}

.session-actions {
  gap: 8px;
  align-items: center;
}

.workspace-empty-state {
  margin-top: 10px;
}

@media (max-width: 768px) {
  .panel-header,
  .session-item {
    flex-direction: column;
    align-items: stretch;
  }

  .session-actions {
    justify-content: flex-end;
  }
}
</style>
