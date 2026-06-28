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
          <el-form-item>
            <el-button class="workspace-btn workspace-btn--ghost" @click="exportAcceptanceDraft">导出验收草稿</el-button>
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
          <span class="review-badge">待跟进：{{ followUpCount }}</span>
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
        <el-button
          class="workspace-btn"
          :class="quickView === 'followUp' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'followUp'"
        >
          只看待跟进
        </el-button>
        <el-button
          class="workspace-btn"
          :class="quickView === 'reviewed' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'reviewed'"
        >
          只看已复盘
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

    <section class="glass-panel review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">优先复盘清单</div>
          <div class="review-desc">这里按失败、慢请求、长回答等规则自动挑出值得优先检查的记录，只做排查建议，不替代人工验收结论。</div>
        </div>
        <div class="review-badges">
          <span class="review-badge">待优先复盘：{{ priorityReviewList.length }}</span>
        </div>
      </div>
      <div v-if="priorityReviewList.length" class="priority-grid">
        <article v-for="item in priorityReviewList" :key="item.key" class="priority-card">
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
            <div class="priority-tag-group">
              <span
                v-for="tag in item.riskTags"
                :key="`${item.key}-${tag.key}`"
                class="priority-tag"
                :class="`priority-tag--${tag.tone}`"
              >
                {{ tag.label }}
              </span>
              <span v-if="getReviewStatus(item.key) !== 'pending'" class="priority-tag priority-tag--success">
                {{ reviewStatusText[getReviewStatus(item.key)] }}
              </span>
            </div>
          </div>
          <div class="priority-block">
            <div class="priority-label">用户问题</div>
            <div class="priority-text">{{ item.detail.userInput || '-' }}</div>
          </div>
          <div class="priority-block">
            <div class="priority-label">回答摘要</div>
            <div class="priority-text">{{ summarizeDisplayText(item.detail.assistantMessage) }}</div>
          </div>
          <div class="priority-block" v-if="item.detail.failReason || item.row.failReason">
            <div class="priority-label">失败原因</div>
            <div class="priority-text priority-text--danger">{{ item.detail.failReason || item.row.failReason }}</div>
          </div>
          <div class="priority-actions">
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="copyText(item.detail.userInput, '查询词')">复制问题</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="openDetailDialog('响应结果', item.detail.assistantMessage)">查看回答</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="copyAcceptanceEntry(item.row, item.detail)">复制验收条目</el-button>
            <el-button
              text
              class="workspace-btn workspace-btn--text record-text-btn"
              @click="cycleReviewStatus(item.key)"
            >
              {{ nextReviewActionText(item.key) }}
            </el-button>
          </div>
        </article>
      </div>
      <div v-else class="empty-review-hint">
        当前筛选结果里没有自动判定为重点复盘的记录，可以继续抽样检查表格明细。
      </div>
    </section>

    <section class="glass-panel review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">待跟进问题分布</div>
          <div class="review-desc">把待跟进记录归到补知识、补切分、补提示词、补展示、补观测等动作类型，帮助后续直接进入任务池。</div>
        </div>
        <div class="review-badges">
          <span class="review-badge">已归类：{{ categorizedFollowUpCount }}</span>
        </div>
      </div>
      <div class="category-export-row">
        <el-button class="workspace-btn workspace-btn--ghost" @click="exportFollowUpTaskDraft">导出后续任务草稿</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" @click="openTaskSuggestionDialog">查看任务建议清单</el-button>
        <span class="quick-filter-hint">按当前待跟进分类结果生成 Markdown，方便直接整理进任务池或复盘文档。</span>
      </div>
      <div class="quick-filter-row">
        <span class="quick-filter-label">分类聚焦</span>
        <el-button
          class="workspace-btn"
          :class="followUpCategoryView === '' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="followUpCategoryView = ''"
        >
          全部分类
        </el-button>
        <el-button
          v-for="option in followUpCategoryOptions"
          :key="option.value"
          class="workspace-btn"
          :class="followUpCategoryView === option.value ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="followUpCategoryView = option.value"
        >
          {{ option.label }}
        </el-button>
      </div>
      <div v-if="followUpCategoryStats.length" class="category-grid">
        <article v-for="item in followUpCategoryStats" :key="item.key" class="category-card">
          <div class="category-card-count">{{ item.count }}</div>
          <div class="category-card-title">{{ item.label }}</div>
          <div class="category-card-desc">{{ item.description }}</div>
        </article>
      </div>
      <div v-else class="empty-review-hint">
        当前还没有待跟进分类，先把某些明细标记为待跟进，再选择对应原因分类。
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
              <el-table-column label="验收整理" width="180">
                <template v-slot:default="scope">
                  <div class="cell-stack">
                    <el-button
                      text
                      class="workspace-btn workspace-btn--text record-text-btn"
                      @click="copyAcceptanceEntry(props.row, scope.row)"
                    >
                      复制验收条目
                    </el-button>
                    <el-button
                      text
                      class="workspace-btn workspace-btn--text record-text-btn"
                      @click="openAcceptanceDialog(props.row, scope.row)"
                    >
                      查看验收草稿
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="复盘状态" width="150">
                <template v-slot:default="scope">
                  <div class="cell-stack">
                    <el-tag
                      :type="reviewStatusTagType[getReviewStatus(buildReviewKey(props.row, scope.row))]"
                      effect="light"
                    >
                      {{ reviewStatusText[getReviewStatus(buildReviewKey(props.row, scope.row))] }}
                    </el-tag>
                    <el-button
                      text
                      class="workspace-btn workspace-btn--text record-text-btn"
                      @click="cycleReviewStatus(buildReviewKey(props.row, scope.row))"
                    >
                      {{ nextReviewActionText(buildReviewKey(props.row, scope.row)) }}
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="跟进分类" width="180">
                <template v-slot:default="scope">
                  <div class="cell-stack">
                    <el-select
                      v-if="getReviewStatus(buildReviewKey(props.row, scope.row)) === 'followUp'"
                      :model-value="getFollowUpCategory(buildReviewKey(props.row, scope.row))"
                      placeholder="选择分类"
                      size="small"
                      style="width: 150px"
                      @change="updateFollowUpCategory(buildReviewKey(props.row, scope.row), $event)"
                    >
                      <el-option
                        v-for="option in followUpCategoryOptions"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                      />
                    </el-select>
                    <span v-else class="follow-up-category-placeholder">仅待跟进可分类</span>
                    <span
                      v-if="getFollowUpCategory(buildReviewKey(props.row, scope.row))"
                      class="follow-up-category-text"
                    >
                      {{ getFollowUpCategoryLabel(buildReviewKey(props.row, scope.row)) }}
                    </span>
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

    <el-dialog v-model="acceptanceDialogVisible" class="record-detail-dialog" title="验收条目草稿" width="860px">
      <div class="detail-dialog-copy">
        <div class="detail-dialog-tip">这是一条可直接带去人工验收文档的草稿，结论项保持待人工填写，避免系统替你做判断。</div>
        <el-button class="workspace-btn workspace-btn--ghost" @click="copyText(acceptanceDialogContent, '验收条目草稿')">复制内容</el-button>
      </div>
      <pre class="detail-dialog-content">{{ acceptanceDialogContent }}</pre>
    </el-dialog>

    <el-dialog v-model="taskSuggestionDialogVisible" class="record-detail-dialog" title="后续任务建议清单" width="920px">
      <div class="detail-dialog-copy">
        <div class="detail-dialog-tip">这里按当前待跟进分类结果生成可直接复制的任务建议清单，适合快速贴到禅道或复盘消息里。</div>
        <el-button class="workspace-btn workspace-btn--ghost" @click="copyText(taskSuggestionContent, '后续任务建议清单')">复制内容</el-button>
      </div>
      <pre class="detail-dialog-content">{{ taskSuggestionContent }}</pre>
    </el-dialog>
  </div>
