<template>
  <div class="page-shell chat-page">
    <section class="context-strip">
      <el-button text class="context-back workspace-btn workspace-btn--text" @click="$router.push('/workspace/app/manage')">返回应用</el-button>
      <span class="context-note">聊天调试属于应用工作流，从应用列表进入更顺手。</span>
    </section>
    <el-container class="app">
      <el-main class="main">
        <div class="chat-head">
          <div>
            <div class="chat-head-title">{{ currentChatName || '请选择或创建会话' }}</div>
            <div class="chat-head-desc">支持上下文连续对话，回答内容实时流式返回。</div>
          </div>
          <div class="chat-head-meta">
            <span class="meta-pill" :class="{ running: pageData.sending }">
              {{ pageData.sending ? '生成中' : '空闲中' }}
            </span>
            <span class="meta-pill">SSE {{ sseConnected ? '已连接' : '重连中' }}</span>
            <el-button text class="scroll-btn workspace-btn workspace-btn--ghost" @click="exportCurrentChat">导出会话</el-button>
            <el-button text class="scroll-btn workspace-btn workspace-btn--ghost" @click="scrollBottom">到底部</el-button>
          </div>
        </div>

        <div ref="chatBox" class="chat-box">
          <template v-if="pageData.messages.length">
            <div
              v-for="(msg, idx) in pageData.messages"
              :key="idx"
              :class="['bubble', msg.typeDesc === 'user' ? 'self' : '']"
            >
              <div class="avatar-ball">{{ msg.typeDesc === 'user' ? 'U' : 'AI' }}</div>
              <div class="content-wrap">
                <div class="bubble-top">
                  <div class="bubble-role">{{ msg.typeDesc === 'user' ? '你' : '问答助手' }}</div>
                  <div class="bubble-tools">
                    <el-button text class="tool-btn workspace-btn workspace-btn--text" @click="copyMessage(msg.message)">复制</el-button>
                    <el-button
                      v-if="msg.typeDesc === 'user'"
                      text
                      class="tool-btn workspace-btn workspace-btn--text"
                      @click="retryMessage(msg.message)"
                    >
                      重试
                    </el-button>
                  </div>
                </div>
                <div class="content" v-html="msg.html"></div>
                <!-- 回答评价 -->
                <div v-if="msg.typeDesc !== 'user'" class="feedback-bar">
                  <button
                    :class="['fb-btn', msg.feedback === 'up' ? 'active-up' : '']"
                    @click="setFeedback(idx, 'up')"
                    title="回答有帮助"
                  >👍</button>
                  <button
                    :class="['fb-btn', msg.feedback === 'down' ? 'active-down' : '']"
                    @click="setFeedback(idx, 'down')"
                    title="回答需改进"
                  >👎</button>
                  <span v-if="msg.feedback" class="fb-text">
                    {{ msg.feedback === 'up' ? '感谢反馈' : '已记录，将持续优化' }}
                  </span>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="empty-chat">
            <div class="empty-chat-title">开始你的第一轮对话</div>
            <div class="empty-chat-desc">
              当前会话还没有消息。输入问题后即可开始验证上下文、多轮对话与严格模式效果。
            </div>
            <div class="quick-prompts">
              <button v-for="prompt in quickPrompts" :key="prompt" class="prompt-chip" @click="usePrompt(prompt)">
                {{ prompt }}
              </button>
            </div>
          </div>
        </div>

        <div class="input-bar" v-if="activeChatId">
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
              <span class="tip-pill">当前问题将保留上下文</span>
              <span class="tip-pill">支持 Markdown 返回</span>
            </div>
            <div class="input-actions-right">
              <el-button class="workspace-btn workspace-btn--ghost" @click="pageData.crtUserInput = ''">清空</el-button>
              <el-button type="primary" class="workspace-btn workspace-btn--primary" :loading="pageData.sending" @click="send">发送问题</el-button>
            </div>
          </div>
        </div>
      </el-main>
    </el-container>

  </div>
