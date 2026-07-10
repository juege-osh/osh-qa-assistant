<template>
  <div class="page-shell admin-knowledge-manage-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">平台巡检</span>
        <span class="workspace-context-note">统一查看知识库归属、描述质量和图标配置，优先补齐基础资料不完整的内容资产。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">结果 {{ currentResultCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">描述完整 {{ describedLibCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">缺少图标 {{ missingIconCountDisplay }}</span>
      </div>
    </section>

    <section class="workspace-section-card admin-knowledge-overview-panel workspace-dashboard-panel">
      <div class="admin-knowledge-overview-head workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="panel-title">知识库工作区</div>
          <div class="panel-desc workspace-panel-desc">先看描述完整度、图标配置和涉及用户分布，再决定是继续补资料说明，还是下钻到文档和应用绑定链路。</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--active">当前结果 {{ currentResultCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">描述完整 {{ describedLibCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">缺少图标 {{ missingIconCountDisplay }}</span>
        </div>
      </div>

      <section class="stats-grid admin-knowledge-metrics-grid workspace-metrics-grid">
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--total">
          <div class="stat-label">知识库总数</div>
          <div class="stat-value">{{ totalLibCountDisplay }}</div>
          <div class="stat-help">平台当前纳管的全部知识库数量。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
          <div class="stat-label">描述完整</div>
          <div class="stat-value">{{ describedLibCountDisplay }}</div>
          <div class="stat-help">便于后续判断范围、归属和适用场景。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--token">
          <div class="stat-label">已配图标</div>
          <div class="stat-value">{{ iconConfiguredCountDisplay }}</div>
          <div class="stat-help">在绑定和列表场景里更容易快速识别。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--time">
          <div class="stat-label">涉及用户</div>
          <div class="stat-value">{{ ownerCountDisplay }}</div>
          <div class="stat-help">帮助判断当前结果覆盖的资产归属面。</div>
        </article>
      </section>
    </section>

    <section class="workspace-section-card summary-panel admin-knowledge-summary-panel">
      <div class="panel-title">巡检建议</div>
      <div class="summary-list">
        <div class="summary-item">{{ knowledgeInspectionSummary }}</div>
        <div class="summary-item">缺少图标的知识库虽然不影响召回，但在绑定应用、列表识别和日常巡检时更容易增加辨识成本。</div>
        <div class="summary-item">如果描述已经完整但回答仍然不稳定，建议继续下钻到文档管理和检索调试，确认问题是不是出在内容结构或切分策略上。</div>
      </div>
    </section>

    <section class="workspace-section-card admin-knowledge-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="panel-title panel-title--md">当前关注点</div>
          <div class="workspace-panel-desc">把当前结果翻译成更直接的巡检动作，方便决定先补描述、补图标，还是继续下钻内容结构与挂载关系。</div>
        </div>
      </div>
      <div class="workspace-tip-grid admin-knowledge-tip-grid">
        <article
          v-for="item in knowledgeFocusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'admin-knowledge-tip-card', `admin-knowledge-tip-card--${item.tone}`]"
        >
          <div class="admin-knowledge-tip-card__head">
            <span :class="['admin-knowledge-tip-card__dot', `admin-knowledge-tip-card__dot--${item.tone}`]"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
        </article>
      </div>
    </section>

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">平台知识库</div>
        <div class="toolbar-title">知识库列表</div>
        <div class="toolbar-desc">
          先按用户和名称缩小范围，再回看描述、归属和图标配置是否完整。
        </div>
      </div>
      <div class="toolbar-actions workspace-toolbar-actions">
        <el-form :model="searchData" :inline="true" class="workspace-toolbar-form">
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--sm">
            <el-input type="text" placeholder="所属用户" v-model="searchData.username" clearable></el-input>
          </el-form-item>
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--md">
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
        <el-table-column label="知识库" min-width="300">
          <template #default="{ row }">
            <div class="workspace-entity-cell">
              <div class="workspace-entity-media knowledge-lib-media">
                <el-avatar :src="convertIconPath(row.iconPath)" />
              </div>
              <div class="workspace-entity-copy">
                <div class="workspace-entity-name">{{ row.libName || '未命名知识库' }}</div>
                <div class="workspace-entity-meta">
                  <span>所属用户：{{ row.username || '--' }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="280">
          <template #default="{ row }">
            <div class="workspace-table-note workspace-table-note--muted knowledge-lib-desc">
              {{ row.libDesc || '暂无描述' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="维护状态" min-width="180">
          <template #default="{ row }">
            <div class="workspace-inline-tags">
              <span class="workspace-inline-tag workspace-inline-tag--soft">{{ row.iconPath ? '已配置图标' : '缺少图标' }}</span>
              <span class="workspace-inline-tag" :class="row.libDesc ? 'workspace-inline-tag--success' : 'workspace-inline-tag--warning'">
                {{ row.libDesc ? '描述完整' : '待补描述' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            <div class="workspace-table-note">{{ row.createdTime || '--' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="最近更新" min-width="170">
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
<script setup name='KnowledgeLibManage' lang='ts'>
import { computed, reactive, onMounted } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageKnowledgeLibApi } from '@/api/admin/knowledgeLibApi';
import { getImage } from '@/util/AssetsImageUtil';
import { useResource } from '@/hooks/useResource';

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
} = useTable({ searchFormData, loadTableApi: pageKnowledgeLibApi })
const { toAddressable } = useResource()
const currentRows = computed(() => tableData.rows || [])
const totalLibCountDisplay = computed(() => tableData.total || 0)
const currentResultCountDisplay = computed(() => currentRows.value.length)
const describedLibCountDisplay = computed(() => currentRows.value.filter((row: { libDesc?: string | null }) => Boolean(String(row.libDesc || '').trim())).length)
const iconConfiguredCountDisplay = computed(() => currentRows.value.filter((row: { iconPath?: string | null }) => Boolean(String(row.iconPath || '').trim())).length)
const missingIconCountDisplay = computed(() => currentRows.value.filter((row: { iconPath?: string | null }) => !String(row.iconPath || '').trim()).length)
const ownerCountDisplay = computed(() => {
  return new Set(
    currentRows.value
      .map((row: { username?: string | null }) => String(row.username || '').trim())
      .filter(Boolean)
  ).size
})
const knowledgeInspectionSummary = computed(() => {
  if (!currentRows.value.length) {
    return '当前筛选结果为空，可以先调整用户或名称条件，再继续查看知识库分布。'
  }
  if (!describedLibCountDisplay.value) {
    return '当前结果里的知识库都还没有补充描述，建议先补齐覆盖范围和使用边界，再继续做绑定或检索巡检。'
  }
  if (missingIconCountDisplay.value > 0) {
    return `当前结果中有 ${missingIconCountDisplay.value} 个知识库缺少图标，建议统一补齐，方便在绑定应用和日常管理时快速识别。`
  }
  return '当前结果里的知识库基础信息相对完整，下一步更适合继续检查文档结构、应用挂载和实际召回效果。'
})
const knowledgeFocusCards = computed(() => [
  {
    title: describedLibCountDisplay.value === 0 ? '优先补齐知识库描述信息' : '描述完整度可以支撑后续巡检',
    desc: describedLibCountDisplay.value === 0
      ? '当前结果里的知识库都还没有补充描述，建议先明确覆盖范围、适用场景和内容边界。'
      : `当前已有 ${describedLibCountDisplay.value} 个知识库描述相对完整，后续定位和交接成本会更低。`,
    tone: describedLibCountDisplay.value === 0 ? 'warning' : 'success'
  },
  {
    title: missingIconCountDisplay.value > 0 ? '缺少图标的知识库适合优先补齐' : '图标配置整体较完整',
    desc: missingIconCountDisplay.value > 0
      ? `当前仍有 ${missingIconCountDisplay.value} 个知识库缺少图标，在绑定应用和列表识别场景里更容易增加辨识成本。`
      : '当前结果里的知识库图标配置相对完整，列表识别和资产管理会更顺畅。',
    tone: missingIconCountDisplay.value > 0 ? 'warning' : 'success'
  },
  {
    title: ownerCountDisplay.value > 1 ? '可以继续观察多用户资产分布' : '当前结果归属相对集中',
    desc: ownerCountDisplay.value > 1
      ? `当前结果覆盖 ${ownerCountDisplay.value} 个用户，适合继续确认是否存在重复建设、无人维护或归属不清的知识资产。`
      : '当前筛选结果主要集中在少量用户范围内，更适合继续下钻内容结构和挂载状态。',
    tone: ownerCountDisplay.value > 1 ? 'success' : 'warning'
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
.admin-knowledge-manage-page {
  gap: 16px;
}

.admin-knowledge-summary-panel,
.admin-knowledge-focus-panel {
  padding: 18px 20px;
}

.admin-knowledge-overview-panel .workspace-panel-desc {
  max-width: 760px;
  font-size: 14px;
  line-height: 1.75;
}

.admin-knowledge-tip-grid {
  margin-top: 14px;
}

.admin-knowledge-tip-card {
  position: relative;
  overflow: hidden;
}

.admin-knowledge-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.admin-knowledge-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.admin-knowledge-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.admin-knowledge-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-knowledge-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex-shrink: 0;
  background: rgba(148, 163, 184, 0.9);
}

.admin-knowledge-tip-card__dot--success {
  background: #12b76a;
  box-shadow: 0 0 0 4px rgba(18, 183, 106, 0.12);
}

.admin-knowledge-tip-card__dot--warning {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.14);
}

.knowledge-lib-media :deep(.el-avatar) {
  width: 100%;
  height: 100%;
  border-radius: 12px;
}

.knowledge-lib-desc {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}

</style>
