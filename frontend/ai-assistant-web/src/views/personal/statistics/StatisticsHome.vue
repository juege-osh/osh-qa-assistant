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

    <section class="workspace-section-card summary-panel statistics-summary-panel">
      <div class="panel-title">当前判断</div>
      <div class="summary-list">
        <div class="summary-item">{{ healthSummary }}</div>
        <div class="summary-item" v-for="item in insightList" :key="item">{{ item }}</div>
      </div>
    </section>

    <section class="workspace-section-card statistics-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="panel-title panel-title--md">优化方向</div>
          <div class="workspace-panel-desc">把当前数据翻译成下一步动作，方便继续决定先调知识库、提示词还是调用链路。</div>
        </div>
      </div>
      <div class="workspace-tip-grid statistics-tip-grid">
        <article
          v-for="item in focusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'statistics-tip-card', `statistics-tip-card--${item.tone}`]"
        >
          <div class="statistics-tip-card__head">
            <span :class="['statistics-tip-card__dot', `statistics-tip-card__dot--${item.tone}`]"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
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

const healthSummary = computed(() => {
  if (successRateValue.value >= 95) {
    return '当前整体稳定性较好，可以继续关注调用量和耗时变化，优先做体验优化与成本优化。'
  }
  if (successRateValue.value >= 80) {
    return '当前成功率处于可用但需要关注的区间，建议结合调用记录页重点复盘失败原因和慢请求。'
  }
  return '当前成功率偏低，建议优先排查异常链路、知识库状态、模型配置和请求参数。'
})

const insightList = computed(() => {
  const items: string[] = []
  const avgCostTime = normalizeNumber(stats.value.avgCostTime)
  const failCalls = normalizeNumber(stats.value.failCalls)
  const todayCalls = normalizeNumber(stats.value.todayCalls)
  const totalCalls = normalizeNumber(stats.value.totalCalls)

  if (failCalls > 0) {
    items.push(`当前累计失败 ${formatCount(failCalls)} 次，建议到调用记录页按失败和慢请求优先复盘。`)
  }
  if (avgCostTime >= 5000) {
    items.push('平均耗时已经偏高，建议优先检查检索、重排和模型响应链路。')
  } else {
    items.push('平均耗时目前相对可控，可以把更多精力放在答案质量和命中效果上。')
  }
  if (todayCalls > 0 && totalCalls > 0) {
    items.push(`今日调用 ${formatCount(todayCalls)} 次，可结合最近会话和调用记录观察当天波动。`)
  }
  items.push(`累计 Token 为 ${formatCount(stats.value.totalCostToken)}，如果增长过快，可以回头优化问题长度、提示词和召回策略。`)
  return items
})

const focusCards = computed(() => {
  const cards: Array<{ title: string, desc: string, tone: 'success' | 'warning' | 'danger' }> = []

  cards.push({
    title: successRateValue.value >= 95 ? '当前回答链路较稳' : successRateValue.value >= 80 ? '需要继续关注稳定性' : '优先排查失败原因',
    desc: successRateValue.value >= 95
      ? '可以把注意力更多放在知识覆盖和答案质量上，而不是先排查稳定性问题。'
      : successRateValue.value >= 80
        ? '建议结合调用记录重点看失败原因和慢请求，再判断问题来自模型还是检索。'
        : '先优先确认模型配置、参数、知识库状态与请求内容是否存在明显异常。',
    tone: successRateValue.value >= 95 ? 'success' : successRateValue.value >= 80 ? 'warning' : 'danger'
  })

  cards.push({
    title: normalizeNumber(stats.value.avgCostTime) >= 5000 ? '优先优化响应速度' : '速度表现目前可控',
    desc: normalizeNumber(stats.value.avgCostTime) >= 5000
      ? '建议先看检索、重排和模型响应时间，再决定是否要简化上下文或裁剪召回范围。'
      : '当前速度没有明显瓶颈，可以继续把时间放在提示词和知识组织方式上。',
    tone: normalizeNumber(stats.value.avgCostTime) >= 5000 ? 'warning' : 'success'
  })

  cards.push({
    title: normalizeNumber(stats.value.todayCalls) > 0 ? '今天已有真实使用数据' : '等待更多当天调用样本',
    desc: normalizeNumber(stats.value.todayCalls) > 0
      ? `今日调用 ${formatCount(stats.value.todayCalls)} 次，适合结合最近会话一起判断今天的问题分布和波动。`
      : '当前当天样本较少，可以先持续观察，等更多真实调用后再判断是不是阶段性问题。',
    tone: normalizeNumber(stats.value.todayCalls) > 0 ? 'warning' : 'success'
  })

  return cards
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

.statistics-summary-panel,
.statistics-focus-panel {
  padding: 18px 20px;
}

.statistics-tip-grid {
  margin-top: 14px;
}

.statistics-tip-card {
  position: relative;
  overflow: hidden;
}

.statistics-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.statistics-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.statistics-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.statistics-tip-card--danger::before {
  background: linear-gradient(180deg, #ef4444 0%, #dc2626 100%);
}

.statistics-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.statistics-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex-shrink: 0;
  background: rgba(148, 163, 184, 0.9);
}

.statistics-tip-card__dot--success {
  background: #12b76a;
  box-shadow: 0 0 0 4px rgba(18, 183, 106, 0.12);
}

.statistics-tip-card__dot--warning {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.14);
}

.statistics-tip-card__dot--danger {
  background: #ef4444;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.12);
}
</style>
