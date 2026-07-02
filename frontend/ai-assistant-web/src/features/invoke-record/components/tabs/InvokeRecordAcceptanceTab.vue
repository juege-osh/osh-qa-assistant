<template>
  <div class="tab-panel">
    <section class="glass-panel section-panel">
      <div class="section-head">
        <div>
          <div class="section-title">当前筛选结果可直接转验收</div>
          <div class="section-desc">先用上方筛选圈定样本，再生成验收批次、默认问题集跑测或真实问题集跑测。</div>
        </div>
        <div class="section-actions">
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportAcceptanceDraft">导出验收草稿</el-button>
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.openSaveAcceptanceBatchDialog">保存验收批次</el-button>
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.openTemplateAcceptanceBatchDialog">从默认问题集建批次</el-button>
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.openRealAcceptanceBatchDialog">执行真实问题集</el-button>
        </div>
      </div>
      <div class="summary-row">
        <span class="summary-badge">当前筛选记录：{{ model.filteredRows.length }}</span>
        <span class="summary-badge">已保存批次：{{ model.savedAcceptanceBatches.length }}</span>
      </div>
    </section>

    <section class="glass-panel review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">已保存验收批次</div>
          <div class="review-desc">保存过的验收批次都在这里，方便回看、对比和复跑。</div>
        </div>
        <div class="review-badges">
          <span class="review-badge">批次数：{{ model.savedAcceptanceBatches.length }}</span>
        </div>
      </div>
      <div class="category-export-row">
        <el-button class="workspace-btn workspace-btn--ghost" :disabled="model.selectedAcceptanceBatchIds.length !== 2" @click="model.openAcceptanceCompareDialog">对比两轮验收</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" :disabled="model.selectedAcceptanceBatchIds.length !== 2" @click="model.exportAcceptanceCompareDraft">导出对比报告</el-button>
        <span class="quick-filter-hint">最好选同一知识库的两轮批次来直接对比。</span>
      </div>
      <div v-if="model.savedAcceptanceBatches.length" class="saved-batch-grid">
        <article v-for="batch in model.savedAcceptanceBatches" :key="batch.id" class="saved-batch-card">
          <div class="saved-batch-head">
            <div>
              <div class="saved-batch-title">{{ batch.batchName }}</div>
              <div class="saved-batch-meta">
                {{ batch.appName || '未填写应用' }} · {{ batch.sceneType || '未填写场景' }} · {{ model.formatDateTime(batch.testDate || batch.createdTime) }}
              </div>
              <div v-if="batch.releaseVersion || batch.experimentVersion" class="saved-batch-meta">
                发布版本：{{ batch.releaseVersion || '-' }} · 实验版本：{{ batch.experimentVersion || '-' }}
              </div>
            </div>
            <div class="saved-batch-head-side">
              <label class="saved-batch-check">
                <input
                  type="checkbox"
                  :checked="model.selectedAcceptanceBatchIds.includes(batch.id)"
                  @change="model.toggleAcceptanceBatchSelection(batch.id)"
                >
                对比
              </label>
              <div class="saved-batch-count">{{ batch.itemCount || 0 }} 条</div>
            </div>
          </div>
          <div class="saved-batch-summary">
            <span>通过：{{ batch.passCount || 0 }}</span>
            <span>待跟进：{{ batch.followUpCount || 0 }}</span>
          </div>
          <div class="saved-batch-text">{{ batch.summaryConclusion || '当前还没有填写汇总结论。' }}</div>
          <div class="priority-actions">
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.openSavedAcceptanceBatch(batch.id)">查看并编辑</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.rerunAcceptanceBatch(batch.id)">按当前生效版本复跑</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.exportSavedAcceptanceBatch(batch)">导出 Markdown</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.openAcceptanceRepairTaskDialog(batch.id)">查看任务池</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.exportAcceptanceRepairDraft(batch.id)">导出修复建议</el-button>
          </div>
        </article>
      </div>
      <div v-else class="empty-review-hint">
        还没有验收批次，先从当前筛选结果生成一轮。
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useInvokeRecordFeatureModel } from '../../composables/useInvokeRecordFeature'

const model = useInvokeRecordFeatureModel()
</script>

<style scoped>
.tab-panel,
.section-panel,
.review-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-panel,
.review-panel {
  padding: 18px;
}

.section-head,
.review-header,
.saved-batch-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.section-title,
.review-title,
.saved-batch-title {
  color: var(--space-text);
  font-weight: 700;
}

.section-title,
.review-title {
  font-size: 16px;
}

.saved-batch-title {
  font-size: 15px;
}

.section-desc,
.review-desc,
.quick-filter-hint,
.saved-batch-meta,
.saved-batch-check,
.empty-review-hint {
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.review-badges,
.section-actions,
.category-export-row,
.summary-row,
.saved-batch-summary,
.priority-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.review-badges {
  justify-content: flex-end;
}

.review-badge,
.summary-badge {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(237, 245, 255, 0.9);
  color: var(--space-primary-strong);
  font-size: 12px;
  font-weight: 600;
}

.saved-batch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
}

.saved-batch-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  background: rgba(255, 255, 255, 0.96);
}

.saved-batch-head-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.saved-batch-check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.saved-batch-count {
  color: var(--space-primary-strong);
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}

.saved-batch-summary {
  margin-top: 12px;
  color: var(--space-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

.saved-batch-text {
  margin-top: 10px;
  color: var(--space-text);
  font-size: 13px;
  line-height: 1.7;
}

.record-text-btn {
  min-height: 24px !important;
  padding: 0 !important;
  font-size: 12px;
}

@media (max-width: 900px) {
  .section-head,
  .review-header,
  .saved-batch-head {
    flex-direction: column;
  }

  .review-badges {
    justify-content: flex-start;
  }

  .saved-batch-head-side {
    align-items: flex-start;
  }
}
</style>
