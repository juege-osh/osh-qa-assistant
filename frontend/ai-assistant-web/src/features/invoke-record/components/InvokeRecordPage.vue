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

    <InvokeRecordFilterToolbar />

    <section class="workspace-section-card tabs-panel">
      <div class="tabs-copy">
        <div class="tabs-title">工作区拆分</div>
        <div class="tabs-desc workspace-panel-desc">把原来的重页面拆成三个任务区，当前筛选条件会在各个 tab 之间共享，便于先筛记录、再做复盘、最后整理验收。</div>
      </div>
      <el-tabs v-model="model.activeTab" class="invoke-record-tabs">
        <el-tab-pane v-for="tab in model.tabOptions" :key="tab.key" :label="tab.label" :name="tab.key">
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
const avgCostTimeDisplay = computed(() => `${formatCount(model.overview.avgCostTime)}ms`)

onMounted(() => {
  model.initialize()
})
</script>

<style scoped>
.invoke-record-page {
  gap: 16px;
}

.tabs-panel {
  padding: 18px 20px;
}

.tabs-title {
  color: var(--space-text);
  font-size: 16px;
  font-weight: 700;
}

.tabs-desc {
  margin-top: 6px;
}

.tabs-copy {
  margin-bottom: 14px;
}

:deep(.invoke-record-tabs .el-tabs__header) {
  margin-bottom: 18px;
}
</style>
