<template>
  <div class="tab-panel">
    <section class="workspace-section-card review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">复盘筛选</div>
          <div class="review-desc">先看失败、慢请求和长回答，通常最容易发现问题；筛好以后再进优先复盘清单会更高效。</div>
        </div>
        <div class="workspace-chip-row review-badges">
          <span class="workspace-chip workspace-chip--info">记录：{{ model.filteredRows.length }}</span>
          <span class="workspace-chip workspace-chip--danger">失败：{{ model.failRowCount }}</span>
          <span class="workspace-chip workspace-chip--warning">慢请求：{{ model.slowRowCount }}</span>
          <span class="workspace-chip workspace-chip--info">长回答：{{ model.longAnswerRowCount }}</span>
          <span class="workspace-chip workspace-chip--info">待跟进：{{ model.followUpCount }}</span>
        </div>
      </div>
      <div class="quick-filter-row">
        <el-button class="workspace-btn workspace-btn--sm" :class="model.quickView === 'all' ? 'workspace-btn--primary' : 'workspace-btn--ghost'" @click="model.quickView = 'all'">全部记录</el-button>
        <el-button class="workspace-btn workspace-btn--sm" :class="model.quickView === 'fail' ? 'workspace-btn--primary' : 'workspace-btn--ghost'" @click="model.quickView = 'fail'">只看失败</el-button>
        <el-button class="workspace-btn workspace-btn--sm" :class="model.quickView === 'slow' ? 'workspace-btn--primary' : 'workspace-btn--ghost'" @click="model.quickView = 'slow'">只看慢请求</el-button>
        <el-button class="workspace-btn workspace-btn--sm" :class="model.quickView === 'long' ? 'workspace-btn--primary' : 'workspace-btn--ghost'" @click="model.quickView = 'long'">只看长回答</el-button>
        <el-button class="workspace-btn workspace-btn--sm" :class="model.quickView === 'followUp' ? 'workspace-btn--primary' : 'workspace-btn--ghost'" @click="model.quickView = 'followUp'">只看待跟进</el-button>
        <el-button class="workspace-btn workspace-btn--sm" :class="model.quickView === 'reviewed' ? 'workspace-btn--primary' : 'workspace-btn--ghost'" @click="model.quickView = 'reviewed'">只看已复盘</el-button>
      </div>
      <div class="review-hint-row">
        <span class="quick-filter-hint">慢请求 ≥ 5000ms · 长回答 ≥ 200 字符 · 建议先看失败，再看慢请求，最后抽样长回答</span>
      </div>
    </section>

    <section class="workspace-section-card review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">优先复盘清单</div>
          <div class="review-desc">这些记录值得优先查看，结论仍以人工判断为准。</div>
        </div>
        <div class="workspace-chip-row review-badges">
          <span class="workspace-chip workspace-chip--info">待优先复盘：{{ model.priorityReviewList.length }}</span>
        </div>
      </div>
      <div v-if="model.priorityReviewList.length" class="priority-grid">
        <article v-for="item in model.priorityReviewList" :key="item.key" class="priority-card">
          <div class="priority-card-head">
            <div>
              <div class="priority-card-title">
                {{ item.row.appName || '未命名应用' }}
                <span class="priority-card-id">#{{ item.row.id }}</span>
              </div>
              <div class="priority-card-meta">
                {{ item.row.libName || '未绑定知识库' }} · {{ item.detail.modelName || '未知模型' }} · {{ item.detail.costTime ?? item.row.costTime ?? '-' }}ms
              </div>
            </div>
            <div class="workspace-chip-row priority-tag-group">
              <span v-for="tag in item.riskTags" :key="`${item.key}-${tag.key}`" class="workspace-chip" :class="`workspace-chip--${tag.tone}`">
                {{ tag.label }}
              </span>
              <span v-if="model.getReviewStatus(item.key) !== 'pending'" class="workspace-chip workspace-chip--success">
                {{ model.reviewStatusText[model.getReviewStatus(item.key)] }}
              </span>
            </div>
          </div>
          <div class="priority-block">
            <div class="priority-label">用户问题</div>
            <div class="priority-text">{{ item.detail.userInput || '-' }}</div>
          </div>
          <div class="priority-block">
            <div class="priority-label">回答摘要</div>
            <div class="priority-text">{{ model.summarizeDisplayText(item.detail.assistantMessage) }}</div>
          </div>
          <div v-if="item.detail.failReason || item.row.failReason" class="priority-block">
            <div class="priority-label">失败原因</div>
            <div class="priority-text priority-text--danger">{{ item.detail.failReason || item.row.failReason }}</div>
          </div>
          <div class="priority-actions">
            <el-button text class="workspace-btn workspace-btn--text workspace-text-btn--compact" @click="model.copyText(item.detail.userInput, '查询词')">复制问题</el-button>
            <el-button text class="workspace-btn workspace-btn--text workspace-text-btn--compact" @click="model.openDetailDialog('响应结果', item.detail.assistantMessage)">查看回答</el-button>
            <el-button text class="workspace-btn workspace-btn--text workspace-text-btn--compact" @click="model.copyAcceptanceEntry(item.row, item.detail)">复制验收条目</el-button>
            <el-button text class="workspace-btn workspace-btn--text workspace-text-btn--compact" @click="model.cycleReviewStatus(item.key)">
              {{ model.nextReviewActionText(item.key) }}
            </el-button>
          </div>
        </article>
      </div>
      <div v-else class="empty-review-hint">
        当前筛选结果里没有自动判定为重点复盘的记录，可以继续抽样检查表格明细。
      </div>
    </section>

    <section class="workspace-section-card review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">待跟进问题分布</div>
          <div class="review-desc">把待跟进记录归到补知识、补切分、补提示词、补展示、补观测等动作类型，帮助后续直接进入任务池。</div>
        </div>
        <div class="workspace-chip-row review-badges">
          <span class="workspace-chip workspace-chip--info">已归类：{{ model.categorizedFollowUpCount }}</span>
        </div>
      </div>
      <div class="category-export-row">
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportFollowUpTaskDraft">导出后续任务草稿</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.openTaskSuggestionDialog">查看任务建议清单</el-button>
        <span class="quick-filter-hint">按当前待跟进分类结果生成 Markdown，方便直接整理进任务池或复盘文档。</span>
      </div>
      <div class="quick-filter-row">
        <span class="quick-filter-label">分类聚焦</span>
        <el-button class="workspace-btn workspace-btn--sm" :class="model.followUpCategoryView === '' ? 'workspace-btn--primary' : 'workspace-btn--ghost'" @click="model.followUpCategoryView = ''">全部分类</el-button>
        <el-button
          v-for="option in model.followUpCategoryOptions"
          :key="option.value"
          class="workspace-btn workspace-btn--sm"
          :class="model.followUpCategoryView === option.value ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="model.followUpCategoryView = option.value"
        >
          {{ option.label }}
        </el-button>
      </div>
      <div v-if="model.followUpCategoryStats.length" class="category-grid">
        <article v-for="item in model.followUpCategoryStats" :key="item.key" class="category-card">
          <div class="category-card-count">{{ item.count }}</div>
          <div class="category-card-title">{{ item.label }}</div>
          <div class="category-card-desc">{{ item.description }}</div>
        </article>
      </div>
      <div v-else class="empty-review-hint">
        当前还没有待跟进分类，先把某些明细标记为待跟进，再选择对应原因分类。
      </div>
    </section>

    <section class="workspace-section-card table-panel table-panel--single-frame">
      <el-table :data="model.filteredRows" stripe style="width: 100%">
        <el-table-column type="expand">
          <template #default="props">
            <div class="workspace-detail-table">
            <el-table :data="props.row.detailList" stripe style="width: 100%">
              <el-table-column prop="modelName" label="模型名称" />
              <el-table-column prop="costToken" label="消费token数" />
              <el-table-column prop="statusDesc" label="状态">
                <template #default="scope">
                  <span :class="['workspace-inline-tag', getStatusTagClass(scope.row.status)]">{{ scope.row.statusDesc }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="costTime" label="耗时(ms)" />
              <el-table-column prop="failReason" label="失败原因">
                <template #default="scope">
                  <div class="workspace-cell-stack">
                    <el-tooltip placement="top">
                      <template #content>
                        <div class="new-line">
                          {{ scope.row.failReason }}
                        </div>
                      </template>
                      <p class="ellipsis">{{ scope.row.failReason }}</p>
                    </el-tooltip>
                    <el-button v-if="scope.row.failReason" text class="workspace-btn workspace-btn--text workspace-text-btn--compact" @click="model.openDetailDialog('失败原因', scope.row.failReason)">查看全文</el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="startTime" label="开始时间" />
              <el-table-column prop="endTime" label="结束时间" />
              <el-table-column prop="userInput" label="查询词">
                <template #default="scope">
                  <div class="workspace-cell-stack">
                    <el-tooltip placement="top">
                      <template #content>
                        <div class="new-line">
                          {{ scope.row.userInput }}
                        </div>
                      </template>
                      <p class="ellipsis">{{ scope.row.userInput }}</p>
                    </el-tooltip>
                    <div class="workspace-action-strip workspace-action-strip--tight">
                      <el-button text class="workspace-btn workspace-btn--text workspace-text-btn--compact" @click="model.copyText(scope.row.userInput, '查询词')">复制</el-button>
                      <el-button text class="workspace-btn workspace-btn--text workspace-text-btn--compact" @click="model.openDetailDialog('查询词', scope.row.userInput)">查看全文</el-button>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="assistantMessage" label="响应结果">
                <template #default="scope">
                  <div class="workspace-cell-stack">
                    <el-tooltip placement="top">
                      <template #content>
                        <div class="new-line">
                          {{ scope.row.assistantMessage }}
                        </div>
                      </template>
                      <p class="ellipsis workspace-clamp-3">{{ scope.row.assistantMessage }}</p>
                    </el-tooltip>
                    <div class="workspace-action-strip workspace-action-strip--tight">
                      <el-button text class="workspace-btn workspace-btn--text workspace-text-btn--compact" @click="model.copyText(scope.row.assistantMessage, '响应结果')">复制</el-button>
                      <el-button text class="workspace-btn workspace-btn--text workspace-text-btn--compact" @click="model.openDetailDialog('响应结果', scope.row.assistantMessage)">查看全文</el-button>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="复盘状态" width="150">
                <template #default="scope">
                  <div class="workspace-cell-stack">
                    <span :class="['workspace-chip', getReviewStatusClass(model.getReviewStatus(model.buildReviewKey(props.row, scope.row)))]">
                      {{ model.reviewStatusText[model.getReviewStatus(model.buildReviewKey(props.row, scope.row))] }}
                    </span>
                    <el-button text class="workspace-btn workspace-btn--text workspace-text-btn--compact" @click="model.cycleReviewStatus(model.buildReviewKey(props.row, scope.row))">
                      {{ model.nextReviewActionText(model.buildReviewKey(props.row, scope.row)) }}
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="跟进分类" width="180">
                <template #default="scope">
                  <div class="workspace-cell-stack">
                    <el-select
                      v-if="model.getReviewStatus(model.buildReviewKey(props.row, scope.row)) === 'followUp'"
                      :model-value="model.getFollowUpCategory(model.buildReviewKey(props.row, scope.row))"
                      class="follow-up-category-select"
                      placeholder="选择分类"
                      size="small"
                      @change="model.updateFollowUpCategory(model.buildReviewKey(props.row, scope.row), $event)"
                    >
                      <el-option
                        v-for="option in model.followUpCategoryOptions"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                      />
                    </el-select>
                    <span v-else class="follow-up-category-placeholder">仅待跟进可分类</span>
                    <span v-if="model.getFollowUpCategory(model.buildReviewKey(props.row, scope.row))" class="follow-up-category-text">
                      {{ model.getFollowUpCategoryLabel(model.buildReviewKey(props.row, scope.row)) }}
                    </span>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="记录" min-width="320">
          <template #default="scope">
            <div class="workspace-table-stack">
              <div class="workspace-table-heading">{{ scope.row.appName || '未命名应用' }}</div>
              <div class="workspace-table-subtext">调用人：{{ scope.row.username || '--' }} · ID {{ scope.row.id || '--' }}</div>
              <div class="workspace-table-subtext">知识库：{{ scope.row.libName || '未绑定知识库' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态与耗时" min-width="180">
          <template #default="scope">
            <div class="workspace-inline-tags">
              <span :class="['workspace-inline-tag', getStatusTagClass(scope.row.status)]">{{ scope.row.statusDesc || '未知状态' }}</span>
              <span class="workspace-inline-tag workspace-inline-tag--soft">{{ formatCostTime(scope.row.costTime) }}</span>
              <span class="workspace-inline-tag workspace-inline-tag--soft">明细 {{ scope.row.detailList?.length || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="220">
          <template #default="scope">
            <el-tooltip placement="top">
              <template #content>
                <div class="new-line">
                  {{ scope.row.failReason || '无' }}
                </div>
              </template>
              <div class="workspace-table-note workspace-table-note--muted ellipsis">{{ scope.row.failReason || '无' }}</div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="时间范围" min-width="220">
          <template #default="scope">
            <div class="workspace-table-stack">
              <div class="workspace-table-subtext">开始：{{ scope.row.startTime || '--' }}</div>
              <div class="workspace-table-subtext">结束：{{ scope.row.endTime || '--' }}</div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useInvokeRecordFeatureModel } from '../../composables/useInvokeRecordFeature'

const model = useInvokeRecordFeatureModel()

function getStatusTagClass(status: number) {
  return Number(status) === 1 ? 'workspace-inline-tag--success' : 'workspace-inline-tag--danger'
}

function formatCostTime(value: number | string) {
  const num = Number(value || 0)
  if (!Number.isFinite(num)) {
    return '--'
  }
  return `${num}ms`
}

function getReviewStatusClass(status: string) {
  if (status === 'reviewed') {
    return 'workspace-chip--success'
  }
  if (status === 'followUp') {
    return 'workspace-chip--warning'
  }
  return 'workspace-chip--info'
}
</script>

<style scoped>
.tab-panel,
.review-panel,
.table-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.review-panel,
.table-panel {
  padding: 16px;
}

.table-panel--single-frame {
  padding: 0;
  border: none;
  background: transparent;
  box-shadow: none;
  overflow: visible;
}

.review-header,
.priority-card-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.review-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--space-text);
}

.review-desc,
.quick-filter-hint,
.review-header-note,
.priority-card-meta,
.priority-label,
.empty-review-hint,
.category-card-desc,
.follow-up-category-placeholder,
.follow-up-category-text {
  color: var(--space-text-soft);
}

.review-desc,
.priority-text,
.empty-review-hint {
  line-height: 1.7;
}

.review-desc {
  margin-top: 6px;
  font-size: 13px;
}

.quick-filter-row,
.category-export-row,
.priority-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-badges {
  justify-content: flex-end;
}

.quick-filter-row,
.category-export-row {
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.review-hint-row {
  margin-top: 8px;
}

.quick-filter-hint {
  font-size: 12px;
  color: var(--space-muted);
}

.quick-filter-label,
.priority-label {
  font-size: 13px;
  font-weight: 700;
}

.priority-grid,
.category-grid {
  display: grid;
  gap: 12px;
}

.priority-grid {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.category-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.priority-card,
.category-card {
  border-radius: 16px;
  border: 1px solid rgba(99, 91, 255, 0.12);
}

.priority-card {
  padding: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 251, 255, 0.95));
  box-shadow: var(--space-shadow);
}

.category-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.96);
}

