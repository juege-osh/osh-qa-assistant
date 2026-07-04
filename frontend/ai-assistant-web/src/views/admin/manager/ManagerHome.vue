<template>
  <div class="page-shell manager-home-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">平台总览</span>
        <span class="workspace-context-note">统一查看平台调用规模、稳定性和耗时，优先判断今天要先巡检哪条链路。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">总调用 {{ totalCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">成功率 {{ successRate }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">平均耗时 {{ avgCostTimeDisplay }}</span>
      </div>
    </section>

    <section class="workspace-section-card manager-overview-panel workspace-dashboard-panel">
      <div class="manager-overview-head workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="panel-title panel-title--md">平台工作区</div>
          <div class="panel-desc workspace-panel-desc">这里适合做每日巡检，快速判断平台是否稳定、是否需要进入更细的记录页继续排查。</div>
        </div>
        <div class="workspace-inline-tags">
          <span :class="['workspace-inline-tag', healthToneClass]">{{ healthLabel }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">失败 {{ failCountDisplay }}</span>
        </div>
      </div>

      <section class="stats-grid manager-metrics-grid workspace-metrics-grid">
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--total">
          <div class="stat-label">总调用次数</div>
          <div class="stat-value">{{ totalCountDisplay }}</div>
          <div class="stat-help">平台累计处理的请求总数，反映整体使用规模。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
          <div class="stat-label">成功次数</div>
          <div class="stat-value workspace-stat-value--success">{{ successCountDisplay }}</div>
          <div class="stat-help">成功返回结果的调用次数，越高说明平台当前越稳定。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--danger">
          <div class="stat-label">失败次数</div>
          <div class="stat-value workspace-stat-value--danger">{{ failCountDisplay }}</div>
          <div class="stat-help">失败上升时，优先排查模型密钥、向量库连接和调用链路。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--token">
          <div class="stat-label">累计 Token</div>
          <div class="stat-value workspace-stat-value--warning">{{ totalCostTokenDisplay }}</div>
          <div class="stat-help">用于观察整体资源消耗，也能辅助识别异常流量。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--time">
          <div class="stat-label">平均耗时</div>
          <div class="stat-value">{{ avgCostTimeDisplay }}</div>
          <div class="stat-help">平均耗时抬高时，建议重点看检索、重排和模型响应时间。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
          <div class="stat-label">成功率</div>
          <div class="stat-value">{{ successRate }}</div>
          <div class="stat-help">适合快速判断平台当前是否处于稳定区间。</div>
        </article>
      </section>
    </section>

    <section class="workspace-section-card summary-panel manager-home-summary-panel">
      <div class="panel-title">值班建议</div>
      <div class="summary-list">
        <div class="summary-item">{{ healthSummary }}</div>
        <div class="summary-item" v-for="item in watchList" :key="item">{{ item }}</div>
      </div>
    </section>

    <section class="workspace-section-card manager-home-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="panel-title panel-title--md">今日关注</div>
          <div class="workspace-panel-desc">把首页数据转成更直接的值班动作，方便先定优先级，再进入具体页面排查。</div>
        </div>
      </div>
      <div class="workspace-tip-grid manager-home-tip-grid">
        <article
          v-for="item in focusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'manager-home-tip-card', `manager-home-tip-card--${item.tone}`]"
        >
          <div class="manager-home-tip-card__head">
            <span :class="['manager-home-tip-card__dot', `manager-home-tip-card__dot--${item.tone}`]"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
        </article>
      </div>
    </section>

    <div class="manager-detail-view workspace-detail-host">
      <router-view></router-view>
    </div>
  </div>
</template>

<script setup name="ManagerHome" lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { queryInvokeRecordOverviewApi } from '@/api/admin/invokeRecordApi'

const pageData = reactive({
  overview: {
    totalCount: 0,
    successCount: 0,
    failCount: 0,
    totalCostToken: 0,
    avgCostTime: 0
  }
})

function normalizeNumber(value: unknown) {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num : 0
}

function formatCount(value: unknown) {
  return normalizeNumber(value).toLocaleString('zh-CN')
}

const totalCountDisplay = computed(() => formatCount(pageData.overview.totalCount))
const successCountDisplay = computed(() => formatCount(pageData.overview.successCount))
const failCountDisplay = computed(() => formatCount(pageData.overview.failCount))
const totalCostTokenDisplay = computed(() => formatCount(pageData.overview.totalCostToken))
const avgCostTimeValue = computed(() => normalizeNumber(pageData.overview.avgCostTime))
const avgCostTimeDisplay = computed(() => `${formatCount(pageData.overview.avgCostTime)}ms`)

const successRateValue = computed(() => {
  const total = normalizeNumber(pageData.overview.totalCount)
  if (!total) {
    return 0
  }
  return Number(((normalizeNumber(pageData.overview.successCount) / total) * 100).toFixed(1))
})

const successRate = computed(() => `${successRateValue.value}%`)

const healthLabel = computed(() => {
  if (successRateValue.value >= 95) {
    return '平台稳定'
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
    return '当前平台整体较稳定，可以继续关注耗时变化和 Token 增长，优先做体验优化与成本优化。'
  }
  if (successRateValue.value >= 80) {
    return '当前平台可用但需要关注，建议进入调用记录页优先复盘失败原因和慢请求。'
  }
  return '当前平台成功率偏低，建议优先检查模型配置、向量库链路、SSE 输出和知识库状态。'
})

const watchList = computed(() => {
  const items: string[] = []
  const failCount = normalizeNumber(pageData.overview.failCount)

  if (failCount > 0) {
    items.push(`当前累计失败 ${formatCount(failCount)} 次，建议按失败记录优先排查真实报错和链路中断点。`)
  }

  if (avgCostTimeValue.value >= 5000) {
    items.push('平均耗时已经偏高，建议优先观察检索、重排和模型响应时间。')
  } else {
    items.push('平均耗时目前相对平稳，可以把更多注意力放在命中效果和业务回答质量上。')
  }

  items.push(`累计 Token 已到 ${formatCount(pageData.overview.totalCostToken)}，如果增长异常，建议巡检 prompt、上下文长度和重复请求。`)
  return items
})

const focusCards = computed(() => {
  const cards: Array<{ title: string, desc: string, tone: 'success' | 'warning' | 'danger' }> = []

  cards.push({
    title: successRateValue.value >= 95 ? '平台当前稳定' : successRateValue.value >= 80 ? '成功率需要关注' : '优先排查异常链路',
    desc: successRateValue.value >= 95
      ? '可以把更多精力放到体验优化、成本控制和回答质量复盘上。'
      : successRateValue.value >= 80
        ? '建议结合失败记录和慢请求继续缩小排查范围，避免问题继续积累。'
        : '先检查模型配置、向量库连接、SSE 输出与知识库可用状态。',
    tone: successRateValue.value >= 95 ? 'success' : successRateValue.value >= 80 ? 'warning' : 'danger'
  })

  cards.push({
    title: avgCostTimeValue.value >= 5000 ? '响应耗时偏高' : '响应时间相对平稳',
    desc: avgCostTimeValue.value >= 5000
      ? '优先关注检索、重排和模型首包响应时间，必要时对链路做分段排查。'
      : '当前链路速度没有明显压力，可以继续关注召回质量和业务回答稳定性。',
    tone: avgCostTimeValue.value >= 5000 ? 'warning' : 'success'
  })

  cards.push({
    title: normalizeNumber(pageData.overview.totalCostToken) > 0 ? '留意 Token 增长趋势' : '等待更多真实调用数据',
    desc: normalizeNumber(pageData.overview.totalCostToken) > 0
      ? `累计 Token 已到 ${formatCount(pageData.overview.totalCostToken)}，如果近期增长异常，建议检查上下文长度和重复请求。`
      : '当前还没有形成足够的成本趋势，可以先让数据继续沉淀，再回头判断资源效率。',
    tone: normalizeNumber(pageData.overview.totalCostToken) > 0 ? 'warning' : 'success'
  })

  return cards
})

function loadOverview() {
  queryInvokeRecordOverviewApi().then((result) => {
    Object.assign(pageData.overview, result.data || {})
  })
}

onMounted(() => {
  loadOverview()
})
</script>

<style scoped>
.manager-home-page {
  gap: 16px;
}

.manager-home-summary-panel,
.manager-home-focus-panel {
  padding: 18px 20px;
}

.manager-home-tip-grid {
  margin-top: 14px;
}

.manager-home-tip-card {
  position: relative;
  overflow: hidden;
}

.manager-home-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.manager-home-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.manager-home-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.manager-home-tip-card--danger::before {
  background: linear-gradient(180deg, #ef4444 0%, #dc2626 100%);
}

.manager-home-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.manager-home-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex-shrink: 0;
  background: rgba(148, 163, 184, 0.9);
}

.manager-home-tip-card__dot--success {
  background: #12b76a;
  box-shadow: 0 0 0 4px rgba(18, 183, 106, 0.12);
}

.manager-home-tip-card__dot--warning {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.14);
}

.manager-home-tip-card__dot--danger {
  background: #ef4444;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.12);
}
</style>
