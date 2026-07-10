<template>
  <div class="page-shell admin-uploadfile-manage-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">平台巡检</span>
        <span class="workspace-context-note">统一查看各用户与应用上传的文件，优先关注高字符量、低命中的资料。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">结果 {{ totalFileCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">启用 {{ enabledFileCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">召回 {{ currentPageRecallDisplay }}</span>
      </div>
    </section>

    <section class="workspace-section-card file-overview-panel workspace-dashboard-panel">
      <div class="file-overview-head workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="panel-title">平台文件工作区</div>
          <div class="panel-desc workspace-panel-desc">先看当前巡检范围内的启用状态、召回热度和整体规模，再决定要继续排查具体用户、应用还是知识库。</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--active">当前结果 {{ totalFileCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">启用 {{ enabledFileCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">停用 {{ disabledFileCountDisplay }}</span>
        </div>
      </div>

      <section class="stats-grid file-overview-metrics-grid workspace-metrics-grid">
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--total">
          <div class="stat-label">文件总数</div>
          <div class="stat-value">{{ totalFileCountDisplay }}</div>
          <div class="stat-help">当前筛选结果里的全部文件数量。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
          <div class="stat-label">启用中文件</div>
          <div class="stat-value workspace-stat-value--success">{{ enabledFileCountDisplay }}</div>
          <div class="stat-help">可以直接参与检索与召回的文件规模。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--token">
          <div class="stat-label">累计召回次数</div>
          <div class="stat-value workspace-stat-value--warning">{{ currentPageRecallDisplay }}</div>
          <div class="stat-help">用于观察当前结果中这些文件的整体命中热度。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--time">
          <div class="stat-label">累计字符量</div>
          <div class="stat-value">{{ currentPageCharCountDisplay }}</div>
          <div class="stat-help">帮助判断文件规模是否过大，是否需要回看切分与组织结构。</div>
        </article>
      </section>
    </section>

    <section class="workspace-section-card admin-upload-summary-panel">
      <div class="panel-title">巡检建议</div>
      <div class="summary-list">
        <div class="summary-item">{{ workflowSummary }}</div>
        <div class="summary-item">高字符数但低召回的文档，通常适合优先复查标题命名、切分规则和所属知识库结构。</div>
        <div class="summary-item">如果启用文件很多但召回热度仍然偏低，建议继续下钻到知识库和调用记录页确认命中链路是否正常。</div>
      </div>
    </section>

    <section class="workspace-section-card admin-upload-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="panel-title panel-title--md">当前关注点</div>
          <div class="workspace-panel-desc">把当前结果翻译成更直接的巡检动作，方便决定先看启用状态、字符规模还是召回热度。</div>
        </div>
      </div>
      <div class="workspace-tip-grid admin-upload-tip-grid">
        <article
          v-for="item in uploadFocusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'admin-upload-tip-card', `admin-upload-tip-card--${item.tone}`]"
        >
          <div class="admin-upload-tip-card__head">
            <span :class="['admin-upload-tip-card__dot', `admin-upload-tip-card__dot--${item.tone}`]"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
        </article>
      </div>
    </section>

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">文件巡检</div>
        <div class="toolbar-title">文件列表</div>
        <div class="toolbar-desc">
          先按用户、应用和知识库缩小范围，再回看高字符量、低召回的文件。
        </div>
      </div>
      <div class="toolbar-actions workspace-toolbar-actions">
        <el-form :model="searchData" :inline="true" class="workspace-toolbar-form">
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--sm">
            <el-input type="text" placeholder="所属用户" v-model="searchData.username" clearable></el-input>
          </el-form-item>
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--sm">
            <el-input type="text" placeholder="所属应用" v-model="searchData.appName" clearable></el-input>
          </el-form-item>
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--sm">
            <el-input type="text" placeholder="知识库名称" v-model="searchData.libName" clearable></el-input>
          </el-form-item>
          <el-form-item>
            <el-button @click="loadTable" type="primary" class="workspace-btn workspace-btn--primary">查询</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <!-- 表格   -->
    <section class="workspace-section-card table-panel">
      <el-table :data="tableData.rows" stripe style="width: 100%">
        <el-table-column label="文件" min-width="320">
          <template #default="{ row }">
            <div class="workspace-entity-copy admin-uploadfile-main">
              <div class="workspace-entity-name">{{ row.fileName || '未命名文件' }}</div>
              <div class="workspace-entity-meta">
                <span>所属用户：{{ row.username || '--' }}</span>
                <span>所属应用：{{ row.appName || '--' }}</span>
                <span>知识库：{{ row.libName || '--' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数据规模" min-width="220">
          <template #default="{ row }">
            <div class="workspace-inline-tags">
              <span class="workspace-inline-tag workspace-inline-tag--soft">{{ formatCount(row.charCount) }} 字</span>
              <span class="workspace-inline-tag workspace-inline-tag--soft">召回 {{ formatCount(row.recallCount) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="130">
          <template #default="{ row }">
            <span :class="['workspace-inline-tag', getStatusTagClass(row.status)]">{{ row.statusDesc || '未知状态' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            <div class="workspace-table-note">{{ row.createdTime || '--' }}</div>
          </template>
        </el-table-column>
      </el-table>
    </section>
    <!-- 分页   -->
    <section class="mt-dot5 workspace-section-card pagination-panel">
      <el-pagination @size-change="handlePageSizeChange" @current-change="handlePageNowChange"
        :current-page="searchData.pageNow" :page-sizes="[10, 30, 50]" :page-size="searchData.pageSize"
        layout="total, sizes, prev, pager, next, jumper" :total="tableData.total">
      </el-pagination>
    </section>
  </div>
</template>
<script setup name='UploadFileManage' lang='ts'>
import { reactive, onMounted, computed } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageUploadFileApi } from '@/api/admin/uploadFileApi';

let searchFormData = reactive({
  username: '',
  appName: '',
  libName: '',
})

let {
  searchData,
  tableData,
  loadTable,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageUploadFileApi })

const totalFileCountDisplay = computed(() => formatCount(tableData.total))
const enabledFileCountDisplay = computed(() => {
  return formatCount(tableData.rows.filter((row: { status?: number }) => Number(row.status) === 1).length)
})
const disabledFileCountDisplay = computed(() => {
  return formatCount(tableData.rows.filter((row: { status?: number }) => Number(row.status) !== 1).length)
})
const currentPageRecallDisplay = computed(() => {
  const total = tableData.rows.reduce((sum: number, row: { recallCount?: number | string }) => sum + Number(row.recallCount || 0), 0)
  return formatCount(total)
})
const currentPageCharCountDisplay = computed(() => {
  const total = tableData.rows.reduce((sum: number, row: { charCount?: number | string }) => sum + Number(row.charCount || 0), 0)
  return formatCount(total)
})
const workflowSummary = computed(() => {
  if (!tableData.rows.length) {
    return '当前筛选范围内还没有文件记录，可以调整筛选条件，或者回到平台其他模块继续查看知识库与应用情况。'
  }
  if (!tableData.rows.some((row: { status?: number }) => Number(row.status) === 1)) {
    return '当前页还没有启用中的文件，这一批资料暂时无法稳定参与检索，建议先确认是否存在误停用或归档过早的情况。'
  }
  return '当前平台已经有启用中的文件资源，适合继续关注召回热度偏低的文档，并排查归属应用和知识库结构是否合理。'
})

const uploadFocusCards = computed(() => [
  {
    title: enabledFileCountDisplay.value === '0' ? '优先确认文件是否误停用' : '启用中的文件适合继续看命中热度',
    desc: enabledFileCountDisplay.value === '0'
      ? '当前结果里还没有启用中的文件，这批资料暂时无法稳定参与检索，建议先确认状态是否正确。'
      : `当前已有 ${enabledFileCountDisplay.value} 个启用中的文件，适合继续判断是否真正参与了知识命中。`,
    tone: enabledFileCountDisplay.value === '0' ? 'warning' : 'success'
  },
  {
    title: currentPageRecallDisplay.value === '0' ? '低召回文件值得优先复查' : '可以继续结合召回热度做巡检',
    desc: currentPageRecallDisplay.value === '0'
      ? '当前结果里的文件召回热度很低，适合优先看标题命名、切分规则和知识库归属。'
      : `当前结果累计召回 ${currentPageRecallDisplay.value} 次，可以继续判断哪些文件高频命中、哪些长期沉默。`,
    tone: currentPageRecallDisplay.value === '0' ? 'warning' : 'success'
  },
  {
    title: tableData.rows.length ? '高字符量文件适合回看切分策略' : '等待更多筛选结果后再判断',
    desc: tableData.rows.length
      ? `当前结果累计字符量 ${currentPageCharCountDisplay.value}，如果命中效果不理想，建议优先回看内容结构与 chunk 切分。`
      : '当前结果为空，建议先调整筛选条件，拿到更多文件样本后再继续巡检。',
    tone: tableData.rows.length ? 'warning' : 'success'
  }
] as const)

function getStatusTagClass(status: number) {
  return Number(status) === 1 ? 'workspace-inline-tag--success' : 'workspace-inline-tag--warning'
}

function formatCount(value: number | string) {
  const num = Number(value || 0)
  if (!Number.isFinite(num)) {
    return '--'
  }
  return num.toLocaleString('zh-CN')
}

onMounted(() => {
  loadTable()
})
</script>
<style scoped>
.admin-uploadfile-manage-page {
  gap: 16px;
}

.admin-upload-summary-panel,
.admin-upload-focus-panel {
  padding: 18px 20px;
}

.admin-upload-tip-grid {
  margin-top: 14px;
}

.admin-upload-tip-card {
  position: relative;
  overflow: hidden;
}

.admin-upload-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.admin-upload-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.admin-upload-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.admin-upload-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-upload-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex-shrink: 0;
  background: rgba(148, 163, 184, 0.9);
}

.admin-upload-tip-card__dot--success {
  background: #12b76a;
  box-shadow: 0 0 0 4px rgba(18, 183, 106, 0.12);
}

.admin-upload-tip-card__dot--warning {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.14);
}

.admin-uploadfile-main {
  gap: 6px;
}
</style>
