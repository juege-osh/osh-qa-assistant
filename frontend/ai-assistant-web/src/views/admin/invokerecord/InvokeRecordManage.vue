<template>
  <div class="page-shell admin-invoke-record-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">平台巡检</span>
        <span class="workspace-context-note">从平台视角查看所有用户和应用的调用行为，先按范围缩小样本，再继续下钻失败原因和模型明细。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">总调用 {{ totalCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">成功率 {{ successRate }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">平均耗时 {{ avgCostTimeDisplay }}</span>
      </div>
    </section>

    <section class="workspace-section-card overview-panel workspace-dashboard-panel">
      <div class="overview-head workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="panel-title panel-title--md">调用工作区</div>
          <div class="panel-desc workspace-panel-desc">先判断平台稳定性和当前记录规模，再按用户、时间和失败原因逐步缩小排查范围。</div>
        </div>
        <div class="workspace-inline-tags">
          <span :class="['workspace-inline-tag', healthToneClass]">{{ healthLabel }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">当前结果 {{ resultCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">失败 {{ failCountDisplay }}</span>
        </div>
      </div>

      <section class="stats-grid overview-metrics-grid workspace-metrics-grid">
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--total">
          <div class="stat-label">总调用次数</div>
          <div class="stat-value">{{ totalCountDisplay }}</div>
          <div class="stat-help">当前统计周期内所有请求的累计规模。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
          <div class="stat-label">成功率</div>
          <div class="stat-value">{{ successRate }}</div>
          <div class="stat-help">快速判断平台当前是否处于稳定区间。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--token">
          <div class="stat-label">累计 Token</div>
          <div class="stat-value">{{ totalCostTokenDisplay }}</div>
          <div class="stat-help">结合业务峰值观察整体资源消耗趋势。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--time">
          <div class="stat-label">平均耗时</div>
          <div class="stat-value">{{ avgCostTimeDisplay }}</div>
          <div class="stat-help">耗时抬高时优先排查检索、重排和模型响应。</div>
        </article>
      </section>
    </section>

    <section class="workspace-section-card summary-panel admin-invoke-summary-panel">
      <div class="panel-title panel-title--md">排查建议</div>
      <div class="summary-list">
        <div class="summary-item">{{ healthSummary }}</div>
        <div v-for="item in watchList" :key="item" class="summary-item">{{ item }}</div>
      </div>
    </section>

    <section class="workspace-section-card admin-invoke-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="panel-title panel-title--md">当前关注点</div>
          <div class="workspace-panel-desc">把平台调用状态翻译成更直接的排查动作，方便决定先盯失败率、耗时，还是先看资源消耗。</div>
        </div>
      </div>
      <div class="workspace-tip-grid admin-invoke-tip-grid">
        <article
          v-for="item in recordFocusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'admin-invoke-tip-card', `admin-invoke-tip-card--${item.tone}`]"
        >
          <div class="admin-invoke-tip-card__head">
            <span :class="['admin-invoke-tip-card__dot', `admin-invoke-tip-card__dot--${item.tone}`]"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
        </article>
      </div>
    </section>

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">调用巡检</div>
        <div class="toolbar-title">筛选记录</div>
        <div class="toolbar-desc">
          结合失败原因、时间范围和用户名筛选，可以快速定位是单个用户问题、某个应用问题，还是平台级异常。
        </div>
      </div>
      <div class="toolbar-actions workspace-toolbar-actions">
          <el-form :model="searchData" :inline="true" class="workspace-toolbar-form">
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--sm">
            <el-input type="text" placeholder="用户名" v-model="searchData.username" clearable></el-input>
          </el-form-item>
          <el-form-item label="状态" class="workspace-toolbar-field workspace-toolbar-field--sm">
            <el-select v-model="searchData.status" clearable>
              <el-option label="全部" value=""></el-option>
              <el-option label="失败" value="0"></el-option>
              <el-option label="成功" value="1"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--date">
            <el-date-picker type="datetime" v-model="searchData.startTime" format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间" />
          </el-form-item>
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--date">
            <el-date-picker type="datetime" v-model="searchData.endTime" format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间" />
          </el-form-item>
          <el-form-item>
            <el-button @click="loadTable" type="primary" class="workspace-btn workspace-btn--primary">查询</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <section class="workspace-section-card records-panel workspace-detail-host">
      <div class="records-head workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="panel-title panel-title--md">记录明细</div>
          <div class="panel-desc workspace-panel-desc">展开单条记录可以继续查看模型明细、失败原因、耗时、查询词和响应结果。</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--soft">成功 {{ successCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">当前页 {{ pageNowDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">每页 {{ searchData.pageSize }}</span>
        </div>
      </div>

      <el-table :data="tableData.rows" stripe style="width: 100%">
        <el-table-column type="expand">
          <template v-slot:default="props">
            <div class="workspace-detail-table">
            <el-table :data="props.row.detailList" stripe style="width: 100%">
              <el-table-column prop="modelName" label="模型名称">
              </el-table-column>
              <el-table-column prop="costToken" label="消费token数">
              </el-table-column>
              <el-table-column prop="statusDesc" label="状态">
                <template v-slot:default="scope">
                  <span :class="['workspace-inline-tag', getStatusTagClass(scope.row.status)]">{{ scope.row.statusDesc }}</span>
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
              <el-table-column prop="userInput" label="查询词">
                <template v-slot:default="scope">
                  <el-tooltip placement="top">
                    <template v-slot:content>
                      <div class="new-line">
                        {{ scope.row.userInput }}
                      </div>
                    </template>
                    <p class="ellipsis">{{ scope.row.userInput }}</p>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="assistantMessage" label="响应结果">
                <template v-slot:default="scope">
                  <el-tooltip placement="top">
                    <template v-slot:content>
                      <div class="new-line">
                        {{ scope.row.assistantMessage }}
                      </div>
                    </template>
                    <p class="ellipsis">{{ scope.row.assistantMessage }}</p>
                  </el-tooltip>
                </template>
              </el-table-column>
            </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="记录" min-width="320">
          <template v-slot:default="scope">
            <div class="workspace-table-stack">
              <div class="workspace-table-heading">{{ scope.row.appName || '未命名应用' }}</div>
              <div class="workspace-table-subtext">调用人：{{ scope.row.username || '--' }} · ID {{ scope.row.id || '--' }}</div>
              <div class="workspace-table-subtext">知识库：{{ scope.row.libName || '未绑定知识库' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态与耗时" min-width="180">
          <template v-slot:default="scope">
            <div class="workspace-inline-tags">
              <span :class="['workspace-inline-tag', getStatusTagClass(scope.row.status)]">{{ scope.row.statusDesc || '未知状态' }}</span>
              <span class="workspace-inline-tag workspace-inline-tag--soft">{{ formatCostTime(scope.row.costTime) }}</span>
              <span class="workspace-inline-tag workspace-inline-tag--soft">明细 {{ scope.row.detailList?.length || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="220">
          <template v-slot:default="scope">
            <el-tooltip placement="top">
              <template v-slot:content>
                <div class="new-line">
                  {{ scope.row.failReason || '无' }}
                </div>
              </template>
              <div class="workspace-table-note workspace-table-note--muted ellipsis">{{ scope.row.failReason || '无' }}</div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="时间范围" min-width="220">
          <template v-slot:default="scope">
            <div class="workspace-table-stack">
              <div class="workspace-table-subtext">开始：{{ scope.row.startTime || '--' }}</div>
              <div class="workspace-table-subtext">结束：{{ scope.row.endTime || '--' }}</div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="mt-dot5 workspace-section-card pagination-panel">
      <el-pagination @size-change="handlePageSizeChange" @current-change="handlePageNowChange"
        :current-page="searchData.pageNow" :page-sizes="[10, 30, 50]" :page-size="searchData.pageSize"
        layout="total, sizes, prev, pager, next, jumper" :total="tableData.total">
      </el-pagination>
    </section>
  </div>
</template>
<script setup name='InvokeRecordManage' lang='ts'>
import { reactive, onMounted, computed } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageInvokeRecordApi, queryInvokeRecordOverviewApi } from '@/api/admin/invokeRecordApi';
let searchFormData = reactive({
  username: '',
  status: "",
  startTime: null,
  endTime: null,
})

let {
  searchData,
  tableData,
  loadTable,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageInvokeRecordApi })

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

function normalizeNumber(value: unknown) {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num : 0
}

function formatCount(value: unknown) {
  return normalizeNumber(value).toLocaleString('zh-CN')
}

const totalCountDisplay = computed(() => formatCount(overview.totalCount))
const successCountDisplay = computed(() => formatCount(overview.successCount))
const failCountDisplay = computed(() => formatCount(overview.failCount))
const totalCostTokenDisplay = computed(() => formatCount(overview.totalCostToken))
const avgCostTimeDisplay = computed(() => `${formatCount(overview.avgCostTime)}ms`)
const resultCountDisplay = computed(() => formatCount(tableData.total))
const pageNowDisplay = computed(() => formatCount(searchData.pageNow))
const successRateValue = computed(() => normalizeNumber(String(successRate.value).replace('%', '')))

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

const healthSummary = computed(() => {
  if (successRateValue.value >= 95) {
    return '当前平台整体较稳定，建议继续观察高耗时请求和异常 Token 增长，优先做体验与成本优化。'
  }
  if (successRateValue.value >= 80) {
    return '当前平台基本可用，但已经出现需要关注的失败或慢请求，建议优先缩小到具体用户、时间段和失败原因。'
  }
  return '当前平台成功率偏低，建议优先检查模型配置、知识库链路和 SSE 返回过程中的异常。'
})

const watchList = computed(() => {
  const items: string[] = []
  const failCount = normalizeNumber(overview.failCount)
  const avgCostTime = normalizeNumber(overview.avgCostTime)
  const totalCostToken = normalizeNumber(overview.totalCostToken)

  if (failCount > 0) {
    items.push(`当前累计失败 ${formatCount(failCount)} 次，建议先在表格中按失败状态筛选，再聚焦真实报错与中断点。`)
  } else {
    items.push('当前未发现失败记录，可以重点关注响应耗时和业务回答质量是否持续稳定。')
  }

  if (avgCostTime >= 5000) {
    items.push(`平均耗时已达到 ${formatCount(avgCostTime)}ms，建议优先排查检索链路、重排耗时和模型响应速度。`)
  } else {
    items.push(`平均耗时约 ${formatCount(avgCostTime)}ms，当前速度相对平稳，可以更多关注命中效果与输出质量。`)
  }

  items.push(`累计 Token 已到 ${formatCount(totalCostToken)}，如果增长异常，建议检查上下文长度、重复调用和高频访问来源。`)
  return items
})
const recordFocusCards = computed(() => {
  const failCount = normalizeNumber(overview.failCount)
  const avgCostTime = normalizeNumber(overview.avgCostTime)
  const totalCostToken = normalizeNumber(overview.totalCostToken)

  return [
    {
      title: successRateValue.value >= 95 ? '平台成功率处于稳定区间' : successRateValue.value >= 80 ? '失败率已经需要重点关注' : '优先排查失败链路',
      desc: successRateValue.value >= 95
        ? `当前成功率保持在 ${successRate.value}，更适合继续盯高耗时请求和局部异常峰值。`
        : successRateValue.value >= 80
          ? `当前成功率为 ${successRate.value}，建议先按失败状态缩小到异常时间段、用户和应用。`
          : `当前成功率仅为 ${successRate.value}，建议优先检查模型配置、知识链路和流式返回过程是否异常。`,
      tone: successRateValue.value >= 95 ? 'success' : successRateValue.value >= 80 ? 'warning' : 'danger'
    },
    {
      title: avgCostTime >= 5000 ? '慢请求适合优先下钻明细' : '耗时整体相对平稳',
      desc: avgCostTime >= 5000
        ? `当前平均耗时约 ${formatCount(avgCostTime)}ms，建议优先看检索、重排和模型响应三个阶段的耗时分布。`
        : `当前平均耗时约 ${formatCount(avgCostTime)}ms，下一步可以更多关注回答质量和异常个案。`,
      tone: avgCostTime >= 5000 ? 'warning' : 'success'
    },
    {
      title: totalCostToken > 0 ? 'Token 消耗适合继续联动峰值排查' : '当前仍需积累更多消耗样本',
      desc: totalCostToken > 0
        ? `当前累计 Token 已到 ${formatCount(totalCostToken)}，如果增长异常，建议继续检查上下文长度、重复调用和高频来源。`
        : '当前还没有明显的 Token 消耗样本，更适合先观察请求规模和基础稳定性。',
      tone: totalCostToken > 0 ? 'success' : 'warning'
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

function loadOverview() {
  queryInvokeRecordOverviewApi().then(result => {
    Object.assign(overview, result.data || {})
  })
}

onMounted(() => {
  loadTable()
  loadOverview()
})
</script>
<style scoped>
.admin-invoke-record-page {
  gap: 16px;
}

.admin-invoke-summary-panel,
.admin-invoke-focus-panel,
.records-panel {
  padding: 18px 20px;
}

.admin-invoke-tip-grid {
  margin-top: 14px;
}

.admin-invoke-tip-card {
  position: relative;
  overflow: hidden;
}

.admin-invoke-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.admin-invoke-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.admin-invoke-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.admin-invoke-tip-card--danger::before {
  background: linear-gradient(180deg, #ef4444 0%, #dc2626 100%);
}

.admin-invoke-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-invoke-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex-shrink: 0;
  background: rgba(148, 163, 184, 0.9);
}

.admin-invoke-tip-card__dot--success {
  background: #12b76a;
  box-shadow: 0 0 0 4px rgba(18, 183, 106, 0.12);
}

.admin-invoke-tip-card__dot--warning {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.14);
}

.admin-invoke-tip-card__dot--danger {
  background: #ef4444;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.12);
}

.workspace-detail-table {
  margin: 4px 0;
}
</style>
