<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">调用记录分析</div>
      <div class="hero-subtitle">
        在这里复盘每一次问答请求，查看调用状态、耗时、失败原因、模型明细和实际输入输出，便于评估知识库和模型效果。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">总调用：{{ overview.totalCount }}</span>
        <span class="hero-badge">成功：{{ overview.successCount }}</span>
        <span class="hero-badge">失败：{{ overview.failCount }}</span>
      </div>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <div class="stat-label">总调用次数</div>
        <div class="stat-value">{{ overview.totalCount }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">成功率</div>
        <div class="stat-value">{{ successRate }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">累计 Token</div>
        <div class="stat-value">{{ overview.totalCostToken }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">平均耗时</div>
        <div class="stat-value">{{ overview.avgCostTime }}ms</div>
      </article>
    </section>

    <section class="toolbar-panel glass-panel">
      <div class="toolbar-copy">
        <div class="toolbar-title">筛选记录</div>
        <div class="toolbar-desc">
          你可以按应用、问题关键词、状态和时间范围过滤记录，快速定位失败请求、慢请求和特定场景下的模型表现。
        </div>
      </div>
      <div class="toolbar-actions">
        <el-form :model="searchData" :inline="true">
          <el-form-item>
            <el-input v-model="searchData.appName" placeholder="按应用名称筛选" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="searchData.userInputKeyword" placeholder="按问题关键词筛选" clearable style="width: 220px" />
          </el-form-item>
          <el-form-item label="状态:" style="width: 150px;">
            <el-select v-model="searchData.status" style="width: 150px">
              <el-option label="全部" value=""></el-option>
              <el-option label="失败" value="0"></el-option>
              <el-option label="成功" value="1"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item style="width: 200px;">
            <el-date-picker type="datetime" v-model="searchData.startTime" format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间" />
          </el-form-item>
          <el-form-item style="width: 200px;">
            <el-date-picker type="datetime" v-model="searchData.endTime" format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间" />
          </el-form-item>
          <el-form-item>
            <el-button @click="loadTable" type="primary">查询</el-button>
          </el-form-item>
          <el-form-item>
            <el-button class="workspace-btn workspace-btn--ghost" @click="resetFilters">重置</el-button>
          </el-form-item>
          <el-form-item>
            <el-button class="workspace-btn workspace-btn--ghost" @click="exportCurrentRows">导出当前结果</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <section class="glass-panel review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">MVP 验收提示</div>
          <div class="review-desc">建议优先检查命中问题、可信、易懂、失败体面四项，再结合调用耗时和失败原因定位问题。</div>
        </div>
        <div class="review-badges">
          <span class="review-badge">当前记录数：{{ filteredRows.length }}</span>
          <span class="review-badge">失败记录：{{ failRowCount }}</span>
          <span class="review-badge">慢请求：{{ slowRowCount }}</span>
          <span class="review-badge">长回答：{{ longAnswerRowCount }}</span>
        </div>
      </div>
      <div class="quick-filter-row">
        <span class="quick-filter-label">快速聚焦</span>
        <el-button
          class="workspace-btn"
          :class="quickView === 'all' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'all'"
        >
          全部记录
        </el-button>
        <el-button
          class="workspace-btn"
          :class="quickView === 'fail' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'fail'"
        >
          只看失败
        </el-button>
        <el-button
          class="workspace-btn"
          :class="quickView === 'slow' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'slow'"
        >
          只看慢请求
        </el-button>
        <el-button
          class="workspace-btn"
          :class="quickView === 'long' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'long'"
        >
          只看长回答
        </el-button>
        <span class="quick-filter-hint">慢请求默认按耗时 ≥ 5000ms，长回答默认按回答长度 ≥ 200 字符判断。</span>
      </div>
      <div class="review-header-note">
        <span>建议顺序：先看失败，再看慢请求，最后抽样看长回答是否真正答题且有依据。</span>
      </div>
    </section>

    <section class="glass-panel review-panel compact-panel">
      <div class="review-header">
        <div>
          <div class="review-title">当前聚焦说明</div>
          <div class="review-desc">{{ currentQuickViewDesc }}</div>
        </div>
      </div>
    </section>

    <!-- 表格   -->
    <section class="glass-panel table-panel">
      <el-table :data="filteredRows" stripe :border="true" style="width: 100%">
        <el-table-column type="expand">
          <template v-slot:default="props">
            <el-table :data="props.row.detailList" stripe :border="true" style="width: 100%">
              <el-table-column prop="modelName" label="模型名称">
              </el-table-column>
              <el-table-column prop="costToken" label="消费token数">
              </el-table-column>
              <el-table-column prop="statusDesc" label="状态">
                <template v-slot:default="scope">
                  <el-tag v-if="scope.row.status === 1" type="success">{{ scope.row.statusDesc }}</el-tag>
                  <el-tag v-if="scope.row.status === 0" type="danger">{{ scope.row.statusDesc }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="costTime" label="耗时(ms)">
              </el-table-column>
              <el-table-column prop="failReason" label="失败原因">
                <template v-slot:default="scope">
                  <div class="cell-stack">
                    <el-tooltip placement="top">
                      <template v-slot:content>
                        <div class="new-line">
                          {{ scope.row.failReason }}
                        </div>
                      </template>
                      <p class="ellipsis">{{ scope.row.failReason }}</p>
                    </el-tooltip>
                    <el-button
                      v-if="scope.row.failReason"
                      text
                      class="workspace-btn workspace-btn--text record-text-btn"
                      @click="openDetailDialog('失败原因', scope.row.failReason)"
                    >
                      查看全文
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="startTime" label="开始时间">
              </el-table-column>
              <el-table-column prop="endTime" label="结束时间">
              </el-table-column>
              <el-table-column prop="userInput" label="查询词">
                <template v-slot:default="scope">
                  <div class="cell-stack">
                    <el-tooltip placement="top">
                      <template v-slot:content>
                        <div class="new-line">
                          {{ scope.row.userInput }}
                        </div>
                      </template>
                      <p class="ellipsis">{{ scope.row.userInput }}</p>
                    </el-tooltip>
                    <div class="record-actions">
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="copyText(scope.row.userInput, '查询词')">复制</el-button>
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="openDetailDialog('查询词', scope.row.userInput)">查看全文</el-button>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="assistantMessage" label="响应结果">
                <template v-slot:default="scope">
                  <div class="cell-stack">
                    <el-tooltip placement="top">
                      <template v-slot:content>
                        <div class="new-line">
                          {{ scope.row.assistantMessage }}
                        </div>
                      </template>
                      <p class="ellipsis multi-line-ellipsis">{{ scope.row.assistantMessage }}</p>
                    </el-tooltip>
                    <div class="record-actions">
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="copyText(scope.row.assistantMessage, '响应结果')">复制</el-button>
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="openDetailDialog('响应结果', scope.row.assistantMessage)">查看全文</el-button>
                    </div>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="系统编号">
        </el-table-column>
        <el-table-column prop="appName" label="所属应用">
        </el-table-column>
        <el-table-column prop="libName" label="所属知识库">
        </el-table-column>
        <el-table-column prop="username" label="调用人">
        </el-table-column>
        <el-table-column prop="statusDesc" label="状态">
          <template v-slot:default="scope">
            <el-tag v-if="scope.row.status === 1" type="success">{{ scope.row.statusDesc }}</el-tag>
            <el-tag v-if="scope.row.status === 0" type="danger">{{ scope.row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因">
          <template v-slot:default="scope">
            <el-tooltip placement="top">
              <template v-slot:content>
                <div class="new-line">
                  {{ scope.row.failReason }}
                </div>
              </template>
              <p class="ellipsis">{{ scope.row.failReason }}</p>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)">
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间">
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间">
        </el-table-column>
      </el-table>
    </section>
    <!-- 分页   -->
    <section class="mt-dot5 glass-panel pagination-panel">
      <el-pagination @size-change="handlePageSizeChange" @current-change="handlePageNowChange"
        :current-page="searchData.pageNow" :page-sizes="[10, 30, 50]" :page-size="searchData.pageSize"
        layout="total, sizes, prev, pager, next, jumper" :total="tableData.total">
      </el-pagination>
    </section>

    <el-dialog v-model="detailDialogVisible" class="record-detail-dialog" :title="detailDialogTitle" width="780px">
      <div class="detail-dialog-copy">
        <div class="detail-dialog-tip">这里展示完整文本，方便直接做人工验收和问题复盘。</div>
        <el-button class="workspace-btn workspace-btn--ghost" @click="copyText(detailDialogContent, detailDialogTitle)">复制内容</el-button>
      </div>
      <pre class="detail-dialog-content">{{ detailDialogContent }}</pre>
    </el-dialog>
  </div>
</template>
<script setup name='InvokeRecordManage' lang='ts'>
import { computed, reactive, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useTable } from '@/hooks/useTable';
import { pageInvokeRecordApi, queryInvokeRecordOverviewApi } from '@/api/workspace/invokeRecordApi';
let searchFormData = reactive({
  appName: "",
  userInputKeyword: "",
  status: "",
  startTime: null,
  endTime: null
})

let {
  searchData,
  tableData,
  loadTable,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageInvokeRecordApi })

const quickView = ref<'all' | 'fail' | 'slow' | 'long'>('all')
const detailDialogVisible = ref(false)
const detailDialogTitle = ref('')
const detailDialogContent = ref('')

const overview = reactive({
  totalCount: 0,
  successCount: 0,
  failCount: 0,
  totalCostToken: 0,
  avgCostTime: 0
})

const successRate = computed(() => {
  if (!overview.totalCount) {
    return '0%'
  }
  return `${((overview.successCount / overview.totalCount) * 100).toFixed(1)}%`
})

const filteredRows = computed(() => {
  const rows = tableData.rows || []
  if (quickView.value === 'fail') {
    return rows.filter((row: any) => Number(row.status) === 0)
  }
  if (quickView.value === 'slow') {
    return rows.filter((row: any) => Number(row.costTime || 0) >= 5000)
  }
  if (quickView.value === 'long') {
    return rows.filter((row: any) =>
      (row.detailList || []).some((detail: any) => String(detail.assistantMessage || '').length >= 200)
    )
  }
  return rows
})

const failRowCount = computed(() => (tableData.rows || []).filter((row: any) => Number(row.status) === 0).length)
const slowRowCount = computed(() => (tableData.rows || []).filter((row: any) => Number(row.costTime || 0) >= 5000).length)
const longAnswerRowCount = computed(() =>
  (tableData.rows || []).filter((row: any) =>
    (row.detailList || []).some((detail: any) => String(detail.assistantMessage || '').length >= 200)
  ).length
)

const currentQuickViewDesc = computed(() => {
  if (quickView.value === 'fail') {
    return '当前仅显示失败记录，适合优先排查失败体面、错误暴露和明显不可用问题。'
  }
  if (quickView.value === 'slow') {
    return '当前仅显示慢请求，适合排查耗时过长、上下文过重或链路抖动问题。'
  }
  if (quickView.value === 'long') {
    return '当前仅显示长回答，适合重点检查是否真正答题、是否有依据，以及是否存在过度铺陈。'
  }
  return '当前显示全部记录，适合做完整抽样和总体复盘。'
})

function loadOverview() {
  queryInvokeRecordOverviewApi().then(result => {
    Object.assign(overview, result.data || {})
  })
}

async function copyText(text: string, label: string) {
  try {
    await navigator.clipboard.writeText(String(text || ''))
    ElMessage.success(`已复制${label}`)
  } catch {
    ElMessage.error(`复制${label}失败`)
  }
}

function exportCurrentRows() {
  if (!filteredRows.value.length) {
    ElMessage.warning('当前没有可导出的调用记录')
    return
  }
  const lines = [
    '# 调用记录导出',
    `> 导出时间：${new Date().toLocaleString()}`,
    `> 当前记录数：${filteredRows.value.length}`,
    `> 当前聚焦：${currentQuickViewDesc.value}`,
    '',
    '---',
    ''
  ]
  filteredRows.value.forEach((row: any) => {
    lines.push(`## 记录 ${row.id}`)
    lines.push(`- 应用：${row.appName || '-'}`)
    lines.push(`- 知识库：${row.libName || '-'}`)
    lines.push(`- 状态：${row.statusDesc || '-'}`)
    lines.push(`- 耗时：${row.costTime ?? '-'} ms`)
    lines.push(`- 开始时间：${row.startTime || '-'}`)
    lines.push(`- 结束时间：${row.endTime || '-'}`)
    if (row.failReason) {
      lines.push(`- 失败原因：${row.failReason}`)
    }
    lines.push('')
    ;(row.detailList || []).forEach((detail: any, index: number) => {
      lines.push(`### 明细 ${index + 1}`)
      lines.push(`- 模型：${detail.modelName || '-'}`)
      lines.push(`- 状态：${detail.statusDesc || '-'}`)
      lines.push(`- Token：${detail.costToken ?? '-'}`)
      lines.push(`- 查询词：${detail.userInput || '-'}`)
      lines.push('')
      lines.push('```text')
      lines.push(String(detail.assistantMessage || ''))
      lines.push('```')
      lines.push('')
    })
    lines.push('---')
    lines.push('')
  })
  const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `invoke-records-${Date.now()}.md`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('已导出当前调用记录')
}

function resetFilters() {
  searchData.appName = ""
  searchData.userInputKeyword = ""
  searchData.status = ""
  searchData.startTime = null
  searchData.endTime = null
  searchData.pageNow = 1
  quickView.value = 'all'
  loadTable()
}

function openDetailDialog(title: string, content: string) {
  detailDialogTitle.value = title
  detailDialogContent.value = String(content || '')
  detailDialogVisible.value = true
}

onMounted(() => {
  loadTable()
  loadOverview()
})
</script>
<style scoped>
.review-panel,
.table-panel,
.pagination-panel {
  padding: 18px;
}

.review-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.compact-panel {
  padding-top: 14px;
  padding-bottom: 14px;
}

.review-header {
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

.review-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--space-text-soft);
}

.review-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.review-badge {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(237, 245, 255, 0.9);
  color: var(--space-primary-strong);
  font-size: 12px;
  font-weight: 600;
}

.quick-filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.quick-filter-label {
  color: var(--space-text);
  font-size: 13px;
  font-weight: 700;
}

.quick-filter-hint,
.review-header-note {
  color: var(--space-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.cell-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.record-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.record-text-btn {
  min-height: 24px !important;
  padding: 0 !important;
  font-size: 12px;
}

.multi-line-ellipsis {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: normal;
}

.detail-dialog-copy {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.detail-dialog-tip {
  color: var(--space-text-soft);
  font-size: 13px;
}

.detail-dialog-content {
  max-height: 55vh;
  overflow: auto;
  margin: 0;
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  color: var(--space-text);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 900px) {
  .review-header {
    flex-direction: column;
  }

  .review-badges {
    justify-content: flex-start;
  }

  .detail-dialog-copy {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
