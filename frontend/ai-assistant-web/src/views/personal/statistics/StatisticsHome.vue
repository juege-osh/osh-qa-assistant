<template>
  <div class="page-shell statistics-home-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">应用统计</span>
        <span class="workspace-context-note">统一查看调用规模、稳定性和耗时，优先判断当前应用需要优化哪一段链路。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">总调用 {{ totalCallsDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">成功率 {{ successRateDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">平均耗时 {{ avgCostTimeDisplay }}</span>
      </div>
    </section>

    <section class="workspace-section-card stats-overview-panel workspace-dashboard-panel">
      <div class="stats-overview-head workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="panel-title panel-title--md">统计工作区</div>
          <div class="panel-desc workspace-panel-desc">从调用量、成功率和响应速度快速判断当前应用处于什么状态。</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--soft">今日调用 {{ todayCallsDisplay }}</span>
          <span :class="['workspace-inline-tag', healthToneClass]">{{ healthLabel }}</span>
        </div>
      </div>

      <div class="stats-grid metrics-grid workspace-metrics-grid">
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--total">
          <div class="stat-label">总调用次数</div>
          <div class="stat-value">{{ totalCallsDisplay }}</div>
          <div class="stat-help">累计所有调用次数，反映整体使用规模。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
          <div class="stat-label">成功次数</div>
          <div class="stat-value workspace-stat-value--success">{{ successCallsDisplay }}</div>
          <div class="stat-help">成功返回结果的次数，越高说明链路越稳定。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--danger">
          <div class="stat-label">失败次数</div>
          <div class="stat-value workspace-stat-value--danger">{{ failCallsDisplay }}</div>
          <div class="stat-help">失败突然上升时，建议优先检查模型、配置和知识库状态。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
          <div class="stat-label">成功率</div>
          <div class="stat-value">{{ successRateDisplay }}</div>
          <div class="stat-help">成功率越高，用户实际体验通常越稳定。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--token">
          <div class="stat-label">今日调用</div>
          <div class="stat-value workspace-stat-value--warning">{{ todayCallsDisplay }}</div>
          <div class="stat-help">快速判断今天的活跃程度和近期波动。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--token">
          <div class="stat-label">累计 Token</div>
          <div class="stat-value">{{ totalCostTokenDisplay }}</div>
          <div class="stat-help">用于感知整体使用量，也能辅助判断成本趋势。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--time">
          <div class="stat-label">平均耗时</div>
          <div class="stat-value">{{ avgCostTimeDisplay }}</div>
          <div class="stat-help">耗时偏高时，优先排查检索、重排和模型响应时间。</div>
        </article>
      </div>
    </section>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getOverviewApi } from '@/api/workspace/statisticsApi'

const stats = ref<any>({})

function normalizeNumber(value: unknown) {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num : 0
}

function formatCount(value: unknown) {
  return normalizeNumber(value).toLocaleString('zh-CN')
}

const totalCallsDisplay = computed(() => formatCount(stats.value.totalCalls))
const successCallsDisplay = computed(() => formatCount(stats.value.successCalls))
const failCallsDisplay = computed(() => formatCount(stats.value.failCalls))
const todayCallsDisplay = computed(() => formatCount(stats.value.todayCalls))
const totalCostTokenDisplay = computed(() => formatCount(stats.value.totalCostToken))
const avgCostTimeDisplay = computed(() => `${formatCount(stats.value.avgCostTime)}ms`)
const successRateValue = computed(() => normalizeNumber(stats.value.successRate))
const successRateDisplay = computed(() => `${successRateValue.value}%`)

const healthLabel = computed(() => {
  if (successRateValue.value >= 95) {
    return '状态稳定'
  }
  if (successRateValue.value >= 80) {
    return '需要关注'
  }
  return '优先排查'
})

const healthToneClass = computed(() => {
  if (successRateValue.value >= 95) {
    return 'workspace-inline-tag--success'
  }
  if (successRateValue.value >= 80) {
    return 'workspace-inline-tag--warning'
  }
  return 'workspace-inline-tag--danger'
})

onMounted(() => {
  getOverviewApi().then((res: any) => {
    stats.value = res.data || {}
  })
})
</script>

<style scoped>
.statistics-home-page {
  gap: 16px;
}
</style>
