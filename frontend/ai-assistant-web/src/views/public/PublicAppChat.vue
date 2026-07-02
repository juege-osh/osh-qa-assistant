<template>
  <div class="public-page">
    <section class="public-shell">
      <template v-if="showChatLayout">
        <section class="public-live-layout">
          <aside class="public-sidecard">
            <div class="side-kicker">公开应用</div>
            <div class="side-brand">
              <el-avatar :src="iconUrl" :size="36"></el-avatar>
              <div class="side-brand-copy">
                <h1>{{ detail.appName || '公开问答入口' }}</h1>
                <p>{{ detail.appDesc || '直接提问即可。' }}</p>
              </div>
            </div>
            <div class="side-meta">
              <span class="meta-chip">访问方式：{{ detail.passwordRequired ? '密码访问' : '公开访问' }}</span>
              <span class="meta-chip">访客会话：{{ visitorIdShort }}</span>
            </div>
            <div class="side-note">
              {{ detail.passwordRequired ? '已验证，可继续提问。' : '现在可以直接开始聊天。' }}
            </div>
          </aside>

          <section class="chat-shell">
            <div ref="chatBoxRef" class="chat-stream">
              <template v-if="messages.length">
                <div
                  v-for="(message, index) in messages"
                  :key="index"
                  :class="['message-row', message.role === 'user' ? 'message-row--user' : 'message-row--assistant']"
                >
                  <div class="message-avatar">{{ message.role === 'user' ? '访客' : 'AI' }}</div>
                  <div class="message-bubble">
                    <div class="message-role">{{ message.role === 'user' ? '你' : detail.appName || '公开应用' }}</div>
                    <div class="message-content" v-html="renderMessage(message)"></div>
                  </div>
                </div>
              </template>
              <div v-else class="empty-chat">
                <div class="state-title">开始你的第一轮对话</div>
                <div class="state-desc">还没有对话，先发一个问题。</div>
              </div>
            </div>

            <div class="composer">
              <div class="composer-head">
                <div>
                  <div class="state-title">发起提问</div>
                  <div class="state-desc">回答会实时返回。</div>
                </div>
                <el-button
                  v-if="detail.passwordRequired"
                  text
                  class="reset-btn"
                  @click="clearAccessToken"
                >
                  重新验证密码
                </el-button>
              </div>
              <el-input
                v-model="userInput"
                type="textarea"
                :rows="3"
                :disabled="sending"
                placeholder="请输入问题、业务背景或要验证的场景"
                @keydown="handleEditorKeydown"
              ></el-input>
              <div class="composer-actions">
                <div class="composer-tip">{{ sending ? '正在生成回答...' : 'Enter 发送，Shift + Enter 换行' }}</div>
                <div class="composer-buttons">
                  <el-button :disabled="sending" @click="userInput = ''">清空</el-button>
                  <el-button type="primary" :loading="sending" @click="sendMessage">发送</el-button>
                </div>
              </div>
            </div>
          </section>
        </section>
      </template>

      <template v-else>
        <div class="public-hero">
          <div class="hero-copy">
            <div class="hero-kicker">公开应用</div>
            <h1>{{ detail.appName || '公开问答入口' }}</h1>
            <p>{{ detail.appDesc || '输入密码后即可开始使用。' }}</p>
            <div class="hero-meta">
              <span class="meta-chip">访问方式：{{ detail.passwordRequired ? '密码访问' : '公开访问' }}</span>
              <span class="meta-chip">访客会话：{{ visitorIdShort }}</span>
            </div>
          </div>
          <div class="hero-card">
            <el-avatar :src="iconUrl" :size="64"></el-avatar>
            <div class="hero-card-title">{{ detail.appName || routeSlug }}</div>
            <div class="hero-card-desc">
              {{ detail.passwordRequired ? '先验证访问密码，再开始聊天。' : '可直接开始聊天。' }}
            </div>
          </div>
        </div>

        <section v-if="loadError" class="state-panel error-panel">
          <div class="state-title">当前公开链接不可用</div>
          <div class="state-desc">{{ loadError }}</div>
          <el-button type="primary" @click="loadDetail">重试</el-button>
        </section>

        <section v-else class="state-panel password-panel">
          <div class="state-title">输入访问密码</div>
          <div class="state-desc">这个应用需要密码，验证后即可进入对话。</div>
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
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
            <el-button type="primary" :loading="passwordLoading" @click="verifyPassword">验证并进入</el-button>
          </el-form>
        </section>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts" name="PublicAppChat">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import { BASE_URL } from '@/config/constants'
import { queryPublicAppDetailApi, verifyPublicAppPasswordApi } from '@/api/publicAppApi'
import { getImage } from '@/util/AssetsImageUtil'
import { useResource } from '@/hooks/useResource'

type PublicMessage = {
  role: 'user' | 'assistant',
  content: string
}

const ACCESS_TOKEN_STORAGE_PREFIX = 'public-app-access-token:'
const VISITOR_ID_STORAGE_PREFIX = 'public-app-visitor-id:'
const STREAM_END = '[DONE]'

