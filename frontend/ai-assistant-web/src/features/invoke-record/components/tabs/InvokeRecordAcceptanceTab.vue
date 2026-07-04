<template>
  <div class="tab-panel">
    <section class="workspace-section-card acceptance-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="review-title">当前验收重点</div>
          <div class="review-desc">先确认当前样本是否适合建批次，再看已保存批次和对比状态，最后决定是继续跑题集还是回到筛选区补样本。</div>
        </div>
      </div>
      <div class="workspace-tip-grid acceptance-tip-grid">
        <article
          v-for="item in acceptanceFocusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'acceptance-tip-card', item.tone ? `acceptance-tip-card--${item.tone}` : '']"
        >
          <div class="acceptance-tip-card__head">
            <span :class="['acceptance-tip-card__dot', item.tone ? `acceptance-tip-card__dot--${item.tone}` : '']"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
        </article>
      </div>
    </section>

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
import { computed } from 'vue'
import { useInvokeRecordFeatureModel } from '../../composables/useInvokeRecordFeature'

const model = useInvokeRecordFeatureModel()

const acceptanceFocusCards = computed(() => {
  return [
    {
      title: model.filteredRows.length ? `当前有 ${model.filteredRows.length} 条样本可直接转验收` : '当前还没有可直接转验收的样本',
      desc: model.filteredRows.length
        ? '可以直接保存批次，或先导出验收草稿后再补充测试结论、版本信息和下一步动作。'
        : '建议先回到筛选区圈定一批更有代表性的调用记录，再回来生成验收批次。',
      tone: model.filteredRows.length ? 'success' : 'warning'
    },
    {
      title: model.savedAcceptanceBatches.length ? `已沉淀 ${model.savedAcceptanceBatches.length} 个验收批次` : '当前还没有历史验收批次',
      desc: model.savedAcceptanceBatches.length
        ? '适合继续回看、编辑、复跑或导出 Markdown，对比不同版本下的问答表现。'
        : '建议先建立第一轮验收批次，后续才能继续做版本对比和任务池整理。',
      tone: model.savedAcceptanceBatches.length ? 'success' : 'warning'
    },
    {
      title: model.selectedAcceptanceBatchIds.length === 2 ? '当前已满足对比条件' : '选择两轮批次后可直接对比',
      desc: model.selectedAcceptanceBatchIds.length === 2
        ? '现在可以直接打开对比弹窗，或导出对比报告，观察同一知识库在不同版本下的变化。'
        : '建议优先挑同一知识库、相近场景的两轮批次，对比结果会更有参考价值。',
      tone: model.selectedAcceptanceBatchIds.length === 2 ? 'success' : 'warning'
    }
  ] as const
})
</script>

<style scoped>
.tab-panel,
.acceptance-focus-panel,
.review-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.acceptance-tip-grid {
  margin-top: 14px;
}

.acceptance-tip-card {
  position: relative;
  overflow: hidden;
}

.acceptance-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.acceptance-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.acceptance-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.acceptance-tip-card--danger::before {
  background: linear-gradient(180deg, #ef4444 0%, #dc2626 100%);
}

.acceptance-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.acceptance-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.78);
  flex-shrink: 0;
}

.acceptance-tip-card__dot--success {
  background: #12b76a;
}

.acceptance-tip-card__dot--warning {
  background: #f59e0b;
}

.acceptance-tip-card__dot--danger {
  background: #ef4444;
}

.acceptance-focus-panel,
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
  .acceptance-tip-grid,
  .workspace-section-head,
  .review-header {
    flex-direction: column;
  }

  .acceptance-tip-grid {
    grid-template-columns: 1fr;
  }

  .review-badges {
    justify-content: flex-start;
  }
}
</style>
