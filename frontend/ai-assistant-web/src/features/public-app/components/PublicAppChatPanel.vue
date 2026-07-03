<template>
  <main ref="chatPanelRef" class="chat-panel">
    <section v-if="loadError" class="panel-state panel-state--error">
      <div class="panel-state-title">当前公开链接不可用</div>
      <div class="panel-state-desc">{{ loadError }}</div>
      <div class="panel-state-actions">
        <el-button type="primary" @click="loadDetail">重试</el-button>
      </div>
    </section>

    <template v-else>
      <header :class="['chat-panel-head', activeSessionMessageCount ? 'chat-panel-head--compact' : '']">
        <div class="chat-panel-head-copy">
          <template v-if="!activeSessionMessageCount">
            <div class="chat-brand-mark">
              <el-avatar :src="iconUrl" :size="72"></el-avatar>
            </div>
            <h1>{{ detail.appName || '公开问答入口' }}</h1>
            <div class="chat-panel-head-subtitle">{{ detail.slug || routeSlug || '公开应用' }}</div>
            <p>{{ detail.appDesc || '直接在当前页面提问即可。' }}</p>
          </template>
        </div>

        <div class="chat-panel-head-actions">
          <span class="status-pill" :class="{ 'status-pill--running': sending }">
            {{ sending ? '正在生成' : '准备就绪' }}
          </span>
          <span class="status-pill">当前会话 {{ activeSessionMessageCount }} 条消息</span>
          <el-button
            text
            class="head-text-btn"
            :disabled="interactionLocked || !activeSessionMessageCount"
            @click="startNewConversation"
          >
            新会话
          </el-button>
        </div>
      </header>

      <section v-if="needsPasswordAuth" class="auth-banner">
        <div class="auth-banner-copy">
          <div class="auth-banner-title">这个公开应用需要访问密码</div>
          <div class="auth-banner-desc">验证通过后即可在当前页面继续使用历史会话并发起新提问。</div>
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

      <section ref="chatBoxRef" class="chat-stream">
        <div v-if="!activeSessionMessageCount" class="chat-welcome">
          <div class="chat-empty">
            <div class="chat-empty-title">直接输入问题开始对话</div>
            <div class="chat-empty-desc">
              {{ detail.appDesc || '你可以直接输入问题、业务场景或想验证的内容。' }}
            </div>
          </div>

          <div class="welcome-grid welcome-grid--compact">
            <button
              v-for="prompt in suggestedPrompts.slice(0, 4)"
              :key="prompt"
              type="button"
              class="welcome-card"
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
            :class="['message-row', message.role === 'user' ? 'message-row--user' : 'message-row--assistant']"
          >
            <div class="message-avatar">{{ message.role === 'user' ? '你' : 'AI' }}</div>

            <div class="message-body">
              <div class="message-bubble">
                <div v-if="message.role === 'assistant'" class="assistant-meta">
                  <div class="assistant-label">{{ detail.appName || '公开应用' }}</div>
                  <span v-if="isStreamingMessage(message)" class="stream-status">生成中</span>
                </div>
                <div
                  v-if="message.role === 'assistant' && isStreamingMessage(message)"
                  class="message-content message-content--streaming"
                >
                  <span v-if="getStreamingMessageText(message)">{{ getStreamingMessageText(message) }}</span>
                  <span v-else class="stream-placeholder">正在整理回答</span>
                  <span class="stream-caret"></span>
                </div>
                <div v-else-if="message.role === 'assistant'" class="message-content" v-html="renderMessage(message)"></div>
                <div v-else class="user-content">{{ message.content }}</div>
              </div>
              <div class="message-time">{{ formatMessageTime(message.createdAt) }}</div>
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
            <div class="composer-tip-group">
              <span class="composer-tip">{{ composerTipText }}</span>
              <span class="composer-tip">Enter 发送，Shift + Enter 换行</span>
            </div>

            <div class="composer-btn-group">
              <el-button :disabled="interactionLocked || !userInput" @click="userInput = ''">清空</el-button>
              <el-button type="primary" :loading="sending" :disabled="sendDisabled" @click="sendMessage">
                发送
              </el-button>
            </div>
          </div>
        </div>

        <div class="composer-footer-note">内容由 AI 生成，仅供参考，请结合实际情况判断。</div>
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
  activeSessionMessages,
  activeSessionMessageCount,
  needsPasswordAuth,
  interactionLocked,
  composerTipText,
  sendDisabled,
  formatMessageTime,
  isStreamingMessage,
  getStreamingMessageText,
  renderMessage,
  startNewConversation,
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

.chat-panel-head {
  position: relative;
  padding: 28px 30px 24px;
  border-bottom: 1px solid var(--public-border);
  min-height: 156px;
  background:
    linear-gradient(180deg, rgba(250, 252, 255, 0.98) 0%, rgba(255, 255, 255, 0.96) 72%),
    radial-gradient(circle at top right, rgba(222, 232, 248, 0.68), transparent 42%);
}

.chat-panel-head--compact {
  min-height: auto;
  padding: 18px 24px;
  background: #ffffff;
}

