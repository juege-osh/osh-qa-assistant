<template>
  <main ref="chatPanelRef" class="chat-panel">
    <section v-if="loadError" class="public-panel-state public-panel-state--error">
      <div class="public-panel-state__title">当前公开链接不可用</div>
      <div class="public-panel-state__desc">{{ loadError }}</div>
      <div class="panel-state-actions">
        <el-button type="primary" @click="loadDetail">重试</el-button>
      </div>
    </section>

    <template v-else>
      <section v-if="needsPasswordAuth" class="public-auth-banner">
        <div class="auth-banner-copy">
          <div class="public-auth-banner__title">这个公开应用需要访问密码</div>
          <div class="public-auth-banner__desc">验证通过后即可在当前页面继续使用历史会话并发起新提问。</div>
        </div>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          class="auth-form"
          label-position="top"
        >
          <el-form-item label="访问密码" prop="accessPassword">
            <el-input
              v-model="passwordForm.accessPassword"
              type="password"
              show-password
              clearable
              placeholder="请输入访问密码"
              @keydown.enter.prevent="verifyPassword"
            ></el-input>
          </el-form-item>
          <div class="auth-form-actions">
            <el-button type="primary" :loading="passwordLoading" @click="verifyPassword">验证并继续</el-button>
          </div>
        </el-form>
      </section>

      <div class="chat-topbar">
        <div class="chat-topbar-inner">
          <div class="composer-app-brief composer-app-brief--top">
            <el-avatar :src="iconUrl" :size="28"></el-avatar>
            <div class="composer-app-brief-copy">
              <div class="composer-app-brief-title">{{ detail.appName || '公开应用' }}</div>
              <div class="composer-app-brief-meta">
                <span class="composer-app-chip">{{ detail.slug || routeSlug || '未发布' }}</span>
                <span class="composer-app-chip">{{ accessModeLabel }}</span>
              </div>
            </div>
          </div>

          <div class="chat-topbar-actions">
            <el-button class="topbar-action-btn" @click="showAppInfoDialog = true">应用信息</el-button>
            <el-button class="topbar-action-btn" @click="jumpToWorkspace">进入工作台</el-button>
          </div>
        </div>
      </div>

      <section ref="chatBoxRef" class="public-chat-stream space-scrollbar">
        <div v-if="!activeSessionMessageCount" class="chat-welcome">
          <div class="chat-empty">
            <div class="chat-empty-title">直接输入问题开始对话</div>
          </div>

          <div class="public-welcome-grid public-welcome-grid--compact">
            <button
              v-for="prompt in suggestedPrompts.slice(0, 4)"
              :key="prompt"
              type="button"
              class="public-welcome-card"
              @click="usePrompt(prompt)"
            >
              {{ prompt }}
            </button>
          </div>
        </div>

        <template v-else>
          <div
            v-for="message in activeSessionMessages"
            :key="message.id"
            :class="[
              'public-chat-message',
              message.role === 'user' ? 'public-chat-message--user' : 'public-chat-message--assistant'
            ]"
          >
            <div class="public-chat-message__avatar">{{ message.role === 'user' ? '你' : 'AI' }}</div>

            <div class="public-chat-message__body">
              <div class="public-chat-message__bubble">
                <div v-if="message.role === 'assistant'" class="public-chat-message__meta">
                  <div class="public-chat-message__label">{{ detail.appName || '公开应用' }}</div>
                  <span v-if="isStreamingMessage(message)" class="public-chat-message__stream-status">生成中</span>
                </div>
                <div
                  v-if="message.role === 'assistant' && isStreamingMessage(message)"
                  class="public-chat-message__content public-chat-message__content--streaming"
                >
                  <span v-if="getStreamingMessageText(message)">{{ getStreamingMessageText(message) }}</span>
                  <span v-else class="public-chat-message__stream-placeholder">正在整理回答</span>
                  <span class="public-chat-message__stream-caret"></span>
                </div>
                <div
                  v-else-if="message.role === 'assistant'"
                  class="public-chat-message__content"
                  v-html="renderMessage(message)"
                ></div>
                <div v-else class="public-chat-message__user-content">{{ message.content }}</div>
              </div>
              <div class="public-chat-message__time">{{ formatMessageTime(message.createdAt) }}</div>
            </div>
          </div>
        </template>
      </section>

      <div class="composer-bar">
        <div class="composer-shell">
          <el-input
            v-model="userInput"
            type="textarea"
            :rows="3"
            :disabled="sending || !!loadError"
            placeholder="输入业务问题、场景或文档中的关键词"
            @keydown="handleEditorKeydown"
          ></el-input>

          <div class="composer-actions">
            <div class="composer-btn-group">
              <span class="public-status-pill" :class="{ 'public-status-pill--running': sending }">
                {{ sending ? '正在生成' : '准备就绪' }}
              </span>
              <span class="public-status-pill">当前会话 {{ activeSessionMessageCount }} 条消息</span>
              <el-button :disabled="interactionLocked || !userInput" @click="userInput = ''">清空</el-button>
              <el-button type="primary" :loading="sending" :disabled="sendDisabled" @click="sendMessage">
                发送
              </el-button>
            </div>
          </div>
        </div>

        <div class="composer-footer-note">内容由 AI 生成，仅供参考，请结合实际情况判断。Enter 发送，Shift + Enter 换行</div>
      </div>
    </template>
  </main>
</template>

<script setup lang="ts">
import { usePublicAppFeatureModel } from '../composables/usePublicAppFeature'

