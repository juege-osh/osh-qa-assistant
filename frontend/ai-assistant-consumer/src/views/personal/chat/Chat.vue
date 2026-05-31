<template>
  <div class="page-shell chat-page">
    <section class="hero-panel">
      <div class="hero-title">智能问答工作台</div>
      <div class="hero-subtitle">
        用于验证应用与知识库的真实问答效果。支持多会话管理、上下文连续对话、回答复制、问题重试和会话重命名，方便做完整调试闭环。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">当前会话：{{ currentChatName || '未选择' }}</span>
        <span class="hero-badge">会话数：{{ pageData.chats.length }}</span>
        <span class="hero-badge">消息数：{{ pageData.messages.length }}</span>
      </div>
    </section>

    <el-container class="app">
      <el-aside width="320px" class="aside">
        <div class="aside-header">
          <div>
            <div class="aside-eyebrow">Sessions</div>
            <div class="aside-title">会话管理</div>
          </div>
          <el-button type="primary" @click="addChat">新开聊天</el-button>
        </div>

        <div class="session-hint">建议按业务场景拆分会话，便于复盘上下文和回答质量。</div>

        <div class="session-search">
          <el-input
            v-model="sessionKeyword"
            placeholder="搜索会话名称"
            clearable
          />
        </div>

        <el-scrollbar class="session-scroll">
          <div
            v-for="s in filteredChats"
            :key="s.id"
            :class="['session-card', activeChatId === s.id ? 'is-active' : '']"
            @click="switchChat(s.id)"
          >
            <div class="session-main">
              <div class="session-name">{{ s.chatName }}</div>
              <div class="session-meta">会话 ID：{{ s.id }}</div>
            </div>
            <div class="session-actions">
              <el-button class="session-btn" text @click.stop="openRenameDialog(s)">
                重命名
              </el-button>
              <el-button class="session-btn delete" text @click.stop="delChat(s.id)">
                删除
              </el-button>
            </div>
          </div>
          <div v-if="!pageData.chats.length" class="empty-session">
            还没有聊天会话，先创建一个再开始测试。
          </div>
        </el-scrollbar>
      </el-aside>

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
            <el-button text class="scroll-btn" @click="exportCurrentChat">导出会话</el-button>
            <el-button text class="scroll-btn" @click="scrollBottom">到底部</el-button>
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
                    <el-button text class="tool-btn" @click="copyMessage(msg.message)">复制</el-button>
                    <el-button
                      v-if="msg.typeDesc === 'user'"
                      text
                      class="tool-btn"
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
              先在左侧选择一个会话，然后输入问题。建议先问知识库范围内的问题，再验证上下文、多轮对话与严格模式效果。
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
              <el-button @click="pageData.crtUserInput = ''">清空</el-button>
              <el-button type="primary" :loading="pageData.sending" @click="send">发送问题</el-button>
            </div>
          </div>
        </div>
      </el-main>
    </el-container>

    <el-dialog v-model="renameDialogVisible" title="重命名会话" width="420px">
      <el-form label-width="90px">
        <el-form-item label="会话名称">
          <el-input v-model="renameForm.chatName" maxlength="50" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRename">保存</el-button>
      </template>
    </el-dialog>
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
import { BASE_URL } from '@/config/constants';
import { addChatApi, chatApi, deleteChatByIdApi, listRecentChatApi, renameChatApi } from '@/api/chatApi';
import type { AnyObjDefine, AnyObjsDefine } from '@/types/common';
import { listHistoryMessageApi } from '@/api/chatMessageApi';

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
const renameDialogVisible = ref(false)
const sessionKeyword = ref('')
const renameForm = reactive({
  id: '',
  chatName: ''
})
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

const filteredChats = computed(() => {
  const keyword = sessionKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return pageData.chats
  }
  return pageData.chats.filter((item: AnyObjDefine) =>
    String(item.chatName || '').toLowerCase().includes(keyword)
  )
})

function addChat() {
  const tmpRandom = Date.now().toString().slice(-4)
  addChatApi({
    appId: pageData.appId,
    chatName: `新会话${tmpRandom}`
  }).then(result => {
    pageData.chats.unshift(result.data)
    switchChat(result.data.id)
  })
}

