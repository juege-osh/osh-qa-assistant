<template>
  <div class="page-shell workspace-chat-page">
    <section class="workspace-context-strip">
      <el-button text class="workspace-btn workspace-btn--text" @click="$router.push('/workspace/app/manage')">返回应用</el-button>
      <span class="workspace-context-note">回到应用列表可以切换别的应用。</span>
    </section>

    <section class="workspace-section-card chat-shell">
      <div class="chat-head">
        <div>
          <div class="chat-head-title">{{ currentChatName || '请选择或创建会话' }}</div>
          <div class="chat-head-desc">直接提问，看看这轮回答是否符合预期。</div>
        </div>
        <div class="chat-head-meta">
          <span class="workspace-status-pill" :class="{ 'workspace-status-pill--active': pageData.sending }">
            {{ pageData.sending ? '生成中' : '空闲中' }}
          </span>
          <span class="workspace-status-pill">SSE {{ sseConnected ? '已连接' : '重连中' }}</span>
          <el-button text class="workspace-btn workspace-btn--ghost chat-head-btn" @click="exportCurrentChat">导出会话</el-button>
          <el-button text class="workspace-btn workspace-btn--ghost chat-head-btn" @click="scrollBottom">到底部</el-button>
        </div>
      </div>

      <div ref="chatBox" class="chat-box">
        <template v-if="pageData.messages.length">
          <div
            v-for="(message, index) in pageData.messages"
            :key="index"
            :class="['bubble', message.typeDesc === 'user' ? 'bubble--self' : '']"
          >
            <div class="avatar-ball">{{ message.typeDesc === 'user' ? 'U' : 'AI' }}</div>
            <div class="content-wrap">
              <div class="bubble-top">
                <div class="bubble-role">{{ message.typeDesc === 'user' ? '你' : '问答助手' }}</div>
                <div class="bubble-tools">
                  <el-button text class="workspace-btn workspace-btn--text chat-tool-btn" @click="copyMessage(message.message)">复制</el-button>
                  <el-button
                    v-if="message.typeDesc === 'user'"
                    text
                    class="workspace-btn workspace-btn--text chat-tool-btn"
                    @click="retryMessage(message.message)"
                  >
                    重试
                  </el-button>
                </div>
              </div>
              <div class="content" v-html="message.html"></div>
              <div v-if="message.typeDesc !== 'user'" class="feedback-bar">
                <button :class="['fb-btn', message.feedback === 'up' ? 'active-up' : '']" @click="setFeedback(index, 'up')" title="回答有帮助">
                  👍
                </button>
                <button :class="['fb-btn', message.feedback === 'down' ? 'active-down' : '']" @click="setFeedback(index, 'down')" title="回答需改进">
                  👎
                </button>
                <span v-if="message.feedback" class="fb-text">
                  {{ message.feedback === 'up' ? '感谢反馈' : '已记录，将持续优化' }}
                </span>
              </div>
            </div>
          </div>
        </template>

        <div v-else class="workspace-empty-state chat-empty">
          <div class="empty-chat-title">开始你的第一轮对话</div>
          <div class="empty-chat-desc">先发一个问题，看看回答和上下文表现。</div>
          <div class="quick-prompts">
            <button v-for="prompt in quickPrompts" :key="prompt" class="prompt-chip" @click="usePrompt(prompt)">
              {{ prompt }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="activeChatId" class="input-bar">
        <div class="input-head">
          <div class="input-title">输入问题</div>
          <div class="input-tip">Enter 发送，Shift + Enter 换行</div>
        </div>
        <el-input
          v-model="pageData.crtUserInput"
          :rows="4"
          type="textarea"
          :disabled="pageData.sending"
          placeholder="请输入你要测试的问题、场景描述或追问内容"
          @keydown="handleEditorKeydown"
        />
        <div class="input-actions">
          <div class="input-actions-left">
            <span class="workspace-status-pill">当前问题将保留上下文</span>
          </div>
          <div class="input-actions-right">
            <el-button class="workspace-btn workspace-btn--ghost" @click="pageData.crtUserInput = ''">清空</el-button>
            <el-button type="primary" class="workspace-btn workspace-btn--primary" :loading="pageData.sending" @click="send">发送问题</el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useWorkspaceChatConversationFeature } from '../composables/useWorkspaceChatConversationFeature'

