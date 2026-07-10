<template>
  <div class="page-shell workspace-chat-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <el-button text class="workspace-btn workspace-btn--text" @click="$router.push('/workspace/app/manage')">返回应用</el-button>
        <span class="workspace-context-note">当前对话会保留上下文，回到应用列表后可以切换别的应用继续测试。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">会话 {{ chatCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">消息 {{ messageCountDisplay }}</span>
      </div>
    </section>

    <section class="workspace-section-card chat-shell workspace-chat-panel">
      <div class="chat-head workspace-overview-head workspace-chat-panel__head">
        <div>
          <div class="chat-head-title panel-title panel-title--lg">{{ currentChatName || '请选择或创建会话' }}</div>
          <div class="chat-head-desc workspace-panel-desc">直接提问，观察回答质量、上下文衔接和流式返回过程是否符合预期。</div>
        </div>
        <div class="chat-head-meta workspace-chat-panel__meta">
          <span class="workspace-status-pill" :class="{ 'workspace-status-pill--active': pageData.sending }">
            {{ pageData.sending ? '生成中' : '准备就绪' }}
          </span>
          <span class="workspace-status-pill">流式 {{ sseConnected ? '已连接' : '重连中' }}</span>
          <el-button text class="workspace-btn workspace-btn--ghost chat-head-btn" @click="exportCurrentChat">导出会话</el-button>
          <el-button text class="workspace-btn workspace-btn--ghost chat-head-btn" @click="scrollBottom">到底部</el-button>
        </div>
      </div>

      <div
        ref="chatBox"
        :class="[
          'chat-box',
          'space-scrollbar',
          'workspace-chat-panel__body',
          !pageData.messages.length ? 'workspace-chat-panel__body--empty' : ''
        ]"
      >
        <div v-if="pageData.messages.length" class="workspace-chat-stream">
          <div
            v-for="(message, index) in pageData.messages"
            :key="index"
            :class="[
              'workspace-chat-message',
              message.typeDesc === 'user' ? 'workspace-chat-message--self' : ''
            ]"
          >
            <div class="workspace-chat-avatar">{{ message.typeDesc === 'user' ? 'U' : 'AI' }}</div>
            <div :class="['workspace-chat-message__body', message.typeDesc === 'user' ? 'workspace-chat-message__body--self' : 'workspace-chat-message__body--assistant']">
              <div class="workspace-chat-message__head">
                <div class="workspace-chat-message__role">{{ message.typeDesc === 'user' ? '你' : '问答助手' }}</div>
                <div class="workspace-chat-message__tools">
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
              <div class="workspace-chat-message__bubble">
                <div v-if="message.typeDesc !== 'user' && isStreamingMessage(message, index)" class="workspace-chat-message__stream-meta">
                  <span class="workspace-chat-message__stream-status">正在生成</span>
                </div>
                <div
                  v-if="message.typeDesc !== 'user' && isStreamingMessage(message, index)"
                  class="workspace-chat-message__stream-content"
                >
                  <span v-if="getStreamingMessageText(message)">{{ getStreamingMessageText(message) }}</span>
                  <span v-else class="workspace-chat-message__stream-placeholder">正在整理回答</span>
                  <span class="workspace-chat-message__stream-caret"></span>
                </div>
                <div
                  v-else-if="message.typeDesc !== 'user'"
                  v-html="message.html"
                ></div>
                <div v-else v-html="message.html"></div>
              </div>
              <div v-if="message.typeDesc !== 'user'" class="workspace-chat-feedback">
                <button :class="['workspace-chat-feedback__btn', message.feedback === 'up' ? 'workspace-chat-feedback__btn--up-active' : '']" @click="setFeedback(index, 'up')" title="回答有帮助">
                  有帮助
                </button>
                <button :class="['workspace-chat-feedback__btn', message.feedback === 'down' ? 'workspace-chat-feedback__btn--down-active' : '']" @click="setFeedback(index, 'down')" title="回答需改进">
                  需改进
                </button>
                <span v-if="message.feedback" class="workspace-chat-feedback__text">
                  {{ message.feedback === 'up' ? '感谢反馈' : '已记录，将持续优化' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="workspace-empty-panel chat-empty workspace-chat-panel__empty">
          <div class="workspace-empty-title">开始你的第一轮对话</div>
          <div class="workspace-empty-desc">可以先问一个真实业务问题，观察回答质量、上下文衔接和流式返回是否稳定。</div>
          <div class="workspace-empty-note">建议先用一条真实业务问题试通链路，再逐步追问和补充上下文。</div>
          <div class="quick-prompts workspace-quick-grid workspace-chat-panel__quick">
            <button v-for="prompt in quickPrompts" :key="prompt" class="prompt-chip workspace-quick-chip" @click="usePrompt(prompt)">
              {{ prompt }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="activeChatId" class="input-bar workspace-chat-panel__footer">
        <div class="input-shell workspace-composer-card">
          <div class="input-head workspace-composer-card__head">
            <div class="input-title workspace-composer-card__title">输入问题</div>
            <div class="input-tip workspace-composer-card__tip">Enter 发送，Shift + Enter 换行</div>
          </div>
          <el-input
            v-model="pageData.crtUserInput"
            :rows="3"
            type="textarea"
            :disabled="pageData.sending"
            placeholder="请输入你要测试的问题、场景描述或追问内容"
            @keydown="handleEditorKeydown"
          />
          <div class="input-actions workspace-composer-card__actions">
            <div class="input-actions-left workspace-composer-card__group">
              <span class="workspace-status-pill">上下文已开启</span>
              <span class="workspace-status-pill">流式 {{ sseConnected ? '已连接' : '重连中' }}</span>
            </div>
            <div class="input-actions-right workspace-composer-card__group">
              <el-button class="workspace-btn workspace-btn--ghost" @click="pageData.crtUserInput = ''">清空</el-button>
              <el-button type="primary" class="workspace-btn workspace-btn--primary" :loading="pageData.sending" @click="send">发送问题</el-button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
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
  isStreamingMessage,
  getStreamingMessageText,
  exportCurrentChat,
  handleEditorKeydown,
  send
} = useWorkspaceChatConversationFeature()

const chatCountDisplay = computed(() => pageData.chats.length)
const messageCountDisplay = computed(() => pageData.messages.length)
</script>

<style scoped>
.workspace-chat-page {
  min-height: 0;
  height: 100%;
  flex: 1;
  overflow: hidden;
  gap: 16px;
}

.chat-shell {
  min-height: 0;
}

.chat-head {
  display: flex;
}

.chat-head-meta {
  min-width: 0;
}

.chat-head-btn {
  min-height: 32px !important;
  font-size: 12px;
}

.chat-box {
  min-width: 0;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.chat-tool-btn {
  min-height: 28px !important;
  font-size: 12px;
}

.chat-empty {
  text-align: center;
}

.input-bar {
  min-width: 0;
}

.input-actions-left,
.input-actions-right {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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
}
</style>
