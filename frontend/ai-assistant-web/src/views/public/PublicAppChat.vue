<template>
  <div class="public-page">
    <section class="public-shell">
      <div class="public-hero">
        <div class="hero-copy">
          <div class="hero-kicker">公开应用</div>
          <h1>{{ detail.appName || '公开问答入口' }}</h1>
          <p>{{ detail.appDesc || '这个入口允许访客直接体验当前应用，不需要登录工作台。' }}</p>
          <div class="hero-meta">
            <span class="meta-chip">访问方式：{{ detail.passwordRequired ? '密码访问' : '公开访问' }}</span>
            <span class="meta-chip">访客会话：{{ visitorIdShort }}</span>
          </div>
        </div>
        <div class="hero-card">
          <el-avatar :src="iconUrl" :size="72"></el-avatar>
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

      <section v-else-if="detail.passwordRequired && !hasAccessToken" class="state-panel password-panel">
        <div class="state-title">输入访问密码</div>
        <div class="state-desc">这个公开应用已开启密码访问。验证通过后，当前浏览器会持有一份临时访问凭证。</div>
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

      <section v-else class="chat-shell">
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
            <div class="state-desc">这个页面直接复用了公开应用访问链路，适合做最小验收和访客体验验证。</div>
          </div>
        </div>

        <div class="composer">
          <div class="composer-head">
            <div>
              <div class="state-title">发起提问</div>
              <div class="state-desc">回答会实时流式输出，不需要登录，也不暴露 `appKey`。</div>
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
            :rows="4"
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
  min-height: 100vh;
  padding: 28px 20px 40px;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.18), transparent 28%),
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.16), transparent 24%),
    linear-gradient(180deg, #f5f9fc 0%, #eef4f7 100%);
}

.public-shell {
  max-width: 1100px;
  margin: 0 auto;
}

.public-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 20px;
  margin-bottom: 20px;
}

.hero-copy,
.hero-card,
.state-panel,
.chat-shell {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.hero-copy {
  padding: 28px;
}

.hero-kicker {
  margin-bottom: 10px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0;
  color: #0f172a;
  font-size: 34px;
  line-height: 1.1;
}

.hero-copy p {
  margin: 14px 0 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.meta-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: #ecfeff;
  color: #155e75;
  font-size: 12px;
}

.hero-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
  align-items: flex-start;
}

.hero-card-title {
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
}

.hero-card-desc,
.state-desc,
.composer-tip {
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.state-panel {
  padding: 24px;
  margin-bottom: 18px;
}

.error-panel {
  border-color: rgba(239, 68, 68, 0.24);
}

.chat-shell {
  padding: 20px;
}

.chat-stream {
  min-height: 420px;
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 4px;
}

.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.message-row--user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex: 0 0 auto;
  min-width: 48px;
  height: 48px;
  padding: 0 10px;
  border-radius: 16px;
  background: linear-gradient(135deg, #0ea5e9, #14b8a6);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.message-row--assistant .message-avatar {
  background: linear-gradient(135deg, #1e293b, #334155);
}

.message-bubble {
  flex: 1;
  padding: 16px 18px;
  border-radius: 22px;
  background: #f8fafc;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.message-row--user .message-bubble {
  background: #eff6ff;
}

.message-role,
.state-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.message-content {
  margin-top: 8px;
  color: #334155;
  line-height: 1.8;
  word-break: break-word;
}

.message-content :deep(pre) {
  overflow-x: auto;
  padding: 12px;
  border-radius: 14px;
  background: #0f172a;
  color: #e2e8f0;
}

.empty-chat {
  padding: 36px 12px;
  text-align: center;
}

.composer {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
}

.composer-head,
.composer-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.composer-actions {
  margin-top: 12px;
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
    padding: 18px 12px 28px;
  }

  .public-hero {
    grid-template-columns: 1fr;
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
