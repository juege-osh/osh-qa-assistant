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
      <header class="chat-panel-head">
        <div class="chat-panel-head-copy">
          <div class="chat-brand-mark">
            <el-avatar :src="iconUrl" :size="72"></el-avatar>
          </div>
          <h1>{{ detail.appName || '公开问答入口' }}</h1>
          <div class="chat-panel-head-subtitle">{{ detail.slug || routeSlug || '公开应用' }}</div>
          <p>{{ detail.appDesc || '直接在当前页面提问即可。' }}</p>
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
          <div class="welcome-message">
            <div class="welcome-brand">
              <el-avatar :src="iconUrl" :size="58"></el-avatar>
            </div>
            <div class="welcome-kicker">公开应用</div>
            <div class="assistant-label assistant-label--welcome">{{ detail.appName || '公开应用' }}</div>
            <div class="welcome-message-text">
              {{ detail.appDesc || '你可以直接输入问题、业务场景或想验证的内容。' }}
            </div>
          </div>

          <div class="welcome-grid">
            <button
              v-for="prompt in suggestedPrompts"
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
                <div v-if="message.role === 'assistant'" class="assistant-label">{{ detail.appName || '公开应用' }}</div>
                <div v-if="message.role === 'assistant'" class="message-content" v-html="renderMessage(message)"></div>
                <div v-else class="user-content">{{ message.content }}</div>
              </div>
              <div class="message-time">{{ formatMessageTime(message.createdAt) }}</div>
            </div>
          </div>
        </template>
      </section>

      <div class="composer-bar">
        <div v-if="!activeSessionMessageCount" class="composer-shortcuts">
          <button
            v-for="prompt in suggestedPrompts.slice(0, 3)"
            :key="prompt"
            type="button"
            class="shortcut-chip"
            @click="usePrompt(prompt)"
          >
            {{ prompt }}
          </button>
        </div>

        <div class="composer-shell">
          <el-input
            v-model="userInput"
            type="textarea"
            :rows="3"
            :disabled="sending || !!loadError"
            placeholder="有什么需要问我的吗"
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
  border: 1px solid #e7edf4;
  border-radius: 30px;
  background: #ffffff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.04);
}

.chat-panel-head {
  position: relative;
  padding: 30px 34px 26px;
  border-bottom: 1px solid #edf1f6;
  min-height: 198px;
  background:
    linear-gradient(180deg, rgba(248, 250, 253, 0.92) 0%, rgba(255, 255, 255, 0.96) 72%),
    radial-gradient(circle at top center, rgba(232, 239, 249, 0.65), transparent 48%);
}

.chat-panel-head-copy {
  max-width: 760px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.chat-brand-mark {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.chat-brand-mark :deep(.el-avatar) {
  border: 8px solid #f5f7fb;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.06);
}

.chat-panel-head-copy h1 {
  margin: 0;
  color: #101828;
  font-size: 42px;
  line-height: 1.08;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.chat-panel-head-subtitle {
  margin-top: 14px;
  color: #667085;
  font-size: 14px;
  font-weight: 600;
}

.chat-panel-head-copy p {
  margin: 18px auto 0;
  max-width: 760px;
  color: #4b5563;
  font-size: 15px;
  line-height: 1.85;
  text-align: center;
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
  right: 34px;
  justify-content: flex-end;
  max-width: 45%;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  color: #4b5563;
  font-size: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.03);
  white-space: nowrap;
}

.status-pill--running {
  background: #eff6ff;
  color: #246bdb;
  border-color: #bfdbfe;
}

.head-text-btn {
  color: #1570ef;
  font-weight: 600;
}

.auth-banner {
  margin: 20px 28px 0;
  padding: 20px 22px;
  border-radius: 22px;
  background: #fff9f1;
  border: 1px solid #f5dfb8;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  align-items: start;
}

.auth-banner-title,
.panel-state-title {
  color: #111827;
  font-weight: 700;
}

.auth-banner-title {
  font-size: 18px;
}

.auth-banner-desc {
  margin-top: 8px;
  color: #6b7280;
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
  padding: 18px 34px 22px;
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.84) 0%, rgba(255, 255, 255, 0) 16%);
}

.chat-welcome {
  max-width: 980px;
  margin: 0 auto;
  padding: 18px 0 30px;
}

.welcome-message {
  max-width: 920px;
  margin: 0 auto 34px;
  text-align: center;
}

.welcome-brand {
  display: flex;
  justify-content: center;
  margin-bottom: 18px;
}

.welcome-brand :deep(.el-avatar) {
  border: 8px solid #f6f8fb;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.05);
}

.welcome-kicker {
  color: #1570ef;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.assistant-label {
  margin-bottom: 12px;
  color: #111827;
  font-size: 15px;
  font-weight: 700;
}

.assistant-label--welcome {
  margin-top: 14px;
  font-size: 34px;
  letter-spacing: -0.03em;
}

.welcome-message-text {
  max-width: 780px;
  margin: 18px auto 0;
  color: #374151;
  font-size: 16px;
  line-height: 1.9;
  text-align: center;
}