const {
  chatBox,
  activeChatId,
  sseConnected,
  pageData,
  quickPrompts,
  currentChatName,
  scrollBottom,
  usePrompt,
  copyMessage,
  retryMessage,
  setFeedback,
  exportCurrentChat,
  handleEditorKeydown,
  send
} = useWorkspaceChatConversationFeature()
</script>

<style scoped>
.workspace-chat-page {
  min-height: 0;
  height: 100%;
  flex: 1;
  overflow: hidden;
  gap: 14px;
}

.chat-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

.chat-head {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px 14px;
  border-bottom: 1px solid rgba(227, 232, 241, 0.9);
  background: rgba(255, 255, 255, 0.72);
}

.chat-head-title {
  color: var(--space-text);
  font-size: 18px;
  font-weight: 800;
}

.chat-head-desc {
  margin-top: 4px;
  color: var(--space-text-soft);
  font-size: 12px;
}

.chat-head-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.chat-head-btn {
  min-height: 32px !important;
  font-size: 12px;
}

.chat-box {
  flex: 1;
  min-height: 0;
  padding: 14px 22px;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0));
}

.chat-box:has(.chat-empty) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.bubble {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.bubble--self {
  flex-direction: row-reverse;
}

.avatar-ball {
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: rgba(64, 158, 255, 0.1);
  border: 1px solid rgba(64, 158, 255, 0.08);
  color: var(--space-primary);
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.1);
}

.bubble--self .avatar-ball {
  color: #ffffff;
  border: 0;
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 70%, #2f7fe2 100%);
}

.content-wrap {
  max-width: min(900px, 88%);
}

.bubble-top,
.bubble-tools,
.input-head,
.input-actions,
.input-actions-left,
.input-actions-right,
.feedback-bar {
  display: flex;
  align-items: center;
}

.bubble-top,
.input-actions {
  justify-content: space-between;
  gap: 14px;
}

.bubble-top {
  margin-bottom: 6px;
}

.bubble-role {
  color: var(--space-text-soft);
  font-size: 11px;
  font-weight: 600;
}

.bubble-tools {
  gap: 10px;
}

.chat-tool-btn {
  min-height: 28px !important;
  font-size: 12px;
}

.content {
  padding: 12px 16px;
  border: 1px solid rgba(227, 232, 241, 0.95);
  border-radius: 14px 14px 14px 4px;
  color: var(--space-text);
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  box-shadow: 0 16px 32px rgba(37, 48, 71, 0.07);
  word-break: break-word;
  line-height: 1.7;
  font-size: 14px;
}

.bubble--self .content {
  color: #ffffff;
  border: 0;
  border-radius: 14px 14px 4px 14px;
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 70%, #2f7fe2 100%);
  box-shadow: 0 18px 34px rgba(64, 158, 255, 0.24);
}

.content :deep(*) {
  max-width: 100%;
}

.content :deep(p),
.content :deep(ul),
.content :deep(ol),
.content :deep(blockquote),
.content :deep(pre),
.content :deep(table) {
  margin: 0 0 10px;
}

.content :deep(p:last-child),
.content :deep(ul:last-child),
.content :deep(ol:last-child),
.content :deep(blockquote:last-child),
.content :deep(pre:last-child),
.content :deep(table:last-child) {
  margin-bottom: 0;
}

.content :deep(ul),
.content :deep(ol) {
  padding-left: 20px;
}

.content :deep(li + li) {
  margin-top: 4px;
}

.content :deep(strong) {
  font-weight: 700;
}

.content :deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(64, 158, 255, 0.08);
  color: var(--space-primary);
  font-size: 0.92em;
}

.bubble--self .content :deep(code) {
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
}

