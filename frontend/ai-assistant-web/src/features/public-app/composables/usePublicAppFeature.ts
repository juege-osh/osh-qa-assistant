import { storeToRefs } from 'pinia'
import { computed, inject, nextTick, onMounted, onUnmounted, provide, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownIt from 'markdown-it'
import { useRoute, useRouter } from 'vue-router'
import { BASE_URL } from '@/config/constants'
import { useResource } from '@/hooks/useResource'
import { useUserStore } from '@/store/useUserStore'
import { getImage } from '@/util/AssetsImageUtil'
import { queryPublicAppDetailApi, verifyPublicAppPasswordApi } from '../api/publicAppApi'
import { usePublicAppSessionStore } from '../stores/usePublicAppSessionStore'
import type { PublicAppDetail, PublicChatSession, PublicMessage, PublicPasswordForm } from '../types'

const ACCESS_TOKEN_STORAGE_PREFIX = 'public-app-access-token:'
const PUBLIC_BROWSER_SCOPE_KEY = 'public-app-browser-scope'
const PUBLIC_SESSION_STORAGE_PREFIX = 'public-app:sessions:'
const PUBLIC_ACTIVE_SESSION_PREFIX = 'public-app:active-session:'
const STREAM_END = '[DONE]'

const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true
})

const publicAppFeatureKey = Symbol('public-app-feature')

function nowIso() {
  return new Date().toISOString()
}

function createId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function createSession(title = '新会话'): PublicChatSession {
  const createdAt = nowIso()
  return {
    id: createId(),
    title,
    createdAt,
    updatedAt: createdAt,
    messages: []
  }
}

function normalizeMessage(message: unknown): PublicMessage | null {
  if (!message || typeof message !== 'object') {
    return null
  }
  const current = message as Record<string, unknown>
  return {
    id: String(current.id || createId()),
    role: current.role === 'user' ? 'user' : 'assistant',
    content: String(current.content || ''),
    createdAt: String(current.createdAt || nowIso())
  }
}

function normalizeSession(session: unknown): PublicChatSession | null {
  if (!session || typeof session !== 'object') {
    return null
  }
  const current = session as Record<string, unknown>
  const messages = Array.isArray(current.messages)
    ? current.messages.map(normalizeMessage).filter((item): item is PublicMessage => Boolean(item))
    : []
  return {
    id: String(current.id || createId()),
    title: String(current.title || '新会话'),
    createdAt: String(current.createdAt || nowIso()),
    updatedAt: String(current.updatedAt || current.createdAt || nowIso()),
    messages
  }
}