.chat-panel-head-copy {
  max-width: 760px;
  margin: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
}

.chat-brand-mark {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 16px;
}

.chat-brand-mark :deep(.el-avatar) {
  border: 6px solid #f5f7fb;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.06);
}

.chat-panel-head-copy h1 {
  margin: 0;
  color: var(--public-text);
  font-size: 36px;
  line-height: 1.08;
  font-weight: 760;
  letter-spacing: -0.035em;
}

.chat-panel-head-subtitle {
  margin-top: 12px;
  color: var(--public-text-muted);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.chat-panel-head-copy p {
  margin: 16px 0 0;
  max-width: 720px;
  color: var(--public-text-soft);
  font-size: 15px;
  line-height: 1.8;
  text-align: left;
}

.chat-panel-head-actions,
.panel-state-actions,
.composer-btn-group,
.auth-form-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.chat-panel-head-actions {
  position: absolute;
  top: 28px;
  right: 30px;
  justify-content: flex-end;
  max-width: 45%;
}

.chat-panel-head--compact .chat-panel-head-copy {
  display: none;
}

.chat-panel-head--compact .chat-panel-head-actions {
  position: static;
  max-width: none;
  justify-content: flex-end;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: #ffffff;
  color: var(--public-text-soft);
  font-size: 12px;
  border: 1px solid var(--public-border);
  box-shadow: none;
  white-space: nowrap;
}

.status-pill--running {
  background: var(--public-accent-soft);
  color: var(--public-accent-strong);
  border-color: rgba(99, 91, 255, 0.24);
}

.head-text-btn {
  color: var(--public-accent);
  font-weight: 600;
}

.auth-banner {
  margin: 20px 24px 0;
  padding: 20px 22px;
  border-radius: 16px;
  background: #fffaf3;
  border: 1px solid #f2dfbc;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  align-items: start;
}

.auth-banner-title,
.panel-state-title {
  color: var(--public-text);
  font-weight: 700;
}

.auth-banner-title {
  font-size: 18px;
}

.auth-banner-desc {
  margin-top: 8px;
  color: var(--public-text-muted);
  font-size: 13px;
  line-height: 1.65;
}

.auth-form {
  min-width: 0;
}

.chat-stream {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 24px 24px;
  background:
    linear-gradient(180deg, rgba(250, 252, 255, 0.92) 0%, rgba(255, 255, 255, 0) 14%),
    linear-gradient(180deg, #ffffff 0%, #ffffff 100%);
}

.chat-welcome {
  width: 100%;
  padding: 8px 0 16px;
}

.assistant-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.assistant-label {
  color: var(--public-text);
  font-size: 14px;
  font-weight: 700;
}

.assistant-label--welcome {
  margin-top: 14px;
  font-size: 34px;
  letter-spacing: -0.03em;
}

.chat-empty {
  padding: 8px 0 20px;
}

.chat-empty-title {
  color: var(--public-text);
  font-size: 20px;
  line-height: 1.3;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.chat-empty-desc {
  max-width: 720px;
  margin-top: 8px;
  color: var(--public-text-muted);
  font-size: 14px;
  line-height: 1.7;
}

.stream-status {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: var(--public-accent-soft);
  color: var(--public-accent-strong);
  font-size: 11px;
  font-weight: 600;
}

.welcome-grid {
  width: 100%;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.welcome-grid--compact {
  max-width: 100%;
}

.welcome-card,
.shortcut-chip {
  border: 1px solid var(--public-border);
  background: #ffffff;
  color: var(--public-text);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.welcome-card {
  min-height: 68px;
  padding: 18px 20px;
  border-radius: 16px;
  text-align: left;
  cursor: pointer;
  font-size: 14px;
  line-height: 1.75;
  box-shadow: none;
}

.welcome-card:hover,
.shortcut-chip:hover {
  transform: translateY(-1px);
  border-color: var(--public-border-strong);
  box-shadow: var(--public-shadow-soft);
}

.message-row {
  display: flex;
  gap: 12px;
  width: 100%;
  max-width: none;
  margin: 0 0 24px;
}

.message-row--user {
  justify-content: flex-end;
}

.message-row--assistant {
  justify-content: flex-start;
}

.message-avatar {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: var(--public-accent-soft);
  color: var(--public-accent-strong);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  box-shadow: none;
  border: 1px solid rgba(99, 91, 255, 0.14);
}

.message-row--user .message-avatar {
  order: 2;
  background: #eef2f6;
  color: var(--public-text-soft);
  border-color: rgba(52, 64, 84, 0.08);
}

.message-body {
  max-width: min(920px, 84%);
  min-width: 0;
}

.message-row--assistant .message-body {
  max-width: none;
  flex: 1;
}

.message-row--user .message-body {
  order: 1;
  max-width: min(920px, 72%);
}

.message-bubble {
  border-radius: 18px;
  padding: 16px 18px;
  border: 1px solid var(--public-border);
  background: #ffffff;
  box-shadow: var(--public-shadow-soft);
}

.message-row--user .message-bubble {
  background: linear-gradient(180deg, var(--public-accent) 0%, var(--public-accent-strong) 100%);
  border-color: rgba(99, 91, 255, 0.26);
  box-shadow: 0 10px 18px rgba(99, 91, 255, 0.18);
}

.message-row--assistant .message-bubble {
  width: 100%;
}

.message-content,
.user-content {
  color: var(--public-text-soft);
  font-size: 15px;
  line-height: 1.86;
  word-break: break-word;
}

.user-content {
  color: #ffffff;
}

.message-content--streaming {
  white-space: pre-wrap;
}

.stream-placeholder {
  color: var(--public-text-muted);
}

.stream-caret {
  display: inline-block;
  width: 1px;
  height: 1.1em;
  margin-left: 3px;
  background: var(--public-accent);
  vertical-align: -0.12em;
  animation: public-stream-caret 1s steps(1, end) infinite;
}

.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4) {
  margin: 0 0 12px;
  color: var(--public-text);
  line-height: 1.35;
}

.message-content :deep(p),
.message-content :deep(ul),
.message-content :deep(ol),
.message-content :deep(table),
.message-content :deep(blockquote) {
  margin: 0 0 12px;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  padding-left: 20px;
}

.message-content :deep(table) {
  display: block;
  width: 100%;
  overflow-x: auto;
  border-collapse: collapse;
  font-size: 13px;
  border: 1px solid var(--public-border);
  border-radius: 12px;
}

.message-content :deep(th),
.message-content :deep(td) {
  padding: 8px 10px;
  border-bottom: 1px solid var(--public-border);
  text-align: left;
  vertical-align: top;
  white-space: nowrap;
}

.message-content :deep(pre) {
  overflow-x: auto;
  padding: 12px 14px;
  border-radius: 16px;
  background: #111827;
  color: #e5e7eb;
  font-size: 12px;
}

.message-content :deep(code) {
  font-size: 12px;
}

.message-time {
  margin-top: 8px;
  color: var(--public-text-muted);
  font-size: 12px;
}

.message-row--user .message-time {
  text-align: right;
}

.composer-bar {
  padding: 0 24px 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, rgba(255, 255, 255, 0.95) 16%, #ffffff 100%);
}

.shortcut-chip {
  min-height: 36px;
  padding: 7px 14px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 12px;
  line-height: 1.4;
  box-shadow: none;
}

.composer-shell {
  padding: 14px 16px;
  border: 1px solid var(--public-border);
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  box-shadow: var(--public-shadow-soft);
}

.composer-shell :deep(.el-textarea__inner) {
  min-height: 108px !important;
  border-radius: 14px !important;
  padding: 8px 4px;
  font-size: 15px;
  line-height: 1.7;
  box-shadow: none !important;
  border: 0 !important;
  background: transparent !important;
}

.composer-actions {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-top: 8px;
}

.composer-tip-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.composer-tip {
  color: var(--public-text-muted);
  font-size: 12px;
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
}

.panel-state {
  margin: auto;
  width: min(560px, calc(100% - 48px));
  padding: 24px;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: var(--public-shadow-soft);
}

.panel-state--error {
  border: 1px solid #f1d7d9;
  background: #fffaf9;
}

.panel-state-title {
  font-size: 20px;
}

.panel-state-desc {
  margin-top: 8px;
  color: var(--public-text-muted);
  font-size: 14px;
  line-height: 1.7;
}

@keyframes public-stream-caret {
  0%,
  45% {
    opacity: 1;
  }

  50%,
  100% {
    opacity: 0;
  }
}

@media (max-width: 1180px) {
  .auth-banner {
    grid-template-columns: 1fr;
  }

  .chat-panel-head-actions {
    position: static;
    max-width: none;
    justify-content: flex-start;
    margin-top: 18px;
  }

  .chat-panel-head-copy {
    max-width: none;
  }

  .chat-panel-head--compact .chat-panel-head-actions {
    margin-top: 0;
  }
}

@media (max-width: 900px) {
  .chat-panel-head {
    padding: 22px 18px 18px;
  }

  .chat-panel-head--compact {
    padding: 16px 18px;
  }

  .chat-brand-mark :deep(.el-avatar) {
    width: 56px !important;
    height: 56px !important;
  }

  .chat-panel-head-copy h1 {
    font-size: 28px;
  }

  .auth-banner {
    margin: 16px 18px 0;
  }

  .chat-stream {
    padding: 18px;
  }

  .welcome-grid {
    grid-template-columns: 1fr;
  }

  .message-row {
    margin-bottom: 20px;
  }

  .message-body {
    max-width: 100%;
  }

  .composer-bar {
    padding: 0 18px 18px;
  }

  .composer-actions {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 640px) {
  .chat-panel-head-actions {
    gap: 8px;
  }

  .status-pill {
    height: 32px;
  }

  .assistant-label--welcome {
    font-size: 26px;
  }

  .message-row {
    gap: 10px;
  }

  .message-content,
  .user-content {
    font-size: 14px;
    line-height: 1.84;
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