const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true
})

const route = useRoute()
const { toAddressable } = useResource()
const passwordFormRef = ref()
const chatBoxRef = ref<HTMLElement | null>(null)

const detail = reactive({
  slug: '',
  appName: '',
  appDesc: '',
  iconPath: '',
  accessType: 'PUBLIC',
  passwordRequired: false
})

const passwordForm = reactive({
  accessPassword: ''
})

const passwordRules = reactive({
  accessPassword: [{ required: true, message: '请输入访问密码', trigger: 'blur' }]
})

const userInput = ref('')
const sending = ref(false)
const passwordLoading = ref(false)
const loadError = ref('')
const accessToken = ref('')
const messages = ref<PublicMessage[]>([])
const visitorId = ref('')

const routeSlug = computed(() => String(route.params.slug || '').trim())
const visitorIdShort = computed(() => visitorId.value ? visitorId.value.slice(0, 8) : '未生成')
const hasAccessToken = computed(() => Boolean(accessToken.value))
const iconUrl = computed(() => detail.iconPath ? toAddressable(detail.iconPath) : getImage('default.png'))
const showChatLayout = computed(() => !loadError.value && (!detail.passwordRequired || hasAccessToken.value))

function renderMessage(message: PublicMessage) {
  if (message.role === 'user') {
    return `<p>${md.utils.escapeHtml(String(message.content || '')).replace(/\n/g, '<br>')}</p>`
  }
  return md.render(String(message.content || ''))
}

function buildAccessTokenStorageKey(slug: string) {
  return `${ACCESS_TOKEN_STORAGE_PREFIX}${slug}`
}

function buildVisitorIdStorageKey(slug: string) {
  return `${VISITOR_ID_STORAGE_PREFIX}${slug}`
}

function ensureVisitorId() {
  const slug = routeSlug.value
  if (!slug) {
    visitorId.value = ''
    return
  }
  const storageKey = buildVisitorIdStorageKey(slug)
  const stored = window.localStorage.getItem(storageKey)
  if (stored) {
    visitorId.value = stored
    return
  }
  const nextVisitorId = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`
  window.localStorage.setItem(storageKey, nextVisitorId)
  visitorId.value = nextVisitorId
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

function resetPageState() {
  loadError.value = ''
  messages.value = []
  userInput.value = ''
  passwordForm.accessPassword = ''
  accessToken.value = ''
}

function loadDetail() {
  const slug = routeSlug.value
  if (!slug) {
    loadError.value = '公开访问标识缺失'
    return
  }
  queryPublicAppDetailApi(slug).then(result => {
    const data = result.data || {}
    detail.slug = String(data.slug || slug)
    detail.appName = String(data.appName || '')
    detail.appDesc = String(data.appDesc || '')
    detail.iconPath = String(data.iconPath || '')
    detail.accessType = String(data.accessType || 'PUBLIC')
    detail.passwordRequired = Boolean(data.passwordRequired)
    loadError.value = ''
    ensureVisitorId()
    restoreAccessToken()
    if (!detail.passwordRequired) {
      clearAccessToken()
    }
  }).catch(error => {
    loadError.value = error?.msg || '公开应用不存在或已关闭'
  })
}

function verifyPassword() {
  passwordFormRef.value?.validate((valid: boolean) => {
    if (!valid) {
      return
    }
    passwordLoading.value = true
    verifyPublicAppPasswordApi({
      slug: routeSlug.value,
      accessPassword: passwordForm.accessPassword
    }).then(result => {
      accessToken.value = String(result.data?.accessToken || '')
      if (routeSlug.value && accessToken.value) {
        window.sessionStorage.setItem(buildAccessTokenStorageKey(routeSlug.value), accessToken.value)
      }
      passwordForm.accessPassword = ''
      ElMessage.success('访问密码验证通过')
    }).finally(() => {
      passwordLoading.value = false
    })
  })
}

async function scrollBottom() {
  await nextTick()
  if (chatBoxRef.value) {
    chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
  }
}

async function sendMessage() {
  const content = userInput.value.trim()
  if (!content) {
    ElMessage.warning('请输入问题后再发送')
    return
  }
  if (detail.passwordRequired && !accessToken.value) {
    ElMessage.warning('请先完成访问密码验证')
    return
  }
  const assistantMessage: PublicMessage = { role: 'assistant', content: '' }
  messages.value.push({ role: 'user', content })
  messages.value.push(assistantMessage)
  userInput.value = ''
  sending.value = true
  scrollBottom()

  try {
    const response = await fetch(`${BASE_URL}/consumer/public/app/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        slug: routeSlug.value,
        visitorId: visitorId.value,
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
    await consumeSseStream(response.body, assistantMessage)
  } catch (error: any) {
    const fallbackMessage = error?.message || '公开聊天失败，请稍后重试'
    if (!assistantMessage.content) {
      assistantMessage.content = fallbackMessage
    } else if (!assistantMessage.content.includes(fallbackMessage)) {
      assistantMessage.content += `\n\n${fallbackMessage}`
    }
    if (detail.passwordRequired) {
      clearAccessToken()
    }
    ElMessage.error(fallbackMessage)
  } finally {
    sending.value = false
    scrollBottom()
  }
}