</template>
<script setup name='Chat' lang='ts'>
import { useRoute } from 'vue-router';
import { ref, reactive, nextTick, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import markdown from 'highlight.js/lib/languages/markdown'
import plaintext from 'highlight.js/lib/languages/plaintext'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import 'highlight.js/styles/github.css'
import { BASE_URL, STORAGE_LAST_APP_ID_KEY } from '@/config/constants';
import { saveItem, getItem } from '@/util/storageUtil';
import { chatApi, listRecentChatApi } from '@/api/workspace/chatApi';
import { pageAppApi } from '@/api/workspace/appApi';
import type { AnyObjDefine, AnyObjsDefine } from '@/types/common';
import { listHistoryMessageApi } from '@/api/workspace/chatMessageApi';
import { useUserStore } from '@/store/useUserStore';

hljs.registerLanguage('bash', bash)
hljs.registerLanguage('sh', bash)
hljs.registerLanguage('java', java)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('json', json)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('md', markdown)
hljs.registerLanguage('plaintext', plaintext)
hljs.registerLanguage('text', plaintext)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)

const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
  highlight: (str: string, lang: string) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre><code class="hljs">${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch {
        return `<pre><code class="hljs">${md.utils.escapeHtml(str)}</code></pre>`
      }
    }
    return `<pre><code class="hljs">${md.utils.escapeHtml(str)}</code></pre>`
  }
})

const route = useRoute()
const userStore = useUserStore()
const pageData = reactive({
  chats: [] as AnyObjsDefine,
  appId: '',
  crtUserInput: '',
  sending: false,
  messages: [] as AnyObjsDefine,
})
const activeChatId = ref('')
const chatBox = ref<HTMLElement | null>(null)
const sseConnected = ref(false)
const quickPrompts = [
  '请先概括这个知识库最核心的主题范围。',
  '请根据当前知识库回答一个最常见的业务问题。',
  '如果上下文中找不到答案，你会怎么处理？',
  '请列出知识库中涉及的关键概念。',
  '帮我总结一下最近的更新内容。',
  '请用通俗易懂的方式解释核心流程。'
]

let evtSource: EventSource | undefined
let streamingMsgIndex = -1

const currentChatName = computed(() => {
  return pageData.chats.find((item: AnyObjDefine) => item.id === activeChatId.value)?.chatName || ''
})

const hasAppId = computed(() => {
  return String(pageData.appId || '').trim().length > 0
})

function ensureAppSelected(showMessage = true) {
  if (hasAppId.value) {
    return true
  }
  evtSource?.close()
  sseConnected.value = false
  activeChatId.value = ''
  pageData.chats = []
  pageData.messages = []
  if (showMessage) {
    ElMessage.warning('正在自动加载应用，请稍候...')
    autoSelectApp()
  }
  return false
}

function switchChat(id: string) {
  activeChatId.value = id
  streamingMsgIndex = -1
  connectSse()
  pageData.messages = []
  loadMessages(id)
}

function loadMessages(chatId: string) {
  listHistoryMessageApi(chatId).then(result => {
    if (result.data) {
      pageData.messages = result.data.map((m: AnyObjDefine) => ({ ...m, html: render(m) }))
      scrollBottom()
    }
  })
}

function pushMessage(typeDesc: string, message: string) {
  const msgObj = { typeDesc, message }
  pageData.messages.push({ ...msgObj, html: render(msgObj) })
  scrollBottom()
}

function renderPlainText(message: string) {
  return md.utils.escapeHtml(String(message || '')).replace(/\n/g, '<br>')
}

function render(msgObj: AnyObjDefine) {
  if (msgObj.typeDesc === 'user') {
    return `<p>${renderPlainText(msgObj.message)}</p>`
  }
  return md.render(String(msgObj.message || ''))
}

async function scrollBottom() {
  await nextTick()
  if (chatBox.value) {
    chatBox.value.scrollTop = chatBox.value.scrollHeight
  }
}