.priority-card-title,
.category-card-title {
  color: var(--space-text);
  font-weight: 700;
}

.priority-card-title {
  font-size: 15px;
  line-height: 1.5;
}

.priority-card-id {
  margin-left: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--space-text-soft);
}

.priority-card-meta,
.category-card-desc,
.follow-up-category-placeholder,
.follow-up-category-text {
  font-size: 12px;
  line-height: 1.6;
}

.priority-block,
.acceptance-item-block {
  margin-top: 10px;
}

.priority-text {
  margin-top: 4px;
  color: var(--space-text);
  font-size: 13px;
  word-break: break-word;
}

.priority-text--danger {
  color: #b42318;
}

.category-card-count {
  color: var(--space-primary);
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
}

.category-card-title {
  margin-top: 10px;
  font-size: 14px;
}

.follow-up-category-text {
  color: var(--space-primary);
  font-weight: 600;
}

.follow-up-category-select {
  width: 150px;
}

.workspace-detail-table {
  margin: 4px 0;
}

@media (max-width: 900px) {
  .review-header,
  .priority-card-head {
    flex-direction: column;
  }

  .review-badges,
  .priority-tag-group {
    justify-content: flex-start;
  }

  .follow-up-category-select {
    width: 100%;
  }
}

:deep(.workspace-btn--sm .el-icon) {
  width: 12px;
  height: 12px;
  font-size: 12px;
}
</style>
