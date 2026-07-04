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
        <span class="workspace-inline-tag workspace-inline-tag--soft">最近更新 {{ latestUpdateTime }}</span>
      </div>
    </section>

    <section class="workspace-section-card session-panel">
      <div class="workspace-overview-head panel-header">
        <div>
          <div class="panel-title-row">
            <div class="panel-title panel-title--lg">会话管理</div>
            <span class="workspace-status-pill workspace-status-pill--active">最近更新：{{ latestUpdateTime }}</span>
          </div>
          <div class="panel-desc workspace-panel-desc">按会话名称快速筛选，直接继续历史聊天，或先把关键会话整理命名。</div>
        </div>
        <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="addChat">
          <el-icon><Plus /></el-icon>
          新开聊天
        </el-button>
      </div>

      <section class="workspace-section-card session-focus-panel">
        <div class="workspace-overview-head">
          <div>
            <div class="workspace-section-title workspace-section-title--md">当前关注点</div>
            <div class="workspace-panel-desc">先确认当前会话规模和筛选范围，再决定是继续历史对话、整理命名，还是直接新开一轮测试。</div>
          </div>
        </div>
        <div class="workspace-tip-grid session-tip-grid">
          <article
            v-for="item in focusCards"
            :key="item.title"
            :class="['workspace-tip-card', 'session-tip-card', item.tone ? `session-tip-card--${item.tone}` : '']"
          >
            <div class="session-tip-card__head">
              <span :class="['session-tip-card__dot', item.tone ? `session-tip-card__dot--${item.tone}` : '']"></span>
              <div class="workspace-tip-card__title">{{ item.title }}</div>
            </div>
            <div class="workspace-tip-card__desc">{{ item.desc }}</div>
          </article>
        </div>
      </section>

      <div class="session-search-panel workspace-filter-panel">
        <div class="session-search-copy workspace-section-copy workspace-filter-panel__copy">
          <div class="session-search-title workspace-section-title">快速筛选</div>
          <div class="session-search-note workspace-section-desc">{{ filterSummary }}</div>
        </div>
        <el-input v-model="sessionKeyword" class="workspace-filter-panel__control" placeholder="搜索会话名称" size="large" clearable>
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="session-list-head workspace-overview-head">
        <div>
          <div class="workspace-section-title workspace-section-title--md">会话列表</div>
          <div class="workspace-panel-desc">{{ listSummary }}</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--soft">展示 {{ filteredChatCount }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">全部 {{ totalChatCount }}</span>
        </div>
      </div>

      <div class="session-grid workspace-selection-list workspace-selection-list--scroll space-scrollbar space-scrollbar--strong space-scrollbar--float-end">
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
const hasKeyword = computed(() => Boolean(sessionKeyword.value.trim()))
const filterSummary = computed(() => {
  if (!pageData.chats.length) {
    return '当前还没有会话，创建后会自动进入列表。'
  }
  if (!hasKeyword.value) {
    return `共 ${totalChatCount.value} 个会话，搜索结果会实时更新。`
  }
  return `关键词“${sessionKeyword.value.trim()}”命中 ${filteredChatCount.value} 个会话。`
})
const listSummary = computed(() => {
  if (!pageData.chats.length) {
    return '创建会话后会自动进入对话页，后续可以回到这里继续追问或整理命名。'
  }
  if (hasKeyword.value) {
    return `当前按“${sessionKeyword.value.trim()}”缩小到 ${filteredChatCount.value} 个会话，适合直接定位某个主题继续追问。`
  }
  return '按更新时间继续历史对话，也可以先把关键会话重命名，方便后续回看和整理。'
})
const focusCards = computed(() => {
  return [
    {
      title: totalChatCount.value ? `当前已沉淀 ${totalChatCount.value} 个会话` : '当前还没有历史会话',
      desc: totalChatCount.value
        ? '如果要回看某轮测试，建议先按主题命名，再继续追问或导出整理。'
        : '建议先发起一轮真实问题测试，后续会自动沉淀到这里，便于继续跟进。',
      tone: totalChatCount.value ? 'success' : 'warning'
    },
    {
      title: hasKeyword.value ? '当前正在按关键词缩小范围' : '当前展示全部会话',
      desc: hasKeyword.value
        ? `关键词“${sessionKeyword.value.trim()}”当前命中 ${filteredChatCount.value} 个会话，适合快速定位某个主题或业务场景。`
        : '如果会话变多，可以直接按主题、问题名或业务场景搜索，减少来回翻找。',
      tone: hasKeyword.value ? 'warning' : ''
    },
    {
      title: pageData.chats.length ? '建议先继续最近会话或整理命名' : '建议先创建第一轮会话',
      desc: pageData.chats.length
        ? `最近更新于 ${latestUpdateTime.value}，可以继续历史对话，也可以先把关键会话重命名，方便后续回看。`
        : '创建后会直接进入对话页，你可以马上开始测试回答质量、上下文衔接和流式返回。',
      tone: pageData.chats.length ? 'success' : 'warning'
    }
  ] as const
})
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

.session-focus-panel {
  padding: 18px 20px;
}

.session-tip-grid {
  margin-top: 14px;
}

.session-list-head {
  align-items: center;
  gap: 16px;
}

.session-tip-card {
  position: relative;
  overflow: hidden;
}

.session-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.session-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.session-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.session-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.78);
  flex-shrink: 0;
}

.session-tip-card__dot--success {
  background: #12b76a;
}

.session-tip-card__dot--warning {
  background: #f59e0b;
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

.session-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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
  .session-list-head,
  .session-item {
    flex-direction: column;
    align-items: stretch;
  }

  .session-tip-grid {
    grid-template-columns: 1fr;
  }

  .panel-title-row {
    align-items: flex-start;
  }

  .session-actions {
    justify-content: flex-end;
  }
}
</style>