async function send() {
  if (!ensureAppSelected()) {
    return
  }
  const txt = pageData.crtUserInput.trim()
  if (!txt) {
    ElMessage.error({ message: '请输入聊天内容' })
    return
  }
  if (!sseConnected.value) {
    connectSse()
  }
  pageData.crtUserInput = ''
  pushMessage('user', txt)
  pageData.sending = true
  chatApi({
    chatId: activeChatId.value,
    userInput: txt
  }).then(() => {
    pageData.sending = false
  }).catch(() => {
    pageData.sending = false
  })
}

function handleEditorKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    send()
  }
}

function connectSse() {
  if (!ensureAppSelected(false) || !activeChatId.value) {
    return
  }
  evtSource?.close()
  sseConnected.value = false
  const token = userStore.token
  const params = new URLSearchParams({
    chatId: activeChatId.value
  })
  if (token) {
    params.set('token', token)
  }
  evtSource = new EventSource(`${BASE_URL}/consumer/chat/connect?${params.toString()}`)
  evtSource.onopen = () => {
    sseConnected.value = true
  }
  evtSource.addEventListener('connected', () => {
    sseConnected.value = true
  })
  evtSource.onmessage = e => {
    if (e.data === '[DONE]') {
      streamingMsgIndex = -1
      scrollBottom()
      return
    }
    if (streamingMsgIndex === -1) {
      pageData.messages.push({ typeDesc: 'assistant', message: '', html: '' })
      streamingMsgIndex = pageData.messages.length - 1
    }
    const target = pageData.messages[streamingMsgIndex]
    target.message += e.data
    target.html = render(target)
    scrollBottom()
  }
  evtSource.onerror = () => {
    sseConnected.value = false
    evtSource?.close()
    setTimeout(() => {
      if (activeChatId.value) {
        connectSse()
      }
    }, 5000)
  }
}

function loadChats() {
  if (!ensureAppSelected()) {
    return
  }
  listRecentChatApi(pageData.appId).then(result => {
    if (result.data) {
      pageData.chats = result.data
      const routeChatId = normalizeRouteChatId()
      if (routeChatId) {
        const existed = pageData.chats.find((item: AnyObjDefine) => String(item.id) === routeChatId)
        if (existed || !activeChatId.value) {
          switchChat(routeChatId)
        }
      }
    }
  })
}

function normalizeRouteChatId() {
  const chatIdFromRoute = route.params.chatId
  const normalizedChatId = Array.isArray(chatIdFromRoute) ? chatIdFromRoute[0] : chatIdFromRoute
  return normalizedChatId ? String(normalizedChatId) : ''
}

function handleQueryStr() {
  const appIdFromQs = route.query.appId
  const normalizedAppId = Array.isArray(appIdFromQs) ? appIdFromQs[0] : appIdFromQs
  if (normalizedAppId) {
    pageData.appId = String(normalizedAppId).trim()
    saveItem(STORAGE_LAST_APP_ID_KEY, pageData.appId)
  } else {
    // 尝试从 localStorage 中恢复上次选中的 appId
    const lastAppId = getItem(STORAGE_LAST_APP_ID_KEY)
    if (lastAppId) {
      pageData.appId = lastAppId
    }
  }
}

function usePrompt(prompt: string) {
  pageData.crtUserInput = prompt
}

/**
 * 当 URL 和 localStorage 中都没有 appId 时，自动加载用户的应用列表并选中第一个
 */
async function autoSelectApp() {
  try {
    const result = await pageAppApi({ pageNow: 1, pageSize: 1 })
    if (result.data && result.data.length > 0) {
      pageData.appId = String(result.data[0].id)
      saveItem(STORAGE_LAST_APP_ID_KEY, pageData.appId)
      loadChats()
    } else {
      ElMessage.info('您还没有创建应用，请先前往应用管理创建应用后再开始聊天')
    }
  } catch {
    ElMessage.warning('自动加载应用列表失败，请手动从应用列表进入聊天')
  }
}