function delChat(id: string) {
  deleteChatByIdApi(id).then(() => {
    pageData.chats = pageData.chats.filter((item: AnyObjDefine) => item.id !== id)
    if (activeChatId.value === id) {
      pageData.messages = []
      activeChatId.value = ''
      evtSource?.close()
      if (pageData.chats.length) {
        switchChat(pageData.chats[0].id)
      }
    }
  })
}

function switchChat(id: string) {
  activeChatId.value = id
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

function render(msgObj: AnyObjDefine) {
  return msgObj.message.startsWith('#') || msgObj.message.includes('```')
    ? md.render(msgObj.message)
    : `<span>${msgObj.message}</span>`
}

async function scrollBottom() {
  await nextTick()
  if (chatBox.value) {
    chatBox.value.scrollTop = chatBox.value.scrollHeight
  }
}

async function send() {
  const txt = pageData.crtUserInput.trim()
  if (!txt) {
    ElMessage.error({ message: '请输入聊天内容' })
    return
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
  evtSource?.close()
  sseConnected.value = false
  evtSource = new EventSource(`${BASE_URL}/chat/connect?chatId=${activeChatId.value}`)
  evtSource.onopen = () => {
    sseConnected.value = true
  }
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
  listRecentChatApi(pageData.appId).then(result => {
    if (result.data) {
      pageData.chats = result.data
      if (!activeChatId.value && pageData.chats.length) {
        switchChat(pageData.chats[0].id)
      }
    }
  })
}

function handleQueryStr() {
  const appIdFromQs = route.query.appId
  if (appIdFromQs) {
    pageData.appId = appIdFromQs as string
  }
}

function usePrompt(prompt: string) {
  pageData.crtUserInput = prompt
}

function openRenameDialog(chat: AnyObjDefine) {
  renameForm.id = chat.id
  renameForm.chatName = chat.chatName
  renameDialogVisible.value = true
}

function submitRename() {
  const chatName = renameForm.chatName.trim()
  if (!chatName) {
    ElMessage.warning('请输入会话名称')
    return
  }
  renameChatApi({
    id: renameForm.id,
    chatName
  }).then(result => {
    const target = pageData.chats.find((item: AnyObjDefine) => item.id === renameForm.id)
    if (target) {
      target.chatName = chatName
    }
    renameDialogVisible.value = false
    ElMessage.success(result.msg)
  })
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
  loadChats()
})

onUnmounted(() => evtSource?.close())
</script>
<style scoped>
.chat-page {
  min-height: calc(100vh - 190px);
}

.app {
  min-height: 78vh;
  overflow: hidden;
  border: 1px solid var(--space-border);
  border-radius: 28px;
  background: rgba(7, 16, 12, 0.68);
  box-shadow: var(--space-shadow);
  backdrop-filter: blur(18px);
}

.aside {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--space-border);
  background: linear-gradient(180deg, rgba(8, 20, 16, 0.96), rgba(10, 18, 14, 0.84));
}

.aside-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 18px 10px;
}

.aside-eyebrow {
  color: var(--space-primary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
}

.aside-title {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 800;
}

.session-hint {
  padding: 0 18px 16px;
  color: var(--space-muted);
  line-height: 1.7;
  font-size: 13px;
}

.session-search {
  padding: 0 16px 14px;
}

.session-scroll {
  flex: 1;
  padding: 0 12px 14px;
}

.session-card {
  padding: 14px;
  margin-bottom: 10px;
  border: 1px solid rgba(52, 211, 153, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
  transition: transform .2s ease, border-color .2s ease, background .2s ease;
}

.session-card:hover,
.session-card.is-active {
  transform: translateY(-2px);
  border-color: rgba(52, 211, 153, 0.36);
  background: linear-gradient(135deg, rgba(52, 211, 153, 0.14), rgba(245, 158, 11, 0.16));
}

.session-main {
  min-width: 0;
}

.session-name {
  font-weight: 700;
  color: #fff;
}

.session-meta {
  margin-top: 6px;
  color: var(--space-muted);
  font-size: 12px;
  word-break: break-all;
}

.session-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.session-btn {
  padding: 0;
  color: var(--space-muted);
}

.session-btn.delete {
  color: #ff8ca4;
}

.empty-session {
  padding: 18px 14px;
  color: var(--space-muted);
  line-height: 1.8;
}

.main {
  display: flex;
  flex-direction: column;
  padding: 0;
  background:
    radial-gradient(circle at 82% 16%, rgba(52, 211, 153, 0.12), transparent 28%),
    radial-gradient(circle at 35% 90%, rgba(245, 158, 11, 0.16), transparent 32%);
}

.chat-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px 16px;
  border-bottom: 1px solid rgba(52, 211, 153, 0.12);
}

.chat-head-title {
  font-size: 22px;
  font-weight: 800;
}

.chat-head-desc {
  margin-top: 8px;
  color: var(--space-muted);
}

.chat-head-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.meta-pill,
.tip-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(234, 246, 255, 0.84);
  font-size: 12px;
}

