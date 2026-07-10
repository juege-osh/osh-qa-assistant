<template>
  <div class="page-shell admin-app-manage-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">平台巡检</span>
        <span class="workspace-context-note">统一查看应用归属、绑定状态和描述完整度，优先排查基础信息不完整的应用。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">结果 {{ currentResultCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">已绑定 {{ boundAppCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">待补描述 {{ missingDescCountDisplay }}</span>
      </div>
    </section>

    <section class="workspace-section-card admin-app-overview-panel workspace-dashboard-panel">
      <div class="admin-app-overview-head workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="panel-title">应用工作区</div>
          <div class="panel-desc workspace-panel-desc">先看当前结果里的绑定完成度、描述完整度和归属分布，再决定是继续排查命名、绑定，还是下钻到具体用户和知识库。</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--active">当前结果 {{ currentResultCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">已绑定 {{ boundAppCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">待补描述 {{ missingDescCountDisplay }}</span>
        </div>
      </div>

      <section class="stats-grid admin-app-metrics-grid workspace-metrics-grid">
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--total">
          <div class="stat-label">应用总数</div>
          <div class="stat-value">{{ totalAppCountDisplay }}</div>
          <div class="stat-help">平台当前纳管的全部应用数量。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
          <div class="stat-label">已绑定知识库</div>
          <div class="stat-value">{{ boundAppCountDisplay }}</div>
          <div class="stat-help">已经接入知识内容，适合继续看回答效果。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--token">
          <div class="stat-label">描述完整</div>
          <div class="stat-value">{{ describedAppCountDisplay }}</div>
          <div class="stat-help">描述清楚的应用更方便后续巡检与交接。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--time">
          <div class="stat-label">涉及用户</div>
          <div class="stat-value">{{ ownerCountDisplay }}</div>
          <div class="stat-help">快速判断当前结果覆盖了多少创建者。</div>
        </article>
      </section>
    </section>

    <section class="workspace-section-card admin-app-summary-panel">
      <div class="panel-title">巡检建议</div>
      <div class="summary-list">
        <div class="summary-item">{{ appInspectionSummary }}</div>
        <div class="summary-item">知识库已经绑定但描述仍然空缺的应用，后续维护时更容易出现识别成本偏高的问题，建议优先补齐。</div>
        <div class="summary-item">如果同一用户名下应用较多，建议继续结合知识库页和调用记录页一起看，判断是否存在重复建设或配置分散。</div>
      </div>
    </section>

    <section class="workspace-section-card admin-app-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="panel-title panel-title--md">当前关注点</div>
          <div class="workspace-panel-desc">把当前结果翻译成更直接的巡检动作，方便决定先核查绑定、描述还是归属分布。</div>
        </div>
      </div>
      <div class="workspace-tip-grid admin-app-tip-grid">
        <article
          v-for="item in appFocusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'admin-app-tip-card', `admin-app-tip-card--${item.tone}`]"
        >
          <div class="admin-app-tip-card__head">
            <span :class="['admin-app-tip-card__dot', `admin-app-tip-card__dot--${item.tone}`]"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
        </article>
      </div>
    </section>

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">平台应用</div>
        <div class="toolbar-title">应用列表</div>
        <div class="toolbar-desc">
          先按名称缩小范围，再回看归属、知识库绑定和描述完整性。
        </div>
      </div>
      <div class="toolbar-actions workspace-toolbar-actions">
        <el-form :model="searchData" :inline="true" class="workspace-toolbar-form">
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--md">
            <el-input type="text" placeholder="按应用名称搜索" v-model="searchData.appName" clearable></el-input>
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
        <el-table-column label="应用" min-width="280">
          <template #default="{ row }">
            <div class="workspace-entity-cell">
              <div class="workspace-entity-media workspace-entity-media--avatar admin-app-media">
                <el-avatar :src="convertIconPath(row.iconPath)" />
              </div>
              <div class="workspace-entity-copy">
                <div class="workspace-entity-name">{{ row.appName || '未命名应用' }}</div>
                <div class="workspace-entity-meta">
                  <span>ID {{ row.id }}</span>
                  <span>所属用户：{{ row.username || '--' }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="绑定知识库" min-width="200">
          <template #default="{ row }">
            <div class="workspace-table-note">
              {{ row.libName || '暂未绑定知识库' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="应用描述" min-width="260">
          <template #default="{ row }">
            <div class="workspace-table-note workspace-table-note--muted admin-app-desc">
              {{ row.appDesc || '暂无描述' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">
            <div class="workspace-table-note">{{ row.createdTime || '--' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="最近更新" min-width="160">
          <template #default="{ row }">
            <div class="workspace-table-note">{{ row.modifiedTime || '--' }}</div>
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
<script setup name='AppManage' lang='ts'>
import { computed, reactive, onMounted } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageAppApi } from '@/api/admin/appApi';
import { getImage } from '@/util/AssetsImageUtil';
import { useResource } from '@/hooks/useResource';

let searchFormData = reactive({
  appName: ''
})

let {
  searchData,
  tableData,
  loadTable,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageAppApi })
const { toAddressable } = useResource()
const currentRows = computed(() => tableData.rows || [])
const totalAppCountDisplay = computed(() => tableData.total || 0)
const currentResultCountDisplay = computed(() => currentRows.value.length)
const boundAppCountDisplay = computed(() => currentRows.value.filter((row: { libName?: string | null }) => Boolean(String(row.libName || '').trim())).length)
const describedAppCountDisplay = computed(() => currentRows.value.filter((row: { appDesc?: string | null }) => Boolean(String(row.appDesc || '').trim())).length)
const missingDescCountDisplay = computed(() => currentRows.value.filter((row: { appDesc?: string | null }) => !String(row.appDesc || '').trim()).length)
const ownerCountDisplay = computed(() => {
  return new Set(
    currentRows.value
      .map((row: { username?: string | null }) => String(row.username || '').trim())
      .filter(Boolean)
  ).size
})
const appInspectionSummary = computed(() => {
  if (!currentRows.value.length) {
    return '当前筛选结果为空，可以先调整名称条件，或者直接查看全量应用分布。'
  }
  if (!boundAppCountDisplay.value) {
    return '当前结果里的应用都还没有绑定知识库，更适合先排查应用定位是否清楚，再决定哪些需要继续接入知识内容。'
  }
  if (missingDescCountDisplay.value > 0) {
    return `当前结果中有 ${missingDescCountDisplay.value} 个应用缺少描述，建议优先补齐定位说明，方便后续巡检、交接和公开展示。`
  }
  return '当前结果里的应用基础信息相对完整，下一步更适合下钻到调用记录或知识库页，继续验证实际回答链路。'
})

const appFocusCards = computed(() => [
  {
    title: boundAppCountDisplay.value === 0 ? '优先核查未绑定知识库的应用' : '绑定关系可以继续下钻验证',
    desc: boundAppCountDisplay.value === 0
      ? '当前结果里还没有绑定知识库的应用，更适合先确认应用定位与知识内容接入计划。'
      : `当前已有 ${boundAppCountDisplay.value} 个应用绑定知识库，适合继续去知识库页或调用记录页看真实效果。`,
    tone: boundAppCountDisplay.value === 0 ? 'warning' : 'success'
  },
  {
    title: missingDescCountDisplay.value > 0 ? '描述缺失的应用适合优先补齐' : '应用说明整体较完整',
    desc: missingDescCountDisplay.value > 0
      ? `当前仍有 ${missingDescCountDisplay.value} 个应用缺少描述，后续巡检、交接和公开展示时识别成本会更高。`
      : '应用名称和描述信息相对清楚，后续做归属核查和排查会更顺畅。',
    tone: missingDescCountDisplay.value > 0 ? 'warning' : 'success'
  },
  {
    title: ownerCountDisplay.value > 1 ? '可以继续看多用户分布是否合理' : '当前结果归属相对集中',
    desc: ownerCountDisplay.value > 1
      ? `当前结果涉及 ${ownerCountDisplay.value} 个用户，适合继续判断是否存在重复建设、绑定分散或命名冲突。`
      : '当前结果里的应用归属相对集中，更适合先看应用自身配置和知识绑定情况。',
    tone: ownerCountDisplay.value > 1 ? 'warning' : 'success'
  }
] as const)

function convertIconPath(iconPath: string) {
  if (iconPath) {
    return toAddressable(iconPath)
  }
  return getImage('default.png')
}

onMounted(() => {
  loadTable()
})
</script>
<style scoped>
.admin-app-manage-page {
  gap: 16px;
}

.admin-app-summary-panel,
.admin-app-focus-panel {
  padding: 18px 20px;
}

.admin-app-tip-grid {
  margin-top: 14px;
}

.admin-app-tip-card {
  position: relative;
  overflow: hidden;
}

.admin-app-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.admin-app-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.admin-app-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.admin-app-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-app-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.78);
  flex-shrink: 0;
}

.admin-app-tip-card__dot--success {
  background: #12b76a;
}

.admin-app-tip-card__dot--warning {
  background: #f59e0b;
}

.admin-app-overview-panel .workspace-panel-desc {
  max-width: 760px;
  font-size: 14px;
  line-height: 1.75;
}

.admin-app-media :deep(.el-avatar) {
  width: 100%;
  height: 100%;
  border-radius: 12px;
}

.admin-app-desc {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}

@media (max-width: 900px) {
  .admin-app-tip-grid {
    grid-template-columns: 1fr;
  }
}

</style>