</template>
<script setup name='InvokeRecordManage' lang='ts'>
import { computed, reactive, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useTable } from '@/hooks/useTable';
import { pageInvokeRecordApi, queryInvokeRecordOverviewApi } from '@/api/workspace/invokeRecordApi';
import { getItem, saveItem } from '@/util/storageUtil';
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

const quickView = ref<'all' | 'fail' | 'slow' | 'long' | 'followUp' | 'reviewed'>('all')
const detailDialogVisible = ref(false)
const detailDialogTitle = ref('')
const detailDialogContent = ref('')
const acceptanceDialogVisible = ref(false)
const acceptanceDialogContent = ref('')
const taskSuggestionDialogVisible = ref(false)
const taskSuggestionContent = ref('')
const followUpCategoryView = ref<FollowUpCategory | ''>('')
const reviewStatusStoreKey = 'invoke-record-review-status'
const followUpCategoryStoreKey = 'invoke-record-follow-up-category'
const reviewStatusMap = ref<Record<string, ReviewStatus>>(loadReviewStatusMap())
const followUpCategoryMap = ref<Record<string, FollowUpCategory>>(loadFollowUpCategoryMap())
const reviewStatusText: Record<ReviewStatus, string> = {
  pending: '待复盘',
  reviewed: '已复盘',
  followUp: '待跟进'
}
const reviewStatusTagType: Record<ReviewStatus, 'info' | 'success' | 'warning'> = {
  pending: 'info',
  reviewed: 'success',
  followUp: 'warning'
}
const followUpCategoryOptions = [
  { value: 'knowledge', label: '补知识' },
  { value: 'chunking', label: '补切分' },
  { value: 'prompt', label: '补提示词' },
  { value: 'ui', label: '补展示' },
  { value: 'observe', label: '补观测' },
  { value: 'other', label: '其他' }
] as const
const followUpCategoryLabelMap: Record<FollowUpCategory, string> = {
  knowledge: '补知识',
  chunking: '补切分',
  prompt: '补提示词',
  ui: '补展示',
  observe: '补观测',
  other: '其他'
}
const followUpCategoryDescMap: Record<FollowUpCategory, string> = {
  knowledge: '知识源缺失、知识范围不完整、资料不够支撑回答',
  chunking: '切分粒度不合适、召回片段断裂、上下文拼接不理想',
  prompt: '提示词约束不够、回答风格不稳、失败反馈不够体面',
  ui: '前端展示不清楚、来源说明不够、验收动作不顺手',
  observe: '日志字段不够、缺少关键观测、排障信息不完整',
  other: '暂时无法归入以上分类的问题'
}

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
  if (quickView.value === 'followUp') {
    return rows.filter((row: any) =>
      (row.detailList || []).some((detail: any) => matchFollowUpFilter(row, detail))
    )
  }
  if (quickView.value === 'reviewed') {
    return rows.filter((row: any) =>
      (row.detailList || []).some((detail: any) => getReviewStatus(buildReviewKey(row, detail)) === 'reviewed')
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

const priorityReviewList = computed(() => {
  return filteredRows.value
    .flatMap((row: any) => {
      const detailList = row.detailList || []
      return detailList.map((detail: any, index: number) => {
        const riskTags = buildRiskTags(row, detail)
        return {
          key: `${row.id}-${index + 1}`,
          row,
          detail,
          riskTags,
          riskScore: riskTags.reduce((sum: number, tag: any) => sum + tag.score, 0)
        }
      })
    })
    .filter((item: any) => item.riskTags.length > 0)
    .sort((a: any, b: any) => {
      if (b.riskScore !== a.riskScore) {
        return b.riskScore - a.riskScore
      }
      return Number(b.detail.costTime || b.row.costTime || 0) - Number(a.detail.costTime || a.row.costTime || 0)
    })
    .slice(0, 8)
})

const followUpCount = computed(() => {
  return Object.values(reviewStatusMap.value).filter((status) => status === 'followUp').length
})

const categorizedFollowUpCount = computed(() => {
  return Object.entries(reviewStatusMap.value).filter(([key, status]) =>
    status === 'followUp' && Boolean(followUpCategoryMap.value[key])
  ).length
})

const followUpCategoryStats = computed(() => {
  return followUpCategoryOptions
    .map((option) => {
      const count = Object.entries(reviewStatusMap.value).filter(([key, status]) =>
        status === 'followUp' && followUpCategoryMap.value[key] === option.value
      ).length
      return {
        key: option.value,
        label: option.label,
        description: followUpCategoryDescMap[option.value],
        count
      }
    })
    .filter((item) => item.count > 0)
    .sort((a, b) => b.count - a.count)
})

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
  if (quickView.value === 'followUp') {
    return '当前仅显示已标记为待跟进的记录，适合集中处理需要继续补知识、补提示词或补链路观测的问题。'
  }
  if (quickView.value === 'reviewed') {
    return '当前仅显示已复盘记录，适合回看已经检查过的样本，确认结论是否需要再整理进验收文档。'
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

function buildAcceptanceEntry(row: any, detail: any, index: number) {
  return [
    `| TC-${String(row.id)}-${index + 1} | 待分类 | ${sanitizeTableCell(detail.userInput)} | 待补充 | ${buildAnswerSummary(detail.assistantMessage)} | 待人工判断 | 待人工判断 | 待人工判断 | ${Number(detail.status) === 0 ? '重点检查' : '待人工判断'} | 待人工判断 | 待人工判断 | 记录ID=${row.id}；应用=${sanitizeInline(row.appName)}；知识库=${sanitizeInline(row.libName)}；状态=${sanitizeInline(detail.statusDesc || row.statusDesc)}；耗时=${detail.costTime ?? row.costTime ?? '-'}ms |`,
    '',
    `补充信息：`,
    `- 原始失败原因：${sanitizeInline(detail.failReason || row.failReason || '无')}`,
    `- 模型：${sanitizeInline(detail.modelName || '未知')}`,
    `- Token：${detail.costToken ?? '-'}`,
    `- 开始时间：${sanitizeInline(detail.startTime || row.startTime || '-')}`,
    `- 结束时间：${sanitizeInline(detail.endTime || row.endTime || '-')}`,
    `- 原始回答全文：`,
    '```text',
    String(detail.assistantMessage || ''),
    '```'
  ].join('\n')
}

function exportAcceptanceDraft() {
  if (!filteredRows.value.length) {
    ElMessage.warning('当前没有可导出的验收草稿')
    return
  }
  const records = filteredRows.value.flatMap((row: any) => {
    const detailList = row.detailList || []
    if (!detailList.length) {
      return [
        `| TC-${String(row.id)}-1 | 待分类 | - | 待补充 | - | 待人工判断 | 待人工判断 | 待人工判断 | ${Number(row.status) === 0 ? '重点检查' : '待人工判断'} | 待人工判断 | 待人工判断 | 记录ID=${row.id}；应用=${sanitizeInline(row.appName)}；知识库=${sanitizeInline(row.libName)}；状态=${sanitizeInline(row.statusDesc)}；耗时=${row.costTime ?? '-'}ms |`
      ]
    }
    return detailList.map((detail: any, index: number) => buildAcceptanceEntry(row, detail, index))
  })

  const lines = [
    '# RAG MVP 测试问题集与效果记录',
    '',
    '## 1. 当前导出说明',
    '',
    `- 导出时间：${new Date().toLocaleString()}`,
    `- 当前聚焦：${currentQuickViewDesc.value}`,
    `- 当前记录数：${filteredRows.value.length}`,
    `- 导出目的：从调用记录页生成验收草稿，便于后续人工补齐“期望知识点 / 验收结论 / 汇总结论”`,
    '',
    '## 2. 当前版本信息',
    '',
    '- 测试日期：',
    '- 测试人：',
    '- 应用名称：',
    '- 场景类型：内部知识问答 / 任务指导',
    '- 知识库范围：',
    '- 发布版本：',
    '- 实验版本：',
    '- 版本说明：',
    '',
    '## 3. 调用记录转验收表',
    '',
    '| 编号 | 问题类型 | 用户问题 | 期望知识点 / 期望行为 | 实际回答摘要 | 命中问题 | 可信 | 易懂 | 失败体面 | HitRate@5 | Completeness | 备注 |',
    '| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |',
    ...records,
    '',
    '## 4. 汇总结论',
    '',
    `- 当前导出记录数：${filteredRows.value.length}`,
    `- 失败记录数：${filteredRows.value.filter((row: any) => Number(row.status) === 0).length}`,
    `- 慢请求数：${filteredRows.value.filter((row: any) => Number(row.costTime || 0) >= 5000).length}`,
    `- 长回答记录数：${filteredRows.value.filter((row: any) => (row.detailList || []).some((detail: any) => String(detail.assistantMessage || '').length >= 200)).length}`,
    '- 命中问题通过数：待人工填写',
    '- 可信通过数：待人工填写',
    '- 易懂通过数：待人工填写',
    '- 失败体面通过数：待人工填写',
    '',
    '## 5. 后续动作',
    '',
    '- 需要补知识的内容：',
    '- 需要优化切分的内容：',
    '- 需要优化检索/重排的内容：',
    '- 需要优化 Prompt 的内容：',
    '- 需要优化前端展示的内容：',
    '- 需要补日志或观测的内容：'
  ]

  downloadMarkdown(lines.join('\n'), `rag-mvp-acceptance-draft-${Date.now()}.md`)
  ElMessage.success('已导出验收草稿')
}

function exportFollowUpTaskDraft() {
  const taskSuggestion = buildFollowUpTaskSuggestion()
  if (!taskSuggestion) {
    ElMessage.warning('当前没有可导出的待跟进任务草稿')
    return
  }
  downloadMarkdown(taskSuggestion, `rag-follow-up-task-draft-${Date.now()}.md`)
  ElMessage.success('已导出后续任务草稿')
}

function openTaskSuggestionDialog() {
  const taskSuggestion = buildFollowUpTaskSuggestion()
  if (!taskSuggestion) {
    ElMessage.warning('当前没有可展示的后续任务建议清单')
    return
  }
  taskSuggestionContent.value = taskSuggestion
  taskSuggestionDialogVisible.value = true
}

function buildFollowUpTaskSuggestion() {
  const followUpEntries = getFollowUpEntries()
  if (!followUpEntries.length) {
    return ''
  }

  const grouped = followUpCategoryOptions
    .map((option) => ({
      ...option,
      entries: followUpEntries.filter((item) => item.category === option.value)
    }))
    .filter((group) => group.entries.length > 0)

  const lines = [
    '# RAG MVP 后续任务草稿',
    '',
    `- 导出时间：${new Date().toLocaleString()}`,
    `- 当前聚焦：${currentQuickViewDesc.value}`,
    `- 待跟进总数：${followUpEntries.length}`,
    '',
    '## 问题分类汇总',
    ''
  ]

  grouped.forEach((group) => {
    lines.push(`### ${group.label}（${group.entries.length}）`)
    lines.push(followUpCategoryDescMap[group.value])
    lines.push('')
    group.entries.forEach((item, index) => {
      lines.push(`#### ${group.label}-${index + 1}`)
      lines.push(`- 记录编号：${item.row.id}`)
      lines.push(`- 应用：${sanitizeInline(item.row.appName)}`)
      lines.push(`- 知识库：${sanitizeInline(item.row.libName)}`)
      lines.push(`- 模型：${sanitizeInline(item.detail.modelName || '未知模型')}`)
      lines.push(`- 耗时：${item.detail.costTime ?? item.row.costTime ?? '-'}ms`)
      lines.push(`- 用户问题：${sanitizeInline(item.detail.userInput)}`)
      lines.push(`- 回答摘要：${summarizeDisplayText(item.detail.assistantMessage)}`)
      lines.push(`- 失败原因：${sanitizeInline(item.detail.failReason || item.row.failReason || '无')}`)
      lines.push(`- 建议动作：围绕“${group.label}”方向进入任务池进一步处理`)
      lines.push('')
    })
  })

  lines.push('## 建议后续动作')
  lines.push('')
  lines.push('- 按分类评估是否需要新建任务，优先处理数量最多且影响体验最直接的问题。')
  lines.push('- 针对同类问题抽样核对原始调用记录，避免把单点异常误判成系统性问题。')
  lines.push('- 若同类问题连续出现，建议在任务中补充可复现样例问题与对应记录编号。')

  return lines.join('\n')
}

function getFollowUpEntries() {
  return filteredRows.value.flatMap((row: any) => {
    const detailList = row.detailList || []
    return detailList
      .map((detail: any) => {
        const key = buildReviewKey(row, detail)
        const category = getFollowUpCategory(key)
        if (getReviewStatus(key) !== 'followUp' || !category) {
          return null
        }
        return {
          key,
          row,
          detail,
          category
        }
      })
      .filter(Boolean)
  }) as Array<{ key: string; row: any; detail: any; category: FollowUpCategory }>
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

function openAcceptanceDialog(row: any, detail: any) {
  const detailList = row.detailList || []
  const index = Math.max(detailList.findIndex((item: any) => item === detail), 0)
  acceptanceDialogContent.value = buildAcceptanceEntry(row, detail, index)
  acceptanceDialogVisible.value = true
}

async function copyAcceptanceEntry(row: any, detail: any) {
  const detailList = row.detailList || []
  const index = Math.max(detailList.findIndex((item: any) => item === detail), 0)
  await copyText(buildAcceptanceEntry(row, detail, index), '验收条目')
}

function buildAnswerSummary(message: string) {
  const normalized = String(message || '').replace(/\s+/g, ' ').trim()
  if (!normalized) {
    return '-'
  }
  return sanitizeTableCell(normalized.slice(0, 120))
}

function summarizeDisplayText(message: string) {
  const normalized = String(message || '').replace(/\s+/g, ' ').trim()
  if (!normalized) {
    return '当前没有可用回答内容'
  }
  if (normalized.length <= 120) {
    return normalized
  }
  return `${normalized.slice(0, 120)}...`
}

function buildRiskTags(row: any, detail: any) {
  const tags = []
  const detailStatus = Number(detail.status ?? row.status)
  const costTime = Number(detail.costTime ?? row.costTime ?? 0)
  const answerText = String(detail.assistantMessage || '').trim()
  const failReason = String(detail.failReason || row.failReason || '').trim()

  if (detailStatus === 0) {
    tags.push({ key: 'fail', label: '失败记录', tone: 'danger', score: 100 })
  }
  if (failReason) {
    tags.push({ key: 'reason', label: '有失败原因', tone: 'warning', score: 50 })
  }
  if (costTime >= 5000) {
    tags.push({ key: 'slow', label: '慢请求', tone: 'warning', score: 35 })
  }
  if (answerText.length >= 200) {
    tags.push({ key: 'long', label: '长回答', tone: 'info', score: 20 })
  }
  if (!answerText) {
    tags.push({ key: 'empty', label: '空回答', tone: 'danger', score: 80 })
  }

  return tags
}

function buildReviewKey(row: any, detail: any) {
  const detailList = row.detailList || []
  const index = Math.max(detailList.findIndex((item: any) => item === detail), 0)
  return `${row.id}-${index + 1}`
}

function getReviewStatus(key: string): ReviewStatus {
  return reviewStatusMap.value[key] || 'pending'
}

function cycleReviewStatus(key: string) {
  const current = getReviewStatus(key)
  const next: ReviewStatus =
    current === 'pending'
      ? 'reviewed'
      : current === 'reviewed'
        ? 'followUp'
        : 'pending'
  reviewStatusMap.value = {
    ...reviewStatusMap.value,
    [key]: next
  }
  if (next !== 'followUp' && followUpCategoryMap.value[key]) {
    const nextCategoryMap = { ...followUpCategoryMap.value }
    delete nextCategoryMap[key]
    followUpCategoryMap.value = nextCategoryMap
    saveItem(followUpCategoryStoreKey, followUpCategoryMap.value)
  }
  saveItem(reviewStatusStoreKey, reviewStatusMap.value)
  ElMessage.success(`已更新为${reviewStatusText[next]}`)
}

function nextReviewActionText(key: string) {
  const current = getReviewStatus(key)
  if (current === 'pending') {
    return '标记已复盘'
  }
  if (current === 'reviewed') {
    return '标记待跟进'
  }
  return '清除状态'
}

function loadReviewStatusMap(): Record<string, ReviewStatus> {
  const raw = getItem(reviewStatusStoreKey)
  if (!raw) {
    return {}
  }
  try {
    return JSON.parse(raw)
  } catch {
    return {}
  }
}

function getFollowUpCategory(key: string): FollowUpCategory | '' {
  return followUpCategoryMap.value[key] || ''
}

function matchFollowUpFilter(row: any, detail: any) {
  const key = buildReviewKey(row, detail)
  if (getReviewStatus(key) !== 'followUp') {
    return false
  }
  if (!followUpCategoryView.value) {
    return true
  }
  return getFollowUpCategory(key) === followUpCategoryView.value
}

function getFollowUpCategoryLabel(key: string) {
  const category = getFollowUpCategory(key)
  if (!category) {
    return ''
  }
  return followUpCategoryLabelMap[category]
}

function updateFollowUpCategory(key: string, category: FollowUpCategory) {
  if (getReviewStatus(key) !== 'followUp') {
    ElMessage.warning('只有待跟进记录才能设置分类')
    return
  }
  followUpCategoryMap.value = {
    ...followUpCategoryMap.value,
    [key]: category
  }
  saveItem(followUpCategoryStoreKey, followUpCategoryMap.value)
  ElMessage.success(`已归类为${followUpCategoryLabelMap[category]}`)
}

function loadFollowUpCategoryMap(): Record<string, FollowUpCategory> {
  const raw = getItem(followUpCategoryStoreKey)
  if (!raw) {
    return {}
  }
  try {
    return JSON.parse(raw)
  } catch {
    return {}
  }
}

function sanitizeInline(value: any) {
  return String(value ?? '-').replace(/\n/g, ' ').trim() || '-'
}

function sanitizeTableCell(value: any) {
  return sanitizeInline(value).replace(/\|/g, '\\|')
}

function downloadMarkdown(content: string, fileName: string) {
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

type ReviewStatus = 'pending' | 'reviewed' | 'followUp'
type FollowUpCategory = 'knowledge' | 'chunking' | 'prompt' | 'ui' | 'observe' | 'other'

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

.priority-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 14px;
}

.priority-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 249, 255, 0.95));
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.priority-card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.priority-card-title {
  color: var(--space-text);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.5;
}

.priority-card-id,
.priority-card-meta,
.priority-label,
.empty-review-hint {
  color: var(--space-text-soft);
}

.priority-card-id {
  margin-left: 6px;
  font-size: 12px;
  font-weight: 600;
}

.priority-card-meta {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
}

.priority-tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.priority-tag {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.priority-tag--danger {
  background: rgba(254, 226, 226, 0.95);
  color: #b42318;
}

.priority-tag--warning {
  background: rgba(255, 244, 229, 0.98);
  color: #b54708;
}

.priority-tag--info {
  background: rgba(237, 245, 255, 0.95);
  color: var(--space-primary-strong);
}

.priority-block {
  margin-top: 12px;
}

.priority-label {
  font-size: 12px;
  font-weight: 700;
}

.priority-text {
  margin-top: 4px;
  color: var(--space-text);
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}

.priority-text--danger {
  color: #b42318;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.category-export-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.category-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  background: rgba(255, 255, 255, 0.96);
}

.category-card-count {
  color: var(--space-primary-strong);
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
}

.category-card-title {
  margin-top: 10px;
  color: var(--space-text);
  font-size: 14px;
  font-weight: 700;
}

.category-card-desc,
.follow-up-category-placeholder,
.follow-up-category-text {
  color: var(--space-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.category-card-desc {
  margin-top: 6px;
}

.follow-up-category-text {
  color: var(--space-primary-strong);
  font-weight: 600;
}

.priority-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.empty-review-hint {
  font-size: 13px;
  line-height: 1.7;
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

  .priority-card-head {
    flex-direction: column;
  }

  .priority-tag-group {
    justify-content: flex-start;
  }
}
</style>
