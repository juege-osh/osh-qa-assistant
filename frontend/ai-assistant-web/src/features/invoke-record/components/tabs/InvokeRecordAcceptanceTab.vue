<template>
  <div class="tab-panel">
    <section class="workspace-section-card workspace-section-panel">
      <div class="workspace-section-head">
        <div>
          <div class="workspace-section-title--md">验收操作</div>
          <div class="workspace-section-desc">先用上方筛选圈定样本，再生成验收批次、运行默认问题集或执行真实问题集。</div>
        </div>
        <div class="workspace-action-strip">
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportAcceptanceDraft">导出验收草稿</el-button>
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.openSaveAcceptanceBatchDialog">保存验收批次</el-button>
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.openTemplateAcceptanceBatchDialog">从默认问题集建批次</el-button>
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.openRealAcceptanceBatchDialog">执行真实问题集</el-button>
        </div>
      </div>
      <div class="workspace-chip-row workspace-chip-row--wide summary-row">
        <span class="workspace-chip workspace-chip--info">当前筛选记录：{{ model.filteredRows.length }}</span>
        <span class="workspace-chip workspace-chip--info">已保存批次：{{ model.savedAcceptanceBatches.length }}</span>
        <span class="workspace-chip workspace-chip--warning">已选对比：{{ model.selectedAcceptanceBatchIds.length }}</span>
      </div>
    </section>

    <section class="workspace-section-card review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">已保存验收批次</div>
          <div class="review-desc">保存过的验收批次都在这里，方便回看、对比和复跑。</div>
        </div>
        <div class="workspace-chip-row review-badges">
          <span class="workspace-chip workspace-chip--info">批次数：{{ model.savedAcceptanceBatches.length }}</span>
        </div>
      </div>
      <div class="category-export-row">
        <el-button class="workspace-btn workspace-btn--ghost" :disabled="model.selectedAcceptanceBatchIds.length !== 2" @click="model.openAcceptanceCompareDialog">对比两轮验收</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" :disabled="model.selectedAcceptanceBatchIds.length !== 2" @click="model.exportAcceptanceCompareDraft">导出对比报告</el-button>
        <span class="quick-filter-hint">最好选同一知识库的两轮批次来直接对比。</span>
      </div>
      <div v-if="model.savedAcceptanceBatches.length" class="workspace-persisted-grid">
        <article v-for="batch in model.savedAcceptanceBatches" :key="batch.id" class="workspace-persisted-card">
          <div class="workspace-persisted-card__head">
            <div>
              <div class="workspace-persisted-card__title">{{ batch.batchName }}</div>
              <div class="workspace-persisted-card__meta">
                {{ batch.appName || '未填写应用' }} · {{ batch.sceneType || '未填写场景' }} · {{ model.formatDateTime(batch.testDate || batch.createdTime) }}
              </div>
              <div v-if="batch.releaseVersion || batch.experimentVersion" class="workspace-persisted-card__meta">
                发布版本：{{ batch.releaseVersion || '-' }} · 实验版本：{{ batch.experimentVersion || '-' }}
              </div>
            </div>
            <div class="workspace-persisted-card__side">
              <label class="workspace-persisted-card__check">
                <input
                  type="checkbox"
                  :checked="model.selectedAcceptanceBatchIds.includes(batch.id)"
                  @change="model.toggleAcceptanceBatchSelection(batch.id)"
                >
                对比
              </label>
              <div class="workspace-persisted-card__count">{{ batch.itemCount || 0 }} 条</div>
            </div>
          </div>
          <div class="workspace-persisted-card__summary">
            <span>通过：{{ batch.passCount || 0 }}</span>
            <span>待跟进：{{ batch.followUpCount || 0 }}</span>
          </div>
          <div class="workspace-persisted-card__text">{{ batch.summaryConclusion || '当前还没有填写汇总结论。' }}</div>
          <div class="workspace-persisted-card__actions">
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.openSavedAcceptanceBatch(batch.id)">查看并编辑</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.rerunAcceptanceBatch(batch.id)">按当前生效版本复跑</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.exportSavedAcceptanceBatch(batch)">导出 Markdown</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.openAcceptanceRepairTaskDialog(batch.id)">查看任务池</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.exportAcceptanceRepairDraft(batch.id)">导出修复建议</el-button>
          </div>
        </article>
      </div>
      <div v-else class="empty-review-hint">
        还没有验收批次，先从当前筛选结果生成一轮，或者先运行默认问题集建立第一版验收样本。
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
.review-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.review-panel {
  padding: 18px;
}
.review-title {
  font-size: 16px;
  color: var(--space-text);
  font-weight: 700;
}

.review-desc,
.quick-filter-hint,
.empty-review-hint {
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.category-export-row,
.summary-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.review-badges {
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .workspace-section-head,
  .review-header {
    flex-direction: column;
  }

  .review-badges {
    justify-content: flex-start;
  }
}
</style>
