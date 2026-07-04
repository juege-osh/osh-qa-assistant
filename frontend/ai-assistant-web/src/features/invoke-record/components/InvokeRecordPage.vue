<template>
  <div class="page-shell invoke-record-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">调用巡检</span>
        <span class="workspace-context-note">按应用、问题和状态筛选样本，再在记录、复盘和验收批次之间连续推进同一批问题。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">总调用 {{ totalCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">成功率 {{ model.successRate }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">平均耗时 {{ avgCostTimeDisplay }}</span>
      </div>
    </section>

    <section class="workspace-section-card overview-panel workspace-dashboard-panel">
      <div class="overview-head workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="overview-title">调用工作区</div>
          <div class="overview-desc workspace-panel-desc">先看整体稳定性和筛选后的工作规模，再决定当前重点放在记录明细、复盘判断，还是正式验收整理。</div>
        </div>
        <div class="workspace-inline-tags">
          <span :class="['workspace-inline-tag', healthToneClass]">{{ healthLabel }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">当前记录 {{ filteredCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">当前工作区 {{ activeTabOption.label }}</span>
        </div>
      </div>

      <section class="stats-grid overview-metrics-grid workspace-metrics-grid">
        <article class="stat-card workspace-stat-card--total workspace-stat-card--framed">
          <div class="stat-label">总调用次数</div>
          <div class="stat-value">{{ totalCountDisplay }}</div>
          <div class="stat-help">当前统计周期内的全部调用规模。</div>
        </article>
        <article class="stat-card workspace-stat-card--success workspace-stat-card--framed">
          <div class="stat-label">成功率</div>
          <div class="stat-value">{{ model.successRate }}</div>
          <div class="stat-help">用来快速判断链路是否稳定。</div>
        </article>
        <article class="stat-card workspace-stat-card--token workspace-stat-card--framed">
          <div class="stat-label">累计 Token</div>
          <div class="stat-value">{{ totalCostTokenDisplay }}</div>
          <div class="stat-help">用于感知整体请求消耗与上下文规模。</div>
        </article>
        <article class="stat-card workspace-stat-card--time workspace-stat-card--framed">
          <div class="stat-label">平均耗时</div>
          <div class="stat-value">{{ avgCostTimeDisplay }}</div>
          <div class="stat-help">耗时偏高时优先排查检索、重排和模型响应。</div>
        </article>
      </section>
    </section>

    <section class="workspace-section-card workspace-panel">
      <div class="tabs-copy">
        <div class="tabs-title">当前工作区</div>
        <div class="tabs-desc workspace-panel-desc">{{ activeWorkspaceDesc }}</div>
      </div>
      <div class="workspace-inline-tags">
        <span class="workspace-inline-tag workspace-inline-tag--soft">筛选后记录 {{ filteredCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">失败 {{ failCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">成功 {{ successCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">{{ quickViewDisplay }}</span>
        <span v-if="model.activeTab === 'review'" class="workspace-inline-tag workspace-inline-tag--soft">待跟进 {{ followUpCountDisplay }}</span>
        <span v-if="model.activeTab === 'acceptance'" class="workspace-inline-tag workspace-inline-tag--soft">批次 {{ savedBatchCountDisplay }}</span>
      </div>
    </section>

    <InvokeRecordFilterToolbar />

    <section class="workspace-section-card tabs-panel">
      <div class="tabs-copy">
        <div class="tabs-title">工作区拆分</div>
        <div class="tabs-desc workspace-panel-desc">把原来的重页面拆成三个任务区，当前筛选条件会在各个 tab 之间共享，便于先筛记录、再做复盘、最后整理验收。</div>
      </div>
      <el-tabs v-model="model.activeTab" class="invoke-record-tabs">
        <el-tab-pane v-for="tab in model.tabOptions" :key="tab.key" :label="tab.label" :name="tab.key">
          <div class="tab-description workspace-panel-desc">{{ tab.description }}</div>
          <InvokeRecordRecordsTab v-if="tab.key === 'records'" />
          <InvokeRecordReviewTab v-else-if="tab.key === 'review'" />
          <InvokeRecordAcceptanceTab v-else />
        </el-tab-pane>
      </el-tabs>
    </section>

    <InvokeRecordDialogs />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import InvokeRecordDialogs from './InvokeRecordDialogs.vue'
import InvokeRecordFilterToolbar from './InvokeRecordFilterToolbar.vue'
import InvokeRecordAcceptanceTab from './tabs/InvokeRecordAcceptanceTab.vue'
import InvokeRecordRecordsTab from './tabs/InvokeRecordRecordsTab.vue'
import InvokeRecordReviewTab from './tabs/InvokeRecordReviewTab.vue'
import { provideInvokeRecordFeatureModel } from '../composables/useInvokeRecordFeature'

const model = provideInvokeRecordFeatureModel()

function normalizeNumber(value: unknown) {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num : 0
}

function formatCount(value: unknown) {
  return normalizeNumber(value).toLocaleString('zh-CN')
}

const totalCountDisplay = computed(() => formatCount(model.overview.totalCount))
const totalCostTokenDisplay = computed(() => formatCount(model.overview.totalCostToken))
const avgCostTimeDisplay = computed(() => `${formatCount(model.overview.avgCostTime)}ms`)
const filteredCountDisplay = computed(() => formatCount(model.filteredRows.length))
const successCountDisplay = computed(() => formatCount(model.overview.successCount))
const failCountDisplay = computed(() => formatCount(model.overview.failCount))
const slowCountDisplay = computed(() => formatCount(model.slowRowCount))
const followUpCountDisplay = computed(() => formatCount(model.followUpCount))
const priorityReviewCountDisplay = computed(() => formatCount(model.priorityReviewList.length))
const savedBatchCountDisplay = computed(() => formatCount(model.savedAcceptanceBatches.length))
const successRateValue = computed(() => normalizeNumber(String(model.successRate).replace('%', '')))
const activeTabOption = computed(() => {
  return model.tabOptions.find((tab) => tab.key === model.activeTab) || model.tabOptions[0]
})
const quickViewLabel = computed(() => {
  if (model.quickView === 'fail') {
    return '只看失败'
  }
  if (model.quickView === 'slow') {
    return '只看慢请求'
  }
  if (model.quickView === 'long') {
    return '只看长回答'
  }
  if (model.quickView === 'followUp') {
    return '只看待跟进'
  }
  if (model.quickView === 'reviewed') {
    return '只看已复盘'
  }
  return '查看全部'
})
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
const activeWorkspaceDesc = computed(() => {
  if (model.activeTab === 'review') {
    return `${activeTabOption.value.description} 当前聚焦：${model.currentQuickViewDesc}`
  }
  if (model.activeTab === 'acceptance') {
    return `${activeTabOption.value.description} 现在可以继续整理批次、回看已保存样本，或发起默认题集和真实问题验收。`
  }
  return `${activeTabOption.value.description} 当前优先把样本范围缩小，再进入对应记录查看明细。`
})
const quickViewDisplay = computed(() => `当前聚焦 ${quickViewLabel.value}`)

onMounted(() => {
  model.initialize()
})
</script>

<style scoped>
.invoke-record-page {
  gap: 16px;
}

.workspace-panel,
.tabs-panel {
  padding: 18px 20px;
}

.overview-title,
.tabs-title {
  color: var(--space-text);
  font-size: 16px;
  font-weight: 700;
}

.overview-desc,
.tabs-desc {
  margin-top: 6px;
}

.workspace-panel {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.tabs-copy {
  margin-bottom: 14px;
}

.tab-description {
  margin-bottom: 12px;
}

:deep(.invoke-record-tabs .el-tabs__header) {
  margin-bottom: 18px;
}

@media (max-width: 900px) {
  .workspace-panel {
    flex-direction: column;
    align-items: stretch;
  }

  .invoke-record-tip-grid {
    grid-template-columns: 1fr;
  }
}
</style>