const model = usePublicAppFeatureModel()
const {
  chatPanelRef,
  chatBoxRef,
  passwordFormRef,
  showAppInfoDialog,
  detail,
  passwordForm,
  passwordRules,
  userInput,
  sending,
  passwordLoading,
  loadError,
  suggestedPrompts,
  routeSlug,
  iconUrl,
  accessModeLabel,
  activeSessionMessages,
  activeSessionMessageCount,
  needsPasswordAuth,
  interactionLocked,
  sendDisabled,
  formatMessageTime,
  isStreamingMessage,
  getStreamingMessageText,
  renderMessage,
  jumpToWorkspace,
  usePrompt,
  loadDetail,
  verifyPassword,
  sendMessage,
  handleEditorKeydown
} = model
</script>

<style scoped>
.chat-panel {
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--public-border);
  border-radius: var(--public-radius-panel);
  background: var(--public-panel-strong);
  box-shadow: var(--public-shadow-soft);
}

.panel-state-actions,
.composer-btn-group,
.auth-form-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.auth-form {
  min-width: 0;
}

.chat-topbar {
  padding: 0 24px;
  background:
    radial-gradient(circle at top left, rgba(99, 91, 255, 0.04), transparent 28%),
    linear-gradient(180deg, var(--public-panel-muted) 0%, rgba(249, 251, 255, 0.96) 100%);
  box-shadow: inset 0 -1px 0 rgba(230, 235, 242, 0.92);
}

.chat-topbar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  min-height: 72px;
  padding: 14px 0;
}

.chat-topbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.chat-welcome {
  width: 100%;
  padding: 8px 0 16px;
}

.chat-empty {
  padding: 8px 0 16px;
}

.chat-empty-title {
  color: var(--public-text);
  font-size: 20px;
  line-height: 1.3;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.composer-bar {
  padding: 0 24px 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, rgba(255, 255, 255, 0.95) 16%, #ffffff 100%);
}

.composer-shell {
  padding: 14px 16px;
  border: 1px solid var(--public-border);
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  box-shadow: var(--public-shadow-soft);
}

.composer-shell :deep(.el-textarea__inner) {
  min-height: 56px !important;
  border-radius: 14px !important;
  padding: 6px 4px;
  font-size: 15px;
  line-height: 1.6;
  box-shadow: none !important;
  border: 0 !important;
  background: transparent !important;
}

.composer-actions {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 8px;
}

.composer-app-brief {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.composer-app-brief--top {
  flex: 1;
  min-height: 0;
  padding-bottom: 0;
  border-bottom: 0;
}

.composer-app-brief :deep(.el-avatar) {
  flex-shrink: 0;
  border: 1px solid var(--public-border);
  background: #ffffff;
}

.composer-app-brief-copy {
  min-width: 0;
}

.composer-app-brief-title {
  color: var(--public-text);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.2;
}

.composer-app-brief-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.composer-app-chip {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border: 1px solid var(--public-border);
  border-radius: 999px;
  color: var(--public-text-muted);
  font-size: 11px;
  background: var(--public-panel-muted);
}

.chat-topbar-actions :deep(.el-button) {
  min-width: 92px;
  height: 34px;
  padding: 0 14px;
  border-radius: 12px;
  border: 1px solid var(--public-border) !important;
  background: rgba(255, 255, 255, 0.78) !important;
  color: var(--public-text-soft) !important;
  box-shadow: none !important;
}

.chat-topbar-actions :deep(.el-button:hover) {
  border-color: rgba(99, 91, 255, 0.18) !important;
  background: #ffffff !important;
  color: var(--public-text) !important;
  box-shadow: var(--public-shadow-soft) !important;
}

.composer-btn-group :deep(.el-button) {
  min-width: 92px;
  height: 40px;
  border-radius: 12px;
}

.composer-btn-group :deep(.el-button--primary) {
  border-color: var(--public-accent) !important;
  background: linear-gradient(180deg, var(--public-accent) 0%, var(--public-accent-strong) 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 10px 18px rgba(99, 91, 255, 0.18) !important;
}

.composer-footer-note {
  margin-top: 10px;
  color: var(--public-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1180px) {
  .public-auth-banner {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .public-auth-banner {
    margin: 16px 18px 0;
  }

  .chat-topbar,
  .public-chat-stream {
    padding: 18px;
  }

  .chat-topbar {
    padding-top: 0;
    padding-bottom: 0;
  }

  .chat-topbar-inner {
    flex-direction: column;
    align-items: stretch;
  }

  .public-welcome-grid {
    grid-template-columns: 1fr;
  }

  .public-chat-message {
    margin-bottom: 20px;
  }

  .public-chat-message__body {
    max-width: 100%;
  }

  .composer-bar {
    padding: 0 18px 18px;
  }

  .composer-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .composer-app-brief,
  .composer-btn-group {
    width: 100%;
  }

  .composer-btn-group {
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .public-status-pill {
    height: 32px;
  }

  .public-chat-message {
    gap: 10px;
  }

  .public-chat-message__content,
  .public-chat-message__user-content {
    font-size: 14px;
    line-height: 1.84;
  }

  .chat-topbar-actions {
    width: 100%;
  }

  .chat-topbar-actions :deep(.el-button) {
    flex: 1;
  }

  .composer-btn-group {
    width: 100%;
    display: flex;
  }

  .composer-btn-group :deep(.el-button) {
    flex: 1;
  }
}
</style>
