<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">平台运营总览</div>
      <div class="hero-subtitle">
        管理端首页用于快速判断整个助手平台的运行状态，包括调用成功率、失败情况、Token 消耗与平均响应耗时，适合每日巡检和问题排查。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">总调用：{{ pageData.overview.totalCount }}</span>
        <span class="hero-badge">成功：{{ pageData.overview.successCount }}</span>
        <span class="hero-badge">失败：{{ pageData.overview.failCount }}</span>
      </div>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <div class="stat-label">总调用次数</div>
        <div class="stat-value">{{ pageData.overview.totalCount }}</div>
        <div class="stat-help">平台累计处理的请求总数。</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">成功次数</div>
        <div class="stat-value">{{ pageData.overview.successCount }}</div>
        <div class="stat-help">返回成功结果的调用次数。</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">失败次数</div>
        <div class="stat-value">{{ pageData.overview.failCount }}</div>
        <div class="stat-help">出现报错或未完成的调用次数。</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">累计 Token</div>
        <div class="stat-value">{{ pageData.overview.totalCostToken }}</div>
        <div class="stat-help">模型调用累计消耗的 Token。</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">平均耗时</div>
        <div class="stat-value">{{ pageData.overview.avgCostTime }}ms</div>
        <div class="stat-help">调用处理平均耗时，越低越稳。</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">成功率</div>
        <div class="stat-value">{{ successRate }}</div>
        <div class="stat-help">帮助快速判断整体健康度。</div>
      </article>
    </section>

    <section class="glass-panel summary-panel">
      <div class="summary-title">值班建议</div>
      <div class="summary-list">
        <div class="summary-item">
          如果失败数明显上升，优先检查模型 key、向量库连接和 SSE 链路。
        </div>
        <div class="summary-item">
          如果平均耗时抬高，优先观察知识库检索、重排和模型响应时间。
        </div>
        <div class="summary-item">
          如果 Token 增长异常，建议巡检 prompt、上下文长度和重复请求情况。
        </div>
      </div>
    </section>

    <router-view></router-view>
  </div>
</template>
<script setup name='ManagerHome' lang='ts'>
import { computed, onMounted, reactive } from 'vue'
import { queryInvokeRecordOverviewApi } from '@/api/invokeRecordApi'

const pageData = reactive({
  overview: {
    totalCount: 0,
    successCount: 0,
    failCount: 0,
    totalCostToken: 0,
    avgCostTime: 0
  }
})

const successRate = computed(() => {
  if (!pageData.overview.totalCount) {
    return '0%'
  }
  return `${((pageData.overview.successCount / pageData.overview.totalCount) * 100).toFixed(1)}%`
})

function loadOverview() {
  queryInvokeRecordOverviewApi().then(result => {
    Object.assign(pageData.overview, result.data || {})
  })
}

onMounted(() => {
  loadOverview()
})
</script>
<style scoped>
.summary-panel {
  padding: 18px 20px;
}

.summary-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--space-text);
}

.summary-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.summary-item {
  padding: 12px 16px;
  border: 1px solid var(--space-border);
  border-radius: 8px;
  background: #fafafa;
  color: var(--space-muted);
  line-height: 1.7;
  font-size: 13px;
}
</style>