async function copyMessage(message: string) {
  try {
    await navigator.clipboard.writeText(message)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

function retryMessage(message: string) {
  pageData.crtUserInput = message
}

function setFeedback(idx: number, type: 'up' | 'down') {
  const msg = pageData.messages[idx]
  if (msg.feedback === type) {
    msg.feedback = ''
  } else {
    msg.feedback = type
  }
}

function exportCurrentChat() {
  if (!pageData.messages.length) {
    ElMessage.warning('当前会话暂无内容可导出')
    return
  }
  const lines = [
    `# ${currentChatName.value || '对话记录'}`,
    `> 导出时间：${new Date().toLocaleString()}`,
    `> 消息数：${pageData.messages.length}`,
    '',
    '---',
    ''
  ]
  pageData.messages.forEach((msg: AnyObjDefine) => {
    const role = msg.typeDesc === 'user' ? '🧑 **你**' : '🤖 **问答助手**'
    lines.push(role)
    lines.push('')
    lines.push(msg.message)
    lines.push('')
    lines.push('---')
    lines.push('')
  })
  const text = lines.join('\n')
  const blob = new Blob([text], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${currentChatName.value || 'chat-session'}.md`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('已导出为 Markdown 文件')
}

onMounted(() => {
  handleQueryStr()
  if (hasAppId.value) {
    loadChats()
  } else {
    autoSelectApp()
  }
})

onUnmounted(() => evtSource?.close())
</script>
<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  flex: 1;
  overflow: hidden;
  gap: 14px;
}

.context-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 2px;
}

.context-back {
  margin-left: -10px;
}

.context-note {
  color: var(--space-text-soft);
  font-size: 13px;
}

.app {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(227, 232, 241, 0.72);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 60px rgba(37, 48, 71, 0.1);
}

.app > .el-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(249, 251, 255, 0.92));
}

.chat-head {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px 10px;
  border-bottom: 1px solid rgba(227, 232, 241, 0.9);
  background: rgba(255, 255, 255, 0.72);
}

.chat-head-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--space-text);
}

.chat-head-desc {
  margin-top: 4px;
  color: var(--space-text-soft);
  font-size: 12px;
}

.chat-head-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.meta-pill,
.tip-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(227, 232, 241, 0.9);
  background: rgba(255, 255, 255, 0.9);
  color: var(--space-text-soft);
  font-size: 11px;
}

.meta-pill.running {
  background: rgba(64, 158, 255, 0.12);
  border-color: rgba(64, 158, 255, 0.18);
  color: var(--space-primary);
}

.scroll-btn {
  min-height: 32px !important;
  font-size: 12px;
}

.chat-box {
  flex: 1;
  min-height: 0;
  padding: 12px 20px;
  overflow-y: auto;
  overscroll-behavior: contain;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0));
}

.chat-box:has(.empty-chat) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.bubble {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.bubble.self {
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

.bubble.self .avatar-ball {
  color: #ffffff;
  border: 0;
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 70%, #2f7fe2 100%);
}

.content-wrap {
  max-width: min(900px, 88%);
}

.bubble-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.bubble-role {
  color: var(--space-text-soft);
  font-size: 11px;
  font-weight: 600;
}

.bubble-tools {
  display: flex;
  gap: 10px;
}

.tool-btn {
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

.bubble.self .content {
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

.bubble.self .content :deep(code) {
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
}

.content :deep(a) {
  color: var(--space-primary);
  text-decoration: none;
  border-bottom: 1px solid rgba(64, 158, 255, 0.22);
}

.bubble.self .content :deep(a) {
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

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
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

.input-head,
.input-actions,
.input-actions-left,
.input-actions-right {
  display: flex;
  align-items: center;
}

.input-head,
.input-actions {
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

@media (max-width: 1200px) {
  .app {
    min-height: 60vh;
  }
}

/* 回答评价 */
.feedback-bar {
  display: flex;
  align-items: center;
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
</style>