async function consumeSseStream(stream: ReadableStream<Uint8Array>, assistantMessage: PublicMessage) {
  const reader = stream.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
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
        return
      }
      assistantMessage.content += data
      scrollBottom()
    }
  }
  const tailData = parseSseFrame(buffer)
  if (tailData && tailData !== STREAM_END) {
    assistantMessage.content += tailData
  }
}

function parseSseFrame(frame: string) {
  return frame
    .split('\n')
    .filter(line => line.startsWith('data:'))
    .map(line => line.slice(5).replace(/^ /, ''))
    .join('\n')
}

function handleEditorKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

watch(() => route.params.slug, () => {
  resetPageState()
  loadDetail()
}, { immediate: true })

onMounted(() => {
  ensureVisitorId()
})
</script>

<style scoped>
.public-page {
  min-height: 100dvh;
  padding: 10px 10px 14px;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.18), transparent 28%),
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.16), transparent 24%),
    linear-gradient(180deg, #f5f9fc 0%, #eef4f7 100%);
}

.public-shell {
  max-width: 1340px;
  margin: 0 auto;
}

.public-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 248px;
  gap: 16px;
  margin-bottom: 16px;
}

.public-live-layout {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.hero-copy,
.hero-card,
.public-sidecard,
.state-panel,
.chat-shell {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.hero-copy {
  padding: 18px 20px;
}

.hero-kicker {
  margin-bottom: 8px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.08;
}

.hero-copy p {
  margin: 12px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.7;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.meta-chip {
  padding: 4px 8px;
  border-radius: 999px;
  background: #ecfeff;
  color: #155e75;
  font-size: 11px;
}

.hero-card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
  align-items: flex-start;
}

.hero-card-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.hero-card-desc,
.side-note,
.state-desc,
.composer-tip {
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
}

.public-sidecard {
  position: sticky;
  top: 18px;
  padding: 12px 12px;
}

.side-kicker {
  color: #0f766e;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.side-brand {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 8px;
}

.side-brand-copy {
  min-width: 0;
}

.side-brand-copy h1 {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.2;
}

.side-brand-copy p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 11px;
  line-height: 1.5;
}

.side-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.state-panel {
  padding: 18px;
  margin-bottom: 12px;
}

.error-panel {
  border-color: rgba(239, 68, 68, 0.24);
}

.chat-shell {
  padding: 12px 14px;
}

.chat-stream {
  min-height: 62dvh;
  max-height: calc(100dvh - 200px);
  overflow-y: auto;
  padding-right: 4px;
}

.message-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.message-row--user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex: 0 0 auto;
  min-width: 30px;
  height: 30px;
  padding: 0 6px;
  border-radius: 10px;
  background: linear-gradient(135deg, #0ea5e9, #14b8a6);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
}

.message-row--assistant .message-avatar {
  background: linear-gradient(135deg, #1e293b, #334155);
}

.message-bubble {
  flex: 1;
  min-width: 0;
  padding: 10px 13px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid rgba(226, 232, 240, 0.95);
  overflow: hidden;
}

.message-row--user .message-bubble {
  background: #eff6ff;
}

.message-role {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.state-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.message-content {
  margin-top: 4px;
  color: #334155;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
}

.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4) {
  margin: 0 0 10px;
  color: #0f172a;
  line-height: 1.35;
}

.message-content :deep(h1) {
  font-size: 18px;
}

.message-content :deep(h2) {
  font-size: 16px;
}

.message-content :deep(h3),
.message-content :deep(h4) {
  font-size: 15px;
}

.message-content :deep(p),
.message-content :deep(ul),
.message-content :deep(ol),
.message-content :deep(table),
.message-content :deep(blockquote) {
  margin: 0 0 10px;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  padding-left: 18px;
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
  padding: 6px 8px;
  border-bottom: 1px solid rgba(203, 213, 225, 0.8);
  text-align: left;
  vertical-align: top;
}

.message-content :deep(pre) {
  overflow-x: auto;
  padding: 10px 12px;
  border-radius: 12px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
}

.message-content :deep(code) {
  font-size: 12px;
}

.empty-chat {
  padding: 32px 8px;
  text-align: center;
}

.composer {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
}

.composer-head,
.composer-actions {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.composer-actions {
  margin-top: 8px;
}

.composer :deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.6;
}

.composer-buttons {
  display: flex;
  gap: 10px;
}

.reset-btn {
  color: #0f766e;
}

@media (max-width: 900px) {
  .public-page {
    padding: 8px 6px 12px;
  }

  .public-hero,
  .public-live-layout {
    grid-template-columns: 1fr;
  }

  .public-sidecard {
    position: static;
  }

  .chat-stream {
    min-height: 50dvh;
    max-height: none;
  }

  .composer-head,
  .composer-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .composer-buttons {
    justify-content: flex-end;
  }
}
</style>
