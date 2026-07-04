import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
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
import { BASE_URL } from '@/config/constants'
import { writeClipboardText } from '@/util/clipboard'
import { normalizeAssistantMarkdown } from '@/util/markdown'
import { useUserStore } from '@/store/useUserStore'
import { chatApi, listHistoryMessageApi, listRecentChatApi } from '../api/chatApi'
import {
  autoSelectWorkspaceAppId,
  normalizeAppIdFromQuery,
  normalizeRouteChatId,
  persistLastWorkspaceAppId,
  restoreLastWorkspaceAppId
} from './useWorkspaceChatShared'
import type { ChatFeedback, WorkspaceChatMessage, WorkspaceChatSession } from '../types'

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

export function useWorkspaceChatConversationFeature() {
  const route = useRoute()
  const userStore = useUserStore()
  const chatBox = ref<HTMLElement | null>(null)
  const activeChatId = ref('')
  const sseConnected = ref(false)

  const pageData = reactive({
    chats: [] as WorkspaceChatSession[],
    appId: '',
    crtUserInput: '',
    sending: false,
    messages: [] as WorkspaceChatMessage[]
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
  let reconnectTimer: number | undefined
  let streamingMsgIndex = -1

  const currentChatName = computed(() => {
    return pageData.chats.find((item) => item.id === activeChatId.value)?.chatName || ''
  })

  const hasAppId = computed(() => String(pageData.appId || '').trim().length > 0)

  function resetReconnectTimer() {
    if (reconnectTimer) {
      window.clearTimeout(reconnectTimer)
      reconnectTimer = undefined
    }
  }

  function closeSse() {
    resetReconnectTimer()
    evtSource?.close()
    evtSource = undefined
    sseConnected.value = false
  }

  function applyRouteAppId() {
    const appIdFromQuery = normalizeAppIdFromQuery(route.query.appId)
    if (appIdFromQuery) {
      pageData.appId = appIdFromQuery
      persistLastWorkspaceAppId(appIdFromQuery)
      return
    }
    const lastAppId = restoreLastWorkspaceAppId()
    if (lastAppId) {
      pageData.appId = lastAppId
    }
  }

  function ensureAppSelected(showMessage = true) {
    if (hasAppId.value) {
      return true
    }
    closeSse()
    activeChatId.value = ''
    pageData.chats = []
    pageData.messages = []
    if (showMessage) {
      ElMessage.warning('正在自动加载应用，请稍候...')
      autoSelectApp()
    }
    return false
  }

  function renderPlainText(message: string) {
    return md.utils.escapeHtml(String(message || '')).replace(/\n/g, '<br>')
  }

  function renderMessage(message: WorkspaceChatMessage) {
    if (message.typeDesc === 'user') {
      return `<p>${renderPlainText(message.message)}</p>`
    }
    return md.render(normalizeAssistantMarkdown(String(message.message || '')))
  }

  function isStreamingMessage(message: WorkspaceChatMessage, index: number) {
    return message.typeDesc !== 'user' && pageData.sending && index === pageData.messages.length - 1
  }

  function getStreamingMessageText(message: WorkspaceChatMessage) {
    return String(message.message || '')
  }

  async function scrollBottom() {
    await nextTick()
    if (chatBox.value) {
      chatBox.value.scrollTop = chatBox.value.scrollHeight
    }
  }

  function loadMessages(chatId: string) {
    listHistoryMessageApi(chatId).then((result) => {
      if (result.data) {
        pageData.messages = (result.data as WorkspaceChatMessage[]).map((message) => ({
          ...message,
          feedback: message.feedback || '',
          html: renderMessage(message)
        }))
        scrollBottom()
      }
    })
  }

  function switchChat(chatId: string) {
    if (!chatId) {
      return
    }
    activeChatId.value = chatId
    streamingMsgIndex = -1
    closeSse()
    connectSse()
    pageData.messages = []
    loadMessages(chatId)
  }

  function pushMessage(typeDesc: 'user' | 'assistant', message: string) {
    const nextMessage: WorkspaceChatMessage = {
      typeDesc,
      message,
      feedback: '',
      html: ''
    }
    nextMessage.html = renderMessage(nextMessage)
    pageData.messages.push(nextMessage)
    scrollBottom()
  }

  function connectSse() {
    if (!ensureAppSelected(false) || !activeChatId.value) {
      return
    }
    closeSse()
    const params = new URLSearchParams({ chatId: activeChatId.value })
    if (userStore.token) {
      params.set('token', userStore.token)
    }
    evtSource = new EventSource(`${BASE_URL}/consumer/chat/connect?${params.toString()}`)
    evtSource.onopen = () => {
      sseConnected.value = true
    }
    evtSource.addEventListener('connected', () => {
      sseConnected.value = true
    })
    evtSource.onmessage = (event) => {
      if (event.data === '[DONE]') {
        streamingMsgIndex = -1
        pageData.sending = false
        scrollBottom()
        return
      }
      if (streamingMsgIndex === -1) {
        pageData.messages.push({ typeDesc: 'assistant', message: '', html: '', feedback: '' })
        streamingMsgIndex = pageData.messages.length - 1
      }
      const target = pageData.messages[streamingMsgIndex]
      target.message += event.data
      target.html = renderMessage(target)
      scrollBottom()
    }
    evtSource.onerror = () => {
      sseConnected.value = false
      evtSource?.close()
      evtSource = undefined
      resetReconnectTimer()
      reconnectTimer = window.setTimeout(() => {
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
    listRecentChatApi(pageData.appId).then((result) => {
      if (!result.data) {
        return
      }
      pageData.chats = result.data as WorkspaceChatSession[]
      const routeChatId = normalizeRouteChatId(route.params.chatId)
      if (routeChatId) {
        const existed = pageData.chats.find((item) => String(item.id) === routeChatId)
        if (existed) {
          switchChat(routeChatId)
          return
        }
      }
      if (!activeChatId.value && pageData.chats.length) {
        switchChat(String(pageData.chats[0].id))
      }
    })
  }

  function usePrompt(prompt: string) {
    pageData.crtUserInput = prompt
  }

  async function autoSelectApp() {
    const appId = await autoSelectWorkspaceAppId({
      onSelected: (nextAppId) => {
        pageData.appId = nextAppId
      }
    })
    if (appId) {
      pageData.appId = appId
      loadChats()
    }
  }

  async function copyMessage(message: string) {
    try {
      await writeClipboardText(message)
      ElMessage.success('已复制到剪贴板')
    } catch {
      ElMessage.error('复制失败')
    }
  }

  function retryMessage(message: string) {
    pageData.crtUserInput = message
  }

  function setFeedback(index: number, type: ChatFeedback) {
    const target = pageData.messages[index]
    if (!target) {
      return
    }
    target.feedback = target.feedback === type ? '' : type
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
    pageData.messages.forEach((message) => {
      const role = message.typeDesc === 'user' ? '🧑 **你**' : '🤖 **问答助手**'
      lines.push(role)
      lines.push('')
      lines.push(message.message)
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

  function handleEditorKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      send()
    }
  }

  function send() {
    if (!ensureAppSelected()) {
      return
    }
    const text = pageData.crtUserInput.trim()
    if (!text) {
      ElMessage.error({ message: '请输入聊天内容' })
      return
    }
    if (!activeChatId.value) {
      ElMessage.warning('当前没有可用会话，请先返回会话列表创建会话')
      return
    }
    if (!sseConnected.value) {
      connectSse()
    }
    pageData.crtUserInput = ''
    pushMessage('user', text)
    pageData.sending = true
    chatApi({
      chatId: activeChatId.value,
      userInput: text
    }).catch(() => {
      pageData.sending = false
    })
  }

  watch(() => route.query.appId, () => {
    applyRouteAppId()
    if (pageData.appId) {
      loadChats()
    }
  })

  watch(() => route.params.chatId, (chatId) => {
    const normalizedChatId = normalizeRouteChatId(chatId)
    if (!normalizedChatId) {
      return
    }
    if (normalizedChatId !== activeChatId.value) {
      switchChat(normalizedChatId)
    }
  })

  onMounted(() => {
    applyRouteAppId()
    if (hasAppId.value) {
      loadChats()
      return
    }
    autoSelectApp()
  })

  onUnmounted(() => {
    closeSse()
  })

  return {
    chatBox,
    activeChatId,
    sseConnected,
    pageData,
    quickPrompts,
    currentChatName,
    renderMessage,
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
  }
}
