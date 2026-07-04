<template>
  <div class="tab-panel">
    <section class="workspace-section-card records-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="workspace-section-title workspace-section-title--md">当前记录重点</div>
          <div class="workspace-panel-desc">先确认当前页记录规模和可操作动作，再决定是先导出、继续抽样，还是直接整理成验收条目。</div>
        </div>
      </div>
      <div class="workspace-tip-grid records-tip-grid">
        <article
          v-for="item in recordFocusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'records-tip-card', item.tone ? `records-tip-card--${item.tone}` : '']"
        >
          <div class="records-tip-card__head">
            <span :class="['records-tip-card__dot', item.tone ? `records-tip-card__dot--${item.tone}` : '']"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
        </article>
      </div>
    </section>

    <section class="workspace-section-card workspace-section-panel">
      <div class="workspace-section-head">
        <div>
          <div class="workspace-section-title--md">记录操作</div>
          <div class="workspace-section-desc">先看原始调用记录，再决定是否导出、继续抽样，或转成验收条目。</div>
        </div>
        <div class="workspace-action-strip">
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportCurrentRows">导出当前结果</el-button>
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportAcceptanceDraft">导出验收草稿</el-button>
        </div>
      </div>
      <div class="workspace-chip-row workspace-chip-row--wide records-summary-row">
        <span class="workspace-chip workspace-chip--info">当前页 {{ currentPageCount }}</span>
        <span class="workspace-chip workspace-chip--info">当前结果 {{ paginationTotalDisplay }}</span>
        <span class="workspace-chip workspace-chip--warning">失败 {{ currentPageFailCount }}</span>
      </div>
    </section>

    <section class="workspace-section-card table-panel table-panel--single-frame">
      <div v-if="!model.recordRows.length" class="workspace-empty-panel records-empty-panel">
        <div class="workspace-empty-title">当前没有匹配记录</div>
        <div class="workspace-empty-desc">可以回到上方筛选区重新调整应用、问题关键词或时间范围，再继续查看明细。</div>
      </div>
      <el-table v-else :data="model.recordRows" stripe style="width: 100%">
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
                    <el-button
                      v-if="scope.row.failReason"
                      text
                      class="workspace-btn workspace-btn--text workspace-text-btn--compact"
                      @click="model.openDetailDialog('失败原因', scope.row.failReason)"
                    >
                      查看全文
                    </el-button>
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
              <el-table-column label="验收整理" width="180">
                <template #default="scope">
                  <div class="workspace-cell-stack">
                    <el-button
                      text
                      class="workspace-btn workspace-btn--text workspace-text-btn--compact"
                      @click="model.copyAcceptanceEntry(props.row, scope.row)"
                    >
                      复制验收条目
                    </el-button>
                    <el-button
                      text
                      class="workspace-btn workspace-btn--text workspace-text-btn--compact"
                      @click="model.openAcceptanceDialog(props.row, scope.row)"
                    >
                      查看验收草稿
                    </el-button>
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

    <section class="mt-dot5 workspace-section-card pagination-panel">
      <el-pagination
        :current-page="model.searchData.pageNow"
        :page-sizes="[10, 30, 50]"
        :page-size="model.searchData.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="model.paginationTotal"
        @size-change="model.handlePageSizeChange"
        @current-change="model.handlePageNowChange"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useInvokeRecordFeatureModel } from '../../composables/useInvokeRecordFeature'

const model = useInvokeRecordFeatureModel()

const currentPageCount = computed(() => model.recordRows.length)
const paginationTotalDisplay = computed(() => model.paginationTotal)
const currentPageFailCount = computed(() => model.recordRows.filter((row: any) => Number(row.status) !== 1).length)
const recordFocusCards = computed(() => {
  return [
    {
      title: currentPageCount.value ? `当前页有 ${currentPageCount.value} 条记录` : '当前页暂无可查看记录',
      desc: currentPageCount.value
        ? `当前筛选结果共 ${paginationTotalDisplay.value} 条，适合先抽样看明细，再决定是否继续缩小范围。`
        : '建议先回到上方筛选区调整条件，再重新进入记录列表查看原始样本。',
      tone: currentPageCount.value ? 'success' : 'warning'
    },
    {
      title: currentPageFailCount.value ? '当前页存在失败样本' : '当前页样本整体可用',
      desc: currentPageFailCount.value
        ? `当前页有 ${currentPageFailCount.value} 条失败记录，优先展开看失败原因、耗时和模型明细会更高效。`
        : '当前页没有明显失败样本，可以更多关注回答质量、上下文衔接和长回答表现。',
      tone: currentPageFailCount.value ? 'danger' : 'success'
    },
    {
      title: currentPageCount.value ? '可以直接导出或转验收' : '先重新准备可用样本',
      desc: currentPageCount.value
        ? '当前页记录已经可以直接导出，或把有代表性的明细复制成验收条目继续整理。'
        : '当记录重新出现后，可以先导出当前结果，再把关键样本整理进验收批次。',
      tone: currentPageCount.value ? 'warning' : 'warning'
    }
  ] as const
})

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
</script>

<style scoped>
.tab-panel,
.records-focus-panel,
.table-panel,
.pagination-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.records-tip-grid {
  margin-top: 14px;
}

.records-tip-card {
  position: relative;
  overflow: hidden;
}

.records-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.records-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.records-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.records-tip-card--danger::before {
  background: linear-gradient(180deg, #ef4444 0%, #dc2626 100%);
}

.records-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.records-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.78);
  flex-shrink: 0;
}

.records-tip-card__dot--success {
  background: #12b76a;
}

.records-tip-card__dot--warning {
  background: #f59e0b;
}

.records-tip-card__dot--danger {
  background: #ef4444;
}

.records-summary-row {
  gap: 10px;
}

.records-focus-panel,
.table-panel,
.pagination-panel {
  padding: 18px;
}

.table-panel--single-frame {
  padding: 0;
  border: none;
  background: transparent;
  box-shadow: none;
  overflow: visible;
}

.records-empty-panel {
  margin-bottom: 0;
}

.workspace-detail-table {
  margin: 4px 0;
}

@media (max-width: 900px) {
  .workspace-section-head {
    flex-direction: column;
  }

  .records-tip-grid {
    grid-template-columns: 1fr;
  }
}
</style>
