<template>
  <div class="page-shell admin-invoke-record-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">平台巡检</span>
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

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">调用巡检</div>
        <div class="toolbar-title">筛选记录</div>
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