.welcome-grid {
  max-width: 1040px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.welcome-card,
.shortcut-chip {
  border: 1px solid #e4e9f0;
  background: #ffffff;
  color: #111827;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.welcome-card {
  min-height: 74px;
  padding: 22px 24px;
  border-radius: 22px;
  text-align: left;
  cursor: pointer;
  font-size: 14px;
  line-height: 1.75;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.03);
}

.welcome-card:hover,
.shortcut-chip:hover {
  transform: translateY(-1px);
  border-color: #d7dfea;
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.06);
}

.message-row {
  display: flex;
  gap: 12px;
  max-width: 1060px;
  margin: 0 auto 30px;
}

.message-row--user {
  justify-content: flex-end;
}

.message-row--assistant {
  justify-content: flex-start;
}

.message-avatar {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  background: #eef4ff;
  color: #246bdb;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.05);
}

.message-row--user .message-avatar {
  order: 2;
  background: #f3f4f6;
  color: #4b5563;
}

.message-body {
  max-width: min(920px, 80%);
  min-width: 0;
}

.message-row--user .message-body {
  order: 1;
}

.message-bubble {
  border-radius: 22px;
  padding: 0;
}

.message-row--assistant .message-bubble {
  background: transparent;
}

.message-row--user .message-bubble {
  padding: 15px 18px;
  background: #f3f5f8;
  border: 1px solid #e7ebf1;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.03);
}

.message-content,
.user-content {
  color: #374151;
  font-size: 15px;
  line-height: 1.92;
  word-break: break-word;
}

.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4) {
  margin: 0 0 12px;
  color: #111827;
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
}

.message-content :deep(th),
.message-content :deep(td) {
  padding: 8px 10px;
  border-bottom: 1px solid #e5e7eb;
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
  color: #9ca3af;
  font-size: 12px;
}

.message-row--user .message-time {
  text-align: right;
}

.composer-bar {
  padding: 0 28px 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, rgba(255, 255, 255, 0.92) 14%, #ffffff 100%);
}

.composer-shortcuts {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 18px;
}

.shortcut-chip {
  min-height: 42px;
  padding: 9px 16px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
  line-height: 1.4;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.03);
}

.composer-shell {
  padding: 18px 20px 16px;
  border: 1px solid #e5eaf2;
  border-radius: 28px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.05);
}

.composer-shell :deep(.el-textarea__inner) {
  min-height: 128px !important;
  border-radius: 22px !important;
  padding: 10px 12px;
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

.composer-btn-group :deep(.el-button) {
  min-width: 92px;
  height: 44px;
  border-radius: 16px;
}

.composer-btn-group :deep(.el-button--primary) {
  border: 0;
  box-shadow: 0 12px 24px rgba(44, 140, 255, 0.24);
}

.composer-tip,
.panel-state-desc {
  color: #6b7280;
  line-height: 1.65;
}

.composer-tip {
  font-size: 12px;
}

.composer-footer-note {
  margin-top: 14px;
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
}

.panel-state {
  margin: 28px 32px;
  padding: 24px;
  border-radius: 20px;
  background: #ffffff;
}

.panel-state--error {
  border: 1px solid #fecaca;
  background: #fffafa;
}

.panel-state-title {
  font-size: 20px;
}

.panel-state-desc {
  margin-top: 8px;
  font-size: 14px;
}

@media (max-width: 1180px) {
  .auth-banner {
    grid-template-columns: 1fr;
  }

  .chat-panel-head {
    min-height: auto;
    padding-top: 22px;
  }

  .chat-panel-head-actions {
    position: static;
    max-width: none;
    justify-content: center;
    margin-top: 22px;
  }
}

@media (max-width: 900px) {
  .chat-panel-head {
    padding: 18px 16px 14px;
  }

  .chat-brand-mark :deep(.el-avatar) {
    width: 56px !important;
    height: 56px !important;
    border-width: 6px !important;
  }

  .chat-panel-head-copy h1 {
    font-size: 28px;
  }

  .chat-panel-head-copy p {
    font-size: 14px;
    margin-top: 14px;
  }

  .auth-banner,
  .chat-stream,
  .composer-bar {
    padding-left: 16px;
    padding-right: 16px;
  }

  .auth-banner,
  .panel-state {
    margin-left: 16px;
    margin-right: 16px;
  }

  .welcome-grid {
    grid-template-columns: 1fr;
  }

  .welcome-message-text {
    font-size: 15px;
  }

  .assistant-label--welcome {
    font-size: 26px;
  }

  .message-body {
    max-width: 100%;
  }

  .composer-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .composer-shell {
    padding: 16px;
    border-radius: 24px;
  }

  .composer-shell :deep(.el-textarea__inner) {
    min-height: 108px !important;
    padding: 8px 6px;
  }

  .composer-btn-group {
    justify-content: flex-end;
    width: 100%;
  }

  .composer-tip-group {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .chat-panel-head-actions {
    gap: 8px;
  }

  .status-pill {
    height: 36px;
    padding: 0 12px;
  }

  .welcome-card {
    padding: 18px 18px;
  }

  .message-row {
    gap: 10px;
    margin-bottom: 24px;
  }

  .message-avatar {
    width: 34px;
    height: 34px;
    border-radius: 12px;
  }

  .message-content,
  .user-content {
    font-size: 14px;
    line-height: 1.84;
  }
}
</style>