.content :deep(a) {
  color: var(--space-primary);
  text-decoration: none;
  border-bottom: 1px solid rgba(64, 158, 255, 0.22);
}

.bubble--self .content :deep(a) {
  color: #ffffff;
  border-bottom-color: rgba(255, 255, 255, 0.3);
}

.content :deep(blockquote) {
  padding: 8px 12px;
  border-left: 3px solid rgba(64, 158, 255, 0.26);
  border-radius: 0 10px 10px 0;
  background: rgba(64, 158, 255, 0.05);
  color: var(--space-text-soft);
}

.content :deep(h1),
.content :deep(h2),
.content :deep(h3),
.content :deep(h4) {
  margin: 0 0 10px;
  color: var(--space-text);
  font-weight: 700;
  line-height: 1.4;
}

.content :deep(h1) {
  font-size: 20px;
}

.content :deep(h2) {
  font-size: 17px;
}

.content :deep(h3),
.content :deep(h4) {
  font-size: 15px;
}

.bubble :deep(pre) {
  margin: 6px 0;
  padding: 10px;
  overflow: auto;
  border-radius: 10px;
  background: #f5f8fc;
  border: 1px solid rgba(227, 232, 241, 0.95);
}

.content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  overflow: hidden;
  border: 1px solid rgba(227, 232, 241, 0.95);
  border-radius: 10px;
}

.content :deep(th),
.content :deep(td) {
  padding: 8px 10px;
  border-bottom: 1px solid rgba(227, 232, 241, 0.9);
  text-align: left;
  vertical-align: top;
  font-size: 13px;
}

.content :deep(th) {
  background: rgba(64, 158, 255, 0.06);
  font-weight: 700;
}

.chat-empty {
  flex-direction: column;
  justify-content: center;
  width: min(980px, 100%);
  min-height: clamp(220px, 42vh, 420px);
  text-align: center;
  margin: 0 auto;
  padding: 8px 20px 4px;
}

.empty-chat-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--space-text);
}

.empty-chat-desc {
  max-width: 640px;
  margin-top: 10px;
  color: var(--space-text-soft);
  line-height: 1.8;
  font-size: 13px;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
}

.prompt-chip {
  padding: 9px 16px;
  border: 1px solid rgba(227, 232, 241, 0.95);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--space-text);
  cursor: pointer;
  font-size: 13px;
  box-shadow: 0 10px 24px rgba(37, 48, 71, 0.05);
  transition: transform .2s ease, border-color .2s ease, box-shadow .2s ease;
}

.prompt-chip:hover {
  transform: translateY(-2px);
  border-color: var(--space-primary);
  box-shadow: 0 14px 28px rgba(64, 158, 255, 0.12);
}

.input-bar {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 18px 14px;
  border-top: 1px solid rgba(227, 232, 241, 0.9);
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.94), rgba(255, 255, 255, 0.98));
}

.input-head {
  justify-content: space-between;
  gap: 14px;
}

.input-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--space-text);
}

.input-tip {
  color: var(--space-muted);
  font-size: 11px;
}

.input-actions-left,
.input-actions-right {
  flex-wrap: wrap;
  gap: 10px;
}

.feedback-bar {
  gap: 8px;
  margin-top: 6px;
  padding-left: 4px;
}

.fb-btn {
  padding: 4px 10px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.fb-btn:hover {
  border-color: var(--space-primary);
  background: rgba(64, 158, 255, 0.06);
}

.fb-btn.active-up {
  border-color: var(--space-success);
  background: rgba(16, 185, 129, 0.1);
}

.fb-btn.active-down {
  border-color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
}

.fb-text {
  color: var(--space-muted);
  font-size: 11px;
}

@media (max-width: 1200px) {
  .chat-shell {
    min-height: 60vh;
  }
}

@media (max-width: 768px) {
  .chat-head,
  .input-head,
  .input-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .chat-head-meta {
    justify-content: flex-start;
  }

  .content-wrap {
    max-width: 100%;
  }
}
</style>
