<template>
  <div class="page-shell admin-knowledge-manage-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">平台巡检</span>
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

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">平台知识库</div>
        <div class="toolbar-title">知识库列表</div>
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