.meta-pill.running {
  background: rgba(46, 233, 166, 0.14);
  color: var(--space-success);
}

.scroll-btn {
  color: var(--space-primary);
}

.chat-box {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.bubble {
  display: flex;
  gap: 14px;
  margin-bottom: 18px;
}

.bubble.self {
  flex-direction: row-reverse;
}

.avatar-ball {
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  background: rgba(52, 211, 153, 0.12);
  border: 1px solid rgba(52, 211, 153, 0.26);
  color: var(--space-primary);
}

.bubble.self .avatar-ball {
  color: #03101f;
  border: 0;
  background: linear-gradient(135deg, var(--space-primary), #9c7cff);
}

.content-wrap {
  max-width: min(780px, 78%);
}

.bubble-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.bubble-role {
  color: var(--space-muted);
  font-size: 12px;
  font-weight: 700;
}

.bubble-tools {
  display: flex;
  gap: 10px;
}

.tool-btn {
  padding: 0;
  color: var(--space-primary);
}

.content {
  padding: 15px 18px;
  border: 1px solid rgba(52, 211, 153, 0.18);
  border-radius: 18px 18px 18px 8px;
  color: var(--space-text);
  background: rgba(10, 22, 18, 0.82);
  box-shadow: 0 16px 42px rgba(0, 0, 0, 0.24);
  word-break: break-word;
  line-height: 1.75;
}

.bubble.self .content {
  color: #03101f;
  border: 0;
  border-radius: 18px 18px 8px 18px;
  background: linear-gradient(135deg, var(--space-primary), #9c7cff);
  box-shadow: 0 16px 42px rgba(52, 211, 153, 0.24);
}

.bubble :deep(pre) {
  margin: 8px 0;
  padding: 12px;
  overflow: auto;
  border-radius: 12px;
  background: rgba(2, 6, 23, 0.85);
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  text-align: center;
}

.empty-chat-title {
  font-size: 28px;
  font-weight: 800;
}

.empty-chat-desc {
  max-width: 720px;
  margin-top: 14px;
  color: var(--space-muted);
  line-height: 1.9;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}

.prompt-chip {
  padding: 10px 16px;
  border: 1px solid rgba(52, 211, 153, 0.22);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--space-text);
  cursor: pointer;
  transition: transform .2s ease, border-color .2s ease;
}

.prompt-chip:hover {
  transform: translateY(-2px);
  border-color: rgba(52, 211, 153, 0.4);
}

.input-bar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px 20px;
  border-top: 1px solid var(--space-border);
  background: rgba(5, 14, 10, 0.82);
  backdrop-filter: blur(18px);
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
  font-size: 16px;
  font-weight: 700;
}

.input-tip {
  color: var(--space-muted);
  font-size: 12px;
}

.input-actions-left,
.input-actions-right {
  flex-wrap: wrap;
  gap: 10px;
}

@media (max-width: 1200px) {
  .app {
    flex-direction: column;
  }

  .aside {
    width: 100% !important;
    border-right: 0;
    border-bottom: 1px solid var(--space-border);
  }

  .session-scroll {
    max-height: 260px;
  }
}

/* 回答评价 */
.feedback-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding-left: 4px;
}

.fb-btn {
  padding: 4px 10px;
  border: 1px solid rgba(52, 211, 153, 0.15);
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
}

.fb-btn:hover {
  border-color: rgba(52, 211, 153, 0.4);
  background: rgba(52, 211, 153, 0.08);
}

.fb-btn.active-up {
  border-color: var(--space-success);
  background: rgba(16, 185, 129, 0.15);
}

.fb-btn.active-down {
  border-color: #f87171;
  background: rgba(248, 113, 113, 0.12);
}

.fb-text {
  color: var(--space-muted);
  font-size: 12px;
}
</style>
