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

.account-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