export function createPublicAppFeatureModel() {
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()
  const sessionStore = usePublicAppSessionStore()
  const { sessions, activeSessionId } = storeToRefs(sessionStore)
  const { toAddressable } = useResource()

  const passwordFormRef = ref<FormInstance>()
  const chatBoxRef = ref<HTMLElement | null>(null)
  const chatPanelRef = ref<HTMLElement | null>(null)
  const showAppInfoDialog = ref(false)
  const renameDialogVisible = ref(false)
  const renameDraft = ref('')
  const renameSessionId = ref('')

  const detail = reactive<PublicAppDetail>({
    slug: '',
    appName: '',
    appDesc: '',
    iconPath: '',
    accessType: 'PUBLIC',
    passwordRequired: false
  })

  const passwordForm = reactive<PublicPasswordForm>({
    accessPassword: ''
  })

  const passwordRules = reactive<FormRules<PublicPasswordForm>>({
    accessPassword: [{ required: true, message: '请输入访问密码', trigger: 'blur' }]
  })

  const userInput = ref('')
  const sending = ref(false)
  const passwordLoading = ref(false)
  const loadError = ref('')
  const accessToken = ref('')
  const browserScopeId = ref('')
  const streamingSessionId = ref('')
  const streamingMessageId = ref('')
  const streamingDisplayContent = ref('')
  const streamingTargetContent = ref('')

  const suggestedPrompts = [
    '请先概括这个应用最适合回答哪些问题。',
    '请根据当前资料回答一个最常见的业务问题。',
    '如果资料里没有答案，你会怎么说明？',
    '请用通俗易懂的方式解释核心流程。',
    '请列出这类问题回答时最需要注意的关键点。'
  ]

  let persistTimer: number | undefined
  let detailRequestToken = 0
  let passwordVerifyToken = 0
  let activeChatRequestToken = 0
  let activeChatController: AbortController | null = null
  let streamingPlaybackTimer: number | undefined
  let shouldAutoScrollStream = true

  const routeSlug = computed(() => String(route.params.slug || '').trim())
  const iconUrl = computed(() => detail.iconPath ? toAddressable(detail.iconPath) : getImage('default.png'))
  const accessModeLabel = computed(() => detail.passwordRequired ? '密码访问' : '公开访问')
  const isLoggedIn = computed(() => Boolean(userStore.token))
  const identityScope = computed(() => {
    if (isLoggedIn.value) {
      return `user:${userStore.userInfo.id || userStore.userInfo.username || 'logged'}`
    }
    return browserScopeId.value ? `guest:${browserScopeId.value}` : ''
  })
  const identityLabel = computed(() => isLoggedIn.value ? '当前身份 已登录用户' : '当前身份 浏览器访客')
  const activeSession = computed(() => sessions.value.find((item) => item.id === activeSessionId.value) || null)
  const activeSessionMessages = computed(() => activeSession.value?.messages || [])
  const activeSessionMessageCount = computed(() => activeSessionMessages.value.length)
  const needsPasswordAuth = computed(() => detail.passwordRequired && !accessToken.value)
  const interactionLocked = computed(() => sending.value || passwordLoading.value)
  const sessionStorageKey = computed(() => {
    if (!routeSlug.value || !identityScope.value) {
      return ''
    }
    return `${PUBLIC_SESSION_STORAGE_PREFIX}${routeSlug.value}:${identityScope.value}`
  })
  const activeSessionStorageKey = computed(() => {
    if (!routeSlug.value || !identityScope.value) {
      return ''
    }
    return `${PUBLIC_ACTIVE_SESSION_PREFIX}${routeSlug.value}:${identityScope.value}`
  })
  const composerTipText = computed(() => {
    if (loadError.value) {
      return '当前公开链接不可用，请先重试。'
    }
    if (needsPasswordAuth.value) {
      return '当前应用需要访问密码，验证通过后即可继续提问。'
    }
    return '当前公开页历史保存在本地浏览器，会按应用和访问身份隔离。'
  })
  const sendDisabled = computed(() => {
    return interactionLocked.value || !userInput.value.trim() || Boolean(loadError.value) || needsPasswordAuth.value
  })

  function formatMessageTime(value: string) {
    const date = new Date(value)
    const hour = String(date.getHours()).padStart(2, '0')
    const minute = String(date.getMinutes()).padStart(2, '0')
    return `${hour}:${minute}`
  }

  function formatSessionTime(value: string) {
    const date = new Date(value)
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hour = String(date.getHours()).padStart(2, '0')
    const minute = String(date.getMinutes()).padStart(2, '0')
    return `${month}-${day} ${hour}:${minute}`
  }

  function resetRenameDialogState() {
    renameDialogVisible.value = false
    renameSessionId.value = ''
    renameDraft.value = ''
  }

  function ensureBrowserScopeId() {
    const stored = window.localStorage.getItem(PUBLIC_BROWSER_SCOPE_KEY)
    if (stored) {
      browserScopeId.value = stored
      return
    }
    const nextId = createId()
    window.localStorage.setItem(PUBLIC_BROWSER_SCOPE_KEY, nextId)
    browserScopeId.value = nextId
  }

  function buildAccessTokenStorageKey(slug: string) {
    return `${ACCESS_TOKEN_STORAGE_PREFIX}${slug}`
  }

  function restoreAccessToken() {
    const slug = routeSlug.value
    accessToken.value = slug ? window.sessionStorage.getItem(buildAccessTokenStorageKey(slug)) || '' : ''
  }

  function clearAccessToken() {
    if (routeSlug.value) {
      window.sessionStorage.removeItem(buildAccessTokenStorageKey(routeSlug.value))
    }
    accessToken.value = ''
    passwordForm.accessPassword = ''
  }

  function persistSessions() {
    if (!sessionStorageKey.value) {
      return
    }
    window.localStorage.setItem(sessionStorageKey.value, JSON.stringify(sessions.value))
    if (!activeSessionStorageKey.value) {
      return
    }
    if (activeSessionId.value) {
      window.localStorage.setItem(activeSessionStorageKey.value, activeSessionId.value)
      return
    }
    window.localStorage.removeItem(activeSessionStorageKey.value)
  }

  function schedulePersistSessions() {
    if (persistTimer) {
      window.clearTimeout(persistTimer)
    }
    persistTimer = window.setTimeout(() => {
      persistTimer = undefined
      persistSessions()
    }, 120)
  }

  function applyLoadedSessions(loadedSessions: PublicChatSession[]) {
    sessionStore.setSessions(
      [...loadedSessions].sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    )

    if (renameSessionId.value && !sessions.value.some((item) => item.id === renameSessionId.value)) {
      resetRenameDialogState()
    }

    const savedActiveId = activeSessionStorageKey.value
      ? window.localStorage.getItem(activeSessionStorageKey.value) || ''
      : ''

    if (savedActiveId && sessions.value.some((item) => item.id === savedActiveId)) {
      sessionStore.setActiveSessionId(savedActiveId)
      return
    }

    if (sessions.value.length) {
      sessionStore.setActiveSessionId(sessions.value[0].id)
      return
    }

    const firstSession = createSession()
    sessionStore.setSessions([firstSession])
    sessionStore.setActiveSessionId(firstSession.id)
    persistSessions()
  }

  function loadSessions() {
    if (!sessionStorageKey.value) {
      sessionStore.reset()
      return
    }

    const raw = window.localStorage.getItem(sessionStorageKey.value)
    if (!raw) {
      applyLoadedSessions([])
      return
    }

    try {
      const parsed = JSON.parse(raw)
      if (!Array.isArray(parsed)) {
        applyLoadedSessions([])
        return
      }
      applyLoadedSessions(parsed.map(normalizeSession).filter((item): item is PublicChatSession => Boolean(item)))
    } catch {
      applyLoadedSessions([])
    }
  }

  function resetDetailState() {
    detail.slug = ''
    detail.appName = ''
    detail.appDesc = ''
    detail.iconPath = ''
    detail.accessType = 'PUBLIC'
    detail.passwordRequired = false
  }

  function clearStreamingPlaybackTimer() {
    if (streamingPlaybackTimer) {
      window.clearTimeout(streamingPlaybackTimer)
      streamingPlaybackTimer = undefined
    }
  }

  function resetStreamingState() {
    clearStreamingPlaybackTimer()
    streamingSessionId.value = ''
    streamingMessageId.value = ''
    streamingDisplayContent.value = ''
    streamingTargetContent.value = ''
  }

  function flushStreamingPlayback() {
    streamingPlaybackTimer = undefined
    if (streamingDisplayContent.value.length >= streamingTargetContent.value.length) {
      return
    }
    const remaining = streamingTargetContent.value.length - streamingDisplayContent.value.length
    const step = remaining > 120 ? 12 : remaining > 48 ? 6 : remaining > 18 ? 3 : 1
    streamingDisplayContent.value = streamingTargetContent.value.slice(
      0,
      Math.min(streamingTargetContent.value.length, streamingDisplayContent.value.length + step)
    )
    if (streamingDisplayContent.value.length < streamingTargetContent.value.length) {
      streamingPlaybackTimer = window.setTimeout(flushStreamingPlayback, remaining > 80 ? 14 : 22)
    }
  }

  function ensureStreamingPlayback() {
    if (streamingPlaybackTimer || streamingDisplayContent.value.length >= streamingTargetContent.value.length) {
      return
    }
    streamingPlaybackTimer = window.setTimeout(flushStreamingPlayback, 16)
  }

  function finishStreamingMessage(sessionId: string, messageId: string) {
    if (streamingSessionId.value !== sessionId || streamingMessageId.value !== messageId) {
      return
    }
    resetStreamingState()
  }

  function cancelActiveChat(resetSending = true) {
    activeChatRequestToken += 1
    if (activeChatController) {
      activeChatController.abort()
      activeChatController = null
    }
    resetStreamingState()
    if (resetSending) {
      sending.value = false
    }
  }

  function invalidatePendingRequests() {
    detailRequestToken += 1
    passwordVerifyToken += 1
    cancelActiveChat()
    passwordLoading.value = false
  }

  function moveSessionToTop(sessionId: string) {
    const index = sessions.value.findIndex((item) => item.id === sessionId)
    if (index <= 0) {
      return
    }
    const [session] = sessions.value.splice(index, 1)
    sessions.value.unshift(session)
  }

  function mutateSession(sessionId: string, updater: (session: PublicChatSession) => void) {
    const target = sessions.value.find((item) => item.id === sessionId)
    if (!target) {
      return
    }
    updater(target)
    target.updatedAt = nowIso()
    moveSessionToTop(sessionId)
    schedulePersistSessions()
  }

  function renderMessage(message: PublicMessage) {
    if (message.role === 'user') {
      return `<p>${md.utils.escapeHtml(String(message.content || '')).replace(/\n/g, '<br>')}</p>`
    }
    return md.render(String(message.content || ''))
  }

  function selectSession(sessionId: string) {
    if (interactionLocked.value) {
      return
    }
    if (!sessions.value.some((item) => item.id === sessionId)) {
      return
    }
    sessionStore.setActiveSessionId(sessionId)
    persistSessions()
    nextTick(() => {
      if (chatBoxRef.value) {
        chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
      }
    })
    scrollPageToChatPanel()
  }

  function openRenameDialog(session: PublicChatSession) {
    if (interactionLocked.value) {
      return
    }
    renameSessionId.value = session.id
    renameDraft.value = session.title
    renameDialogVisible.value = true
  }

  function confirmRenameSession() {
    const nextTitle = renameDraft.value.trim()
    if (!renameSessionId.value) {
      resetRenameDialogState()
      return
    }
    if (!nextTitle) {
      ElMessage.warning('请输入会话名称')
      return
    }
    if (!sessions.value.some((item) => item.id === renameSessionId.value)) {
      resetRenameDialogState()
      ElMessage.warning('当前会话已不存在，请刷新后重试')
      return
    }
    mutateSession(renameSessionId.value, (current) => {
      current.title = nextTitle
    })
    resetRenameDialogState()
    ElMessage.success('会话名称已更新')
  }

  function startNewConversation() {
    if (interactionLocked.value) {
      return
    }
    const reusableSession = sessions.value.find((session) => session.messages.length === 0 && session.title === '新会话')
    if (reusableSession) {
      sessionStore.setActiveSessionId(reusableSession.id)
      moveSessionToTop(reusableSession.id)
      userInput.value = ''
      persistSessions()
      nextTick(() => {
        if (chatBoxRef.value) {
          chatBoxRef.value.scrollTop = 0
        }
      })
      scrollPageToChatPanel()
      return
    }
    const session = createSession()
    sessions.value.unshift(session)
    sessionStore.setActiveSessionId(session.id)
    userInput.value = ''
    persistSessions()
    nextTick(() => {
      if (chatBoxRef.value) {
        chatBoxRef.value.scrollTop = 0
      }
    })
    scrollPageToChatPanel()
  }

  async function deleteSession(sessionId: string) {
    if (interactionLocked.value) {
      return
    }
    const target = sessions.value.find((item) => item.id === sessionId)
    if (!target) {
      return
    }

    try {
      await ElMessageBox.confirm(
        `确定删除会话“${target.title}”吗？删除后无法恢复。`,
        '删除会话',
        {
          type: 'warning',
          confirmButtonText: '删除',
          cancelButtonText: '取消'
        }
      )
    } catch {
      return
    }

    const nextSessions = sessions.value.filter((item) => item.id !== sessionId)
    sessionStore.setSessions(nextSessions)

    if (!nextSessions.length) {
      const fallbackSession = createSession()
      sessionStore.setSessions([fallbackSession])
      sessionStore.setActiveSessionId(fallbackSession.id)
    } else if (activeSessionId.value === sessionId) {
      sessionStore.setActiveSessionId(nextSessions[0].id)
    }

    if (renameSessionId.value === sessionId) {
      resetRenameDialogState()
    }

    persistSessions()
    ElMessage.success('会话已删除')
  }

  function usePrompt(prompt: string) {
    userInput.value = prompt
  }

  function goLogin() {
    router.push('/login')
  }

  function goRegister() {
    router.push('/register')
  }

  function jumpToWorkspace() {
    if (userStore.userInfo.role === 'ADMIN') {
      router.push('/admin/manager/manage')
      return
    }
    router.push('/workspace/app/manage')
  }

  function ensureActiveSession() {
    if (activeSession.value) {
      return activeSession.value
    }
    const session = createSession()
    sessions.value.unshift(session)
    sessionStore.setActiveSessionId(session.id)
    persistSessions()
    return session
  }

  function buildSessionTitle(content: string) {
    const trimmed = content.trim()
    if (!trimmed) {
      return '新会话'
    }
    return trimmed.length > 18 ? `${trimmed.slice(0, 18)}...` : trimmed
  }

  function appendAssistantContent(sessionId: string, messageSeed: PublicMessage, chunk: string) {
    mutateSession(sessionId, (current) => {
      const target = current.messages.find((item) => item.id === messageSeed.id)
      if (target) {
        target.content += chunk
        return
      }
      current.messages.push({
        ...messageSeed,
        content: chunk
      })
    })
    if (streamingSessionId.value === sessionId && streamingMessageId.value === messageSeed.id) {
      streamingTargetContent.value += chunk
      ensureStreamingPlayback()
    }
  }

  function appendAssistantError(sessionId: string, messageSeed: PublicMessage, fallbackMessage: string) {
    mutateSession(sessionId, (current) => {
      const target = current.messages.find((item) => item.id === messageSeed.id)
      if (target) {
        if (!target.content.includes(fallbackMessage)) {
          target.content += target.content ? `\n\n${fallbackMessage}` : fallbackMessage
        }
        return
      }
      current.messages.push({
        ...messageSeed,
        content: fallbackMessage
      })
    })
    if (streamingSessionId.value === sessionId && streamingMessageId.value === messageSeed.id) {
      streamingTargetContent.value = streamingTargetContent.value
        ? `${streamingTargetContent.value}\n\n${fallbackMessage}`
        : fallbackMessage
      ensureStreamingPlayback()
    }
  }

  function shouldResetAccessTokenOnChatError(message: string) {
    return message.includes('访问凭证已失效') || message.includes('请先完成访问密码验证')
  }

  async function scrollBottom() {
    await nextTick()
    if (chatBoxRef.value) {
      chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
    }
  }

  function isNearChatBottom(offset = 120) {
    if (!chatBoxRef.value) {
      return true
    }
    const { scrollTop, clientHeight, scrollHeight } = chatBoxRef.value
    return scrollHeight - (scrollTop + clientHeight) <= offset
  }

  async function scrollBottomIfNeeded(force = false) {
    if (force || shouldAutoScrollStream || isNearChatBottom()) {
      await scrollBottom()
    }
  }

  async function scrollPageToChatPanel() {
    await nextTick()
    if (typeof window === 'undefined' || window.innerWidth > 1180 || !chatPanelRef.value) {
      return
    }
    const container = document.querySelector('.content-box') as HTMLElement | null
    if (!container) {
      return
    }
    const containerRect = container.getBoundingClientRect()
    const chatRect = chatPanelRef.value.getBoundingClientRect()
    const nextTop = container.scrollTop + chatRect.top - containerRect.top
    container.scrollTo({
      top: Math.max(0, nextTop - 12),
      behavior: 'smooth'
    })
  }

  function loadDetail() {
    const slug = routeSlug.value
    if (!slug) {
      detailRequestToken += 1
      resetDetailState()
      clearAccessToken()
      loadError.value = '公开访问标识缺失'
      return
    }
    const requestToken = ++detailRequestToken
    loadError.value = ''
    queryPublicAppDetailApi(slug).then((result) => {
      if (requestToken !== detailRequestToken) {
        return
      }
      const data = result.data || {}
      detail.slug = String(data.slug || slug)
      detail.appName = String(data.appName || '')
      detail.appDesc = String(data.appDesc || '')
      detail.iconPath = String(data.iconPath || '')
      detail.accessType = String(data.accessType || 'PUBLIC')
      detail.passwordRequired = Boolean(data.passwordRequired)
      loadError.value = ''
      restoreAccessToken()
      if (!detail.passwordRequired) {
        clearAccessToken()
      }
    }).catch((error) => {
      if (requestToken !== detailRequestToken) {
        return
      }
      resetDetailState()
      clearAccessToken()
      loadError.value = error?.msg || '公开应用不存在或已关闭'
    })
  }

  function verifyPassword() {
    passwordFormRef.value?.validate((valid: boolean) => {
      if (!valid) {
        return
      }
      const requestToken = ++passwordVerifyToken
      passwordLoading.value = true
      verifyPublicAppPasswordApi({
        slug: routeSlug.value,
        accessPassword: passwordForm.accessPassword
      }).then((result) => {
        if (requestToken !== passwordVerifyToken) {
          return
        }
        accessToken.value = String(result.data?.accessToken || '')
        if (routeSlug.value && accessToken.value) {
          window.sessionStorage.setItem(buildAccessTokenStorageKey(routeSlug.value), accessToken.value)
        }
        passwordForm.accessPassword = ''
        ElMessage.success('访问密码验证通过')
      }).catch((error) => {
        if (requestToken !== passwordVerifyToken) {
          return
        }
        accessToken.value = ''
        ElMessage.error(error?.msg || '访问密码验证失败')
      }).finally(() => {
        if (requestToken === passwordVerifyToken) {
          passwordLoading.value = false
        }
      })
    })
  }

  async function sendMessage() {
    if (interactionLocked.value) {
      return
    }
    const content = userInput.value.trim()
    if (!content) {
      ElMessage.warning('请输入问题后再发送')
      return
    }
    if (loadError.value) {
      ElMessage.warning('当前公开链接不可用，请先重试')
      return
    }
    if (needsPasswordAuth.value) {
      ElMessage.warning('请先完成访问密码验证')
      return
    }

    const session = ensureActiveSession()
    const userMessage: PublicMessage = {
      id: createId(),
      role: 'user',
      content,
      createdAt: nowIso()
    }
    const assistantMessage: PublicMessage = {
      id: createId(),
      role: 'assistant',
      content: '',
      createdAt: nowIso()
    }

    mutateSession(session.id, (current) => {
      const shouldResetTitle = current.title === '新会话' || current.messages.length === 0
      current.messages.push(userMessage)
      if (shouldResetTitle) {
        current.title = buildSessionTitle(content)
      }
    })

    userInput.value = ''
    const requestToken = activeChatRequestToken + 1
    cancelActiveChat(false)
    activeChatRequestToken = requestToken
    const controller = new AbortController()
    activeChatController = controller
    streamingSessionId.value = session.id
    streamingMessageId.value = assistantMessage.id
    streamingDisplayContent.value = ''
    streamingTargetContent.value = ''
    shouldAutoScrollStream = isNearChatBottom()
    sending.value = true
    await scrollBottom()

    try {
      const response = await fetch(`${BASE_URL}/consumer/public/app/chat`, {
        method: 'POST',
        signal: controller.signal,
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          slug: routeSlug.value,
          visitorId: session.id,
          userInput: content,
          accessToken: accessToken.value || undefined
        })
      })
      if (!response.ok || !response.body) {
        throw new Error(`公开聊天请求失败(${response.status})`)
      }
      const contentType = response.headers.get('content-type') || ''
      if (contentType.includes('application/json')) {
        const payload = await response.json()
        throw new Error(payload?.msg || '公开聊天失败，请稍后重试')
      }
      await consumeSseStream(response.body, session.id, assistantMessage, requestToken)
    } catch (error: any) {
      if (controller.signal.aborted || requestToken !== activeChatRequestToken) {
        return
      }
      const fallbackMessage = error?.message || '公开聊天失败，请稍后重试'
      appendAssistantError(session.id, assistantMessage, fallbackMessage)
      if (detail.passwordRequired && shouldResetAccessTokenOnChatError(fallbackMessage)) {
        clearAccessToken()
      }
      ElMessage.error(fallbackMessage)
    } finally {
      if (requestToken === activeChatRequestToken) {
        finishStreamingMessage(session.id, assistantMessage.id)
        activeChatController = null
        sending.value = false
        persistSessions()
        await scrollBottom()
      }
    }
  }

  async function consumeSseStream(
    stream: ReadableStream<Uint8Array>,
    sessionId: string,
    assistantMessage: PublicMessage,
    requestToken: number
  ) {
    const reader = stream.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    while (true) {
      const { value, done } = await reader.read()
      if (requestToken !== activeChatRequestToken) {
        return
      }
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      const frames = buffer.split('\n\n')
      buffer = frames.pop() || ''
      for (const frame of frames) {
        const data = parseSseFrame(frame)
        if (!data) {
          continue
        }
        if (data === STREAM_END) {
          if (requestToken === activeChatRequestToken) {
            persistSessions()
          }
          return
        }
        appendAssistantContent(sessionId, assistantMessage, data)
        await scrollBottomIfNeeded()
      }
    }
    const tailData = parseSseFrame(buffer)
    if (requestToken === activeChatRequestToken && tailData && tailData !== STREAM_END) {
      appendAssistantContent(sessionId, assistantMessage, tailData)
      await scrollBottomIfNeeded()
    }
  }

  function parseSseFrame(frame: string) {
    return frame
      .split('\n')
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).replace(/^ /, ''))
      .join('\n')
  }

  function handleStorageChange(event: StorageEvent) {
    if (event.storageArea !== window.localStorage) {
      return
    }
    if (event.key === PUBLIC_BROWSER_SCOPE_KEY && !isLoggedIn.value) {
      browserScopeId.value = event.newValue || browserScopeId.value
      return
    }
    if (!routeSlug.value || !identityScope.value) {
      return
    }
    if (event.key === sessionStorageKey.value || event.key === activeSessionStorageKey.value) {
      loadSessions()
    }
  }

  function handleEditorKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      if (!sendDisabled.value) {
        sendMessage()
      }
    }
  }

  function isStreamingMessage(message: PublicMessage) {
    return message.role === 'assistant'
      && message.id === streamingMessageId.value
      && activeSessionId.value === streamingSessionId.value
      && sending.value
  }

  function getStreamingMessageText(message: PublicMessage) {
    return isStreamingMessage(message) ? streamingDisplayContent.value : message.content
  }

  watch(routeSlug, () => {
    invalidatePendingRequests()
    resetRenameDialogState()
    if (!routeSlug.value) {
      sessionStore.reset()
      userInput.value = ''
      resetDetailState()
      clearAccessToken()
      loadError.value = '公开访问标识缺失'
      return
    }
    loadDetail()
  }, { immediate: true })

  watch([routeSlug, identityScope], () => {
    if (!routeSlug.value) {
      return
    }
    resetRenameDialogState()
    userInput.value = ''
    cancelActiveChat()
    passwordVerifyToken += 1
    passwordLoading.value = false
    loadSessions()
  }, { immediate: true })

  onMounted(() => {
    ensureBrowserScopeId()
    window.addEventListener('storage', handleStorageChange)
  })

  onUnmounted(() => {
    window.removeEventListener('storage', handleStorageChange)
    invalidatePendingRequests()
    if (persistTimer) {
      window.clearTimeout(persistTimer)
      persistTimer = undefined
    }
    persistSessions()
    sessionStore.reset()
  })

  return {
    passwordFormRef,
    chatBoxRef,
    chatPanelRef,
    showAppInfoDialog,
    renameDialogVisible,
    renameDraft,
    detail,
    passwordForm,
    passwordRules,
    userInput,
    sending,
    passwordLoading,
    loadError,
    sessions,
    activeSessionId,
    suggestedPrompts,
    routeSlug,
    iconUrl,
    accessModeLabel,
    isLoggedIn,
    identityLabel,
    activeSessionMessages,
    activeSessionMessageCount,
    needsPasswordAuth,
    interactionLocked,
    composerTipText,
    sendDisabled,
    formatMessageTime,
    formatSessionTime,
    isStreamingMessage,
    getStreamingMessageText,
    resetRenameDialogState,
    renderMessage,
    selectSession,
    openRenameDialog,
    confirmRenameSession,
    startNewConversation,
    deleteSession,
    usePrompt,
    goLogin,
    goRegister,
    jumpToWorkspace,
    loadDetail,
    verifyPassword,
    sendMessage,
    handleEditorKeydown
  }
}

export type PublicAppFeatureModel = ReturnType<typeof createPublicAppFeatureModel>

export function providePublicAppFeatureModel() {
  const model = createPublicAppFeatureModel()
  provide(publicAppFeatureKey, model)
  return model
}

export function usePublicAppFeatureModel() {
  const model = inject<PublicAppFeatureModel>(publicAppFeatureKey)
  if (!model) {
    throw new Error('PublicAppFeatureModel is not provided')
  }
  return model
}
