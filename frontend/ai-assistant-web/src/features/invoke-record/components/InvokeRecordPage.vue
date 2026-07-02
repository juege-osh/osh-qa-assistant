<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">调用记录分析</div>
      <div class="hero-subtitle">
        按应用、问题和状态筛选记录，并在记录、复盘、验收批次三个视角之间切换。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">总调用：{{ model.overview.totalCount }}</span>
        <span class="hero-badge">成功：{{ model.overview.successCount }}</span>
        <span class="hero-badge">失败：{{ model.overview.failCount }}</span>
      </div>
    </section>

    <section class="stats-grid stats-grid--colored">
      <article class="stat-card stat-card--total">
        <div class="stat-bar"></div>
        <div class="stat-body">
          <div class="stat-label">总调用次数</div>
          <div class="stat-value">{{ model.overview.totalCount }}</div>
        </div>
      </article>
      <article class="stat-card stat-card--success">
        <div class="stat-bar"></div>
        <div class="stat-body">
          <div class="stat-label">成功率</div>
          <div class="stat-value">{{ model.successRate }}</div>
        </div>
      </article>
      <article class="stat-card stat-card--token">
        <div class="stat-bar"></div>
        <div class="stat-body">
          <div class="stat-label">累计 Token</div>
          <div class="stat-value">{{ model.overview.totalCostToken }}</div>
        </div>
      </article>
      <article class="stat-card stat-card--time">
        <div class="stat-bar"></div>
        <div class="stat-body">
          <div class="stat-label">平均耗时</div>
          <div class="stat-value">{{ model.overview.avgCostTime }}<span class="stat-unit">ms</span></div>
        </div>
      </article>
    </section>

    <InvokeRecordFilterToolbar />

    <section class="glass-panel tabs-panel">
      <div class="tabs-copy">
        <div class="tabs-title">工作区拆分</div>
        <div class="tabs-desc">把原来的重页面拆成三个任务区，当前筛选条件会在各个 tab 之间共享。</div>
      </div>
      <el-tabs v-model="model.activeTab" class="invoke-record-tabs">
        <el-tab-pane v-for="tab in model.tabOptions" :key="tab.key" :label="tab.label" :name="tab.key">
          <div class="tab-description">{{ tab.description }}</div>
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
import { onMounted } from 'vue'
import InvokeRecordDialogs from './InvokeRecordDialogs.vue'
import InvokeRecordFilterToolbar from './InvokeRecordFilterToolbar.vue'
import InvokeRecordAcceptanceTab from './tabs/InvokeRecordAcceptanceTab.vue'
import InvokeRecordRecordsTab from './tabs/InvokeRecordRecordsTab.vue'
import InvokeRecordReviewTab from './tabs/InvokeRecordReviewTab.vue'
import { provideInvokeRecordFeatureModel } from '../composables/useInvokeRecordFeature'

const model = provideInvokeRecordFeatureModel()

onMounted(() => {
  model.initialize()
})
</script>

<style scoped>
.tabs-panel {
  padding: 18px;
}

.stats-grid--colored {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stats-grid--colored .stat-card {
  display: flex;
  align-items: stretch;
  padding: 0;
  overflow: hidden;
}

.stats-grid--colored .stat-bar {
  flex: 0 0 4px;
  border-radius: 4px 0 0 4px;
}

.stats-grid--colored .stat-card--total .stat-bar {
  background: linear-gradient(180deg, #409eff, #2f7fe2);
}

.stats-grid--colored .stat-card--success .stat-bar {
  background: linear-gradient(180deg, #67c23a, #4fa82d);
}

.stats-grid--colored .stat-card--token .stat-bar {
  background: linear-gradient(180deg, #e6a23c, #cf8a2e);
}

.stats-grid--colored .stat-card--time .stat-bar {
  background: linear-gradient(180deg, #909399, #73767a);
}

.stats-grid--colored .stat-body {
  flex: 1;
  padding: 16px 18px;
}

.stats-grid--colored .stat-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.stats-grid--colored .stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.stats-grid--colored .stat-card--total .stat-value {
  color: var(--space-primary-strong);
}

.stats-grid--colored .stat-card--success .stat-value {
  color: #3a9a1e;
}

.stats-grid--colored .stat-card--token .stat-value {
  color: #b5791e;
}

.stats-grid--colored .stat-card--time .stat-value {
  color: var(--space-text);
}

.stats-grid--colored .stat-unit {
  margin-left: 2px;
  font-size: 14px;
  font-weight: 600;
  color: var(--space-muted);
}

@media (max-width: 900px) {
  .stats-grid--colored {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.tabs-copy {
  margin-bottom: 14px;
}

.tabs-title {
  color: var(--space-text);
  font-size: 16px;
  font-weight: 700;
}

.tabs-desc,
.tab-description {
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.tabs-desc {
  margin-top: 6px;
}

.tab-description {
  margin-bottom: 12px;
}

:deep(.invoke-record-tabs .el-tabs__header) {
  margin-bottom: 18px;
}
</style>
