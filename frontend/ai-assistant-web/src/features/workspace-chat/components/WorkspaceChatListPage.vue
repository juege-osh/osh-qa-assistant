<template>
  <div class="page-shell workspace-chat-list-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <el-button text class="workspace-btn workspace-btn--text" @click="router.push('/workspace/app/manage')">返回应用</el-button>
        <span class="workspace-context-note">当前会话按应用独立保存，可随时切换应用继续测试。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">总会话 {{ totalChatCount }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">筛选后 {{ filteredChatCount }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">最近更新 {{ latestUpdateLabel }}</span>
      </div>
    </section>

    <section class="workspace-section-card session-panel">
      <div class="workspace-overview-head panel-header">
        <div>
          <div class="panel-title-row">
            <div class="panel-title panel-title--lg">会话管理</div>
            <span class="workspace-status-pill workspace-status-pill--active">最近更新：{{ latestUpdateLabel }}</span>
          </div>
          <div class="panel-desc workspace-panel-desc">按会话名称快速筛选，直接继续历史聊天，或先把关键会话整理命名。</div>
        </div>
        <div class="session-head-actions">
          <el-input v-model="sessionKeyword" class="session-search-input" placeholder="搜索会话名称" size="large" clearable>
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="addChat">
            <el-icon><Plus /></el-icon>
            新开聊天
          </el-button>
        </div>
      </div>

      <div class="session-grid workspace-selection-list workspace-selection-list--scroll space-scrollbar space-scrollbar--strong">
        <div v-for="session in filteredChats" :key="session.id" class="session-item workspace-selection-card" @click="openChat(session.id)">
          <div class="workspace-selection-card__main">
            <div class="session-name workspace-selection-card__title">{{ session.chatName }}</div>
            <div class="workspace-selection-card__meta">
              <span>会话 ID：{{ session.id }}</span>
              <span v-if="formatTime(session.createTime || '')">创建：{{ formatTime(session.createTime || '') }}</span>
            </div>
          </div>
          <div class="session-actions workspace-selection-card__actions">
            <el-button text class="workspace-icon-btn" @click.stop="openRenameDialog(session)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button text type="danger" class="workspace-icon-btn workspace-icon-btn--danger" @click.stop="deleteChat(session.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <div v-if="!pageData.chats.length" class="workspace-empty-state">
          <div class="workspace-empty-panel session-empty-panel">
            <div class="workspace-empty-title">还没有历史会话</div>
            <div class="workspace-empty-desc">先新开一轮聊天，后续就可以在这里继续追问、回看结果和整理命名。</div>
            <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="addChat">创建第一个会话</el-button>
          </div>
        </div>
        <div v-else-if="!filteredChats.length" class="workspace-empty-state">
          <div class="workspace-empty-panel session-empty-panel">
            <div class="workspace-empty-title">没有找到匹配的会话</div>
            <div class="workspace-empty-desc">可以换一个关键词，或者直接清空筛选后回到完整会话列表。</div>
            <el-button class="workspace-btn workspace-btn--ghost" @click="sessionKeyword = ''">清空搜索</el-button>
          </div>
        </div>
      </div>
    </section>

    <el-dialog v-model="renameDialogVisible" class="workspace-form-dialog rename-session-dialog" title="重命名会话" width="520px">
      <div class="dialog-intro">给这轮会话一个更容易识别的名字，方便后续继续追问或回看。</div>
      <section class="workspace-info-card workspace-info-card--flush">
        <div class="workspace-info-grid workspace-info-grid--compact">
          <div class="workspace-info-item">
            <div class="workspace-info-label">当前会话</div>
            <div class="workspace-info-value">{{ renameOriginName || '未命名会话' }}</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">会话 ID</div>
            <div class="workspace-info-value workspace-info-value--mono">{{ renameChatId || '--' }}</div>
          </div>
        </div>
      </section>
      <el-form label-position="top">
        <el-form-item label="会话名称">
          <el-input v-model="renameChatName" maxlength="40" show-word-limit placeholder="请输入会话名称" />
          <div class="field-help">建议直接写主题或问题背景，后续回看会更快定位上下文。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="workspace-dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="renameDialogVisible = false">取消</el-button>
          <el-button class="workspace-btn workspace-btn--primary" type="primary" @click="confirmRename">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { useWorkspaceChatListFeature } from '../composables/useWorkspaceChatListFeature'

const {
  router,
  sessionKeyword,
  renameDialogVisible,
  renameChatName,
  renameChatId,
  renameOriginName,
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

const totalChatCount = computed(() => pageData.chats.length)
const filteredChatCount = computed(() => filteredChats.value.length)
const latestUpdateLabel = computed(() => latestUpdateTime.value || '暂无')
</script>

<style scoped>
.workspace-chat-list-page {
  gap: 16px;
}

.session-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.session-head-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex: 0 1 540px;
  min-width: 0;
}

.session-search-input {
  flex: 1 1 320px;
  min-width: 220px;
}

.panel-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.session-item {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
}

.session-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: minmax(132px, auto);
  align-content: start;
  min-height: 420px;
  max-height: 62vh;
  padding-right: 2px;
}

.session-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 0;
}

.session-item {
  min-height: 132px;
}

.session-grid .workspace-empty-state {
  grid-column: 1 / -1;
  margin-top: 0;
}

.workspace-empty-state {
  margin-top: 10px;
}

.session-empty-panel {
  display: flex;
  width: 100%;
  max-width: 420px;
  flex-direction: column;
  gap: 10px;
  text-align: center;
}

@media (max-width: 768px) {
  .panel-header,
  .session-item,
  .session-head-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .panel-title-row {
    align-items: flex-start;
  }

  .session-search-input {
    width: 100%;
  }

  .session-grid {
    grid-template-columns: 1fr;
    max-height: none;
  }

  .session-actions {
    justify-content: flex-end;
  }
}

@media (max-width: 1200px) and (min-width: 769px) {
  .session-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
