<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">账户</div>
      <div class="hero-subtitle">
        查看账号信息和 App Key。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">用户名：{{ pageData.user.username || '--' }}</span>
        <span class="hero-badge">注册时间：{{ pageData.user.registerTime || '--' }}</span>
        <span class="hero-badge">应用凭证已生成</span>
      </div>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <div class="stat-label">用户名</div>
        <div class="stat-value value-small">{{ pageData.user.username || '--' }}</div>
        <div class="stat-help">登录时使用。</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">注册时间</div>
        <div class="stat-value value-small">{{ pageData.user.registerTime || '--' }}</div>
        <div class="stat-help">方便确认账号开通时间。</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">App Key</div>
        <div class="stat-value value-small key-text">{{ pageData.user.appKey || '--' }}</div>
        <div class="stat-help">对接接口时使用，请妥善保管。</div>
      </article>
    </section>

    <section class="glass-panel account-panel">
      <div class="panel-title">详细信息</div>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ pageData.user.username || '--' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ pageData.user.registerTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="App Key">
          <span class="key-text">{{ pageData.user.appKey || '--' }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <div class="hint">
        外部系统接入时使用 `appKey`，不要直接使用登录密码。
      </div>
    </section>
  </div>
</template>
<script setup name='UserCenter' lang='ts'>
import { reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/useUserStore'
import { queryUserByIdApi } from '@/api/workspace/userApi'
import type { AnyObjDefine } from '@/types/common'

let pageData = reactive({
  user: {} as AnyObjDefine,
})

let userStore = useUserStore()

// ===== 账户 =====
// 查询用户信息
function queryUserInfo() {
  queryUserByIdApi(userStore.userInfo.id).then(result => {
    pageData.user = result.data
  })
}

onMounted(() => {
  queryUserInfo()
})
</script>
<style scoped>
.account-panel {
  padding: 18px 20px 22px;
}

.panel-title {
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 600;
  color: var(--space-text);
}

.hint {
  margin-top: 14px;
  color: var(--space-muted);
  line-height: 1.7;
  font-size: 13px;
}

.value-small {
  font-size: 15px;
  line-height: 1.5;
}

.key-text {
  word-break: break-all;
}
</style>
