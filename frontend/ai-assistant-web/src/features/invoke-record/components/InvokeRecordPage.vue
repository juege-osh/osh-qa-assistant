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

    <section class="stats-grid">
      <article class="stat-card">
        <div class="stat-label">总调用次数</div>
        <div class="stat-value">{{ model.overview.totalCount }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">成功率</div>
        <div class="stat-value">{{ model.successRate }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">累计 Token</div>
        <div class="stat-value">{{ model.overview.totalCostToken }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">平均耗时</div>
        <div class="stat-value">{{ model.overview.avgCostTime }}ms</div>
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
