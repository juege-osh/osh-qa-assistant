<template>
  <div class="page-shell user-center-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">账户工作区</span>
        <span class="workspace-context-note">统一查看当前账号身份和接入凭证，对外联调时优先从这里确认 App Key 与环境是否一致。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">用户名 {{ pageData.user.username || '--' }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">注册 {{ pageData.user.registerTime || '--' }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">{{ credentialToneLabel }}</span>
      </div>
    </section>

    <section class="workspace-section-card account-overview-panel">
      <div class="account-overview-head workspace-overview-head">
        <div>
          <div class="panel-title panel-title--md">账户概览</div>
          <div class="panel-desc workspace-panel-desc">这里展示当前账号的基础身份信息、接入凭证和联调时最需要先确认的关键项。</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--success">凭证已生成</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">{{ credentialToneLabel }}</span>
        </div>
        <div class="account-actions">
          <el-button class="workspace-btn workspace-btn--ghost" :disabled="!pageData.user.appKey" @click="copyAppKey">
            复制 App Key
          </el-button>
        </div>
      </div>

      <div class="workspace-info-grid workspace-info-grid--compact">
        <article class="workspace-info-item">
          <span class="workspace-info-label">用户名</span>
          <strong class="workspace-info-value">{{ pageData.user.username || '--' }}</strong>
        </article>
        <article class="workspace-info-item">
          <span class="workspace-info-label">注册时间</span>
          <strong class="workspace-info-value">{{ pageData.user.registerTime || '--' }}</strong>
        </article>
        <article class="workspace-info-item workspace-info-item--full">
          <span class="workspace-info-label">App Key</span>
          <strong class="workspace-info-value workspace-info-value--mono">{{ pageData.user.appKey || '--' }}</strong>
        </article>
      </div>

      <div class="workspace-context-strip account-context-strip">
        <div class="workspace-context-copy">
          <span class="workspace-chip workspace-chip--primary">对外接入统一使用 App Key</span>
          <span class="workspace-context-note">登录密码不用于外部系统对接，接口或脚本接入优先使用这里的凭证。</span>
        </div>
      </div>
    </section>

    <section class="workspace-auth-metric-strip">
      <article class="workspace-auth-metric">
        <div class="workspace-auth-metric__value">App Key</div>
        <div class="workspace-auth-metric__label">外部系统接入统一使用 App Key，不直接暴露登录密码。</div>
      </article>
      <article class="workspace-auth-metric">
        <div class="workspace-auth-metric__value">先核验</div>
        <div class="workspace-auth-metric__label">对接失败时先确认当前凭证、应用和接口环境是否一致。</div>
      </article>
      <article class="workspace-auth-metric">
        <div class="workspace-auth-metric__value">再排查</div>
        <div class="workspace-auth-metric__label">确认凭证后，再去调用记录页看失败原因、耗时和请求内容。</div>
      </article>
    </section>

    <section class="workspace-section-card account-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="panel-title panel-title--md">账户关注点</div>
          <div class="workspace-panel-desc">从凭证使用、环境确认和问题排查三个角度，快速判断下一步该先看哪里。</div>
        </div>
      </div>
      <div class="workspace-tip-grid account-tip-grid">
        <article
          v-for="item in accountFocusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'account-tip-card', `account-tip-card--${item.tone}`]"
        >
          <div class="account-tip-card__head">
            <span :class="['account-tip-card__dot', `account-tip-card__dot--${item.tone}`]"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
        </article>
      </div>
    </section>

    <section class="workspace-auth-note-card">
      <div class="workspace-auth-note-card__title">使用提醒</div>
      <div class="workspace-auth-note-card__desc">接入和排查时先按这个顺序确认，能明显减少把环境、身份或配置问题误判成接口异常的情况。</div>
      <div class="workspace-auth-list">
        <div class="workspace-auth-list__item">
          <span class="workspace-auth-list__badge">1</span>
          <span>App Key 适合给外部系统或脚本接入使用，不要在公开位置暴露。</span>
        </div>
        <div class="workspace-auth-list__item">
          <span class="workspace-auth-list__badge">2</span>
          <span>调用异常时，先确认当前账号是否对应正确应用、正确知识库和正确环境。</span>
        </div>
        <div class="workspace-auth-list__item">
          <span class="workspace-auth-list__badge">3</span>
          <span>确认凭证无误后，再结合调用记录页查看失败原因、耗时和请求内容。</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup name="UserCenter" lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { queryUserByIdApi } from '@/api/workspace/userApi'
import { useUserStore } from '@/store/useUserStore'
import type { AnyObjDefine } from '@/types/common'
import { writeClipboardText } from '@/util/clipboard'

const pageData = reactive({
  user: {} as AnyObjDefine
})

const userStore = useUserStore()

const credentialToneLabel = computed(() => pageData.user.appKey ? '建议定期核验环境' : '当前暂无可用凭证')

const accountFocusCards = computed(() => [
  {
    title: pageData.user.appKey ? '当前可以直接对外接入' : '当前还没有可用凭证',
    desc: pageData.user.appKey
      ? '脚本、外部系统和联调环境都优先使用 App Key，不要直接暴露登录密码。'
      : '如果这里没有可用 App Key，建议先确认当前账号状态或返回应用页继续检查。',
    tone: pageData.user.appKey ? 'success' : 'danger'
  },
  {
    title: '对接失败先核对目标环境',
    desc: '调用异常时，优先确认当前账号、应用、知识库和接口地址是不是来自同一个环境。',
    tone: 'warning'
  },
  {
    title: '问题定位优先结合调用记录',
    desc: '如果已经确认凭证没问题，再去调用记录页看失败原因、耗时和具体请求内容会更高效。',
    tone: 'warning'
  }
] as const)

function queryUserInfo() {
  queryUserByIdApi(userStore.userInfo.id).then((result) => {
    pageData.user = result.data
  })
}

async function copyAppKey() {
  const appKey = String(pageData.user.appKey || '').trim()
  if (!appKey) {
    ElMessage.warning('当前没有可复制的 App Key')
    return
  }
  try {
    await writeClipboardText(appKey)
    ElMessage.success('已复制 App Key')
  } catch {
    ElMessage.error('复制失败，请稍后重试')
  }
}

onMounted(() => {
  queryUserInfo()
})
</script>

<style scoped>
.user-center-page {
  gap: 16px;
}

.account-overview-panel {
  padding: 20px;
}

.account-overview-head {
  margin-bottom: 16px;
}

.account-focus-panel {
  padding: 18px 20px;
}

.account-tip-grid {
  margin-top: 14px;
}

.account-tip-card {
  position: relative;
  overflow: hidden;
}

.account-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.account-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.account-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.account-tip-card--danger::before {
  background: linear-gradient(180deg, #ef4444 0%, #dc2626 100%);
}

.account-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.account-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.78);
  flex-shrink: 0;
}

.account-tip-card__dot--success {
  background: #12b76a;
}

.account-tip-card__dot--warning {
  background: #f59e0b;
}

.account-tip-card__dot--danger {
  background: #ef4444;
}

.account-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.account-context-strip {
  margin-top: 16px;
}

@media (max-width: 900px) {
  .workspace-auth-metric-strip {
    grid-template-columns: 1fr;
  }

  .account-tip-grid {
    grid-template-columns: 1fr;
  }
}
</style>
