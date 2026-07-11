<template>
  <div class="page-shell admin-user-manage-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">平台巡检</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">结果 {{ currentResultCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">已分配 {{ assignedKeyCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">缺少头像 {{ missingAvatarCountDisplay }}</span>
      </div>
    </section>

    <section class="workspace-section-card admin-user-overview-panel workspace-dashboard-panel">
      <div class="workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="panel-title">用户工作区</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--active">当前结果 {{ currentResultCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">已分配 {{ assignedKeyCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">未分配 {{ missingKeyCountDisplay }}</span>
        </div>
      </div>

      <section class="stats-grid admin-user-metrics-grid workspace-metrics-grid">
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--total">
          <div class="stat-label">用户总数</div>
          <div class="stat-value">{{ totalUserCountDisplay }}</div>
          <div class="stat-help">平台当前已注册的全部用户数量。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--time">
          <div class="stat-label">当前结果</div>
          <div class="stat-value">{{ currentResultCountDisplay }}</div>
          <div class="stat-help">当前筛选条件下可直接核查的账号规模。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
          <div class="stat-label">已分配 appKey</div>
          <div class="stat-value workspace-stat-value--success">{{ assignedKeyCountDisplay }}</div>
          <div class="stat-help">已经具备对接凭证，可继续排查接入与授权。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--token">
          <div class="stat-label">头像已配置</div>
          <div class="stat-value">{{ avatarConfiguredCountDisplay }}</div>
          <div class="stat-help">头像越完整，后台巡检和协作识别越轻松。</div>
        </article>
      </section>
    </section>

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">账号巡检</div>
        <div class="toolbar-title">用户列表</div>
      </div>
      <div class="toolbar-actions workspace-toolbar-actions">
        <el-form :model="searchData" :inline="true" class="workspace-toolbar-form">
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--md">
            <el-input type="text" placeholder="用户名" v-model="searchData.username" clearable></el-input>
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
        <el-table-column label="用户" min-width="280">
          <template #default="{ row }">
            <div class="workspace-entity-cell">
              <div class="workspace-entity-media workspace-entity-media--avatar user-avatar-media">
                <el-avatar :src="convertAvatarPath(row.avatarPath)">
                  {{ (row.username || '?').slice(0, 1).toUpperCase() }}
                </el-avatar>
              </div>
              <div class="workspace-entity-copy">
                <div class="workspace-entity-name">{{ row.username || '未命名用户' }}</div>
                <div class="workspace-entity-meta">
                  <span>注册：{{ row.registerTime || '--' }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="应用凭证" min-width="260">
          <template #default="{ row }">
            <span class="workspace-table-code">{{ row.appKey || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="账户状态" min-width="180">
          <template #default="{ row }">
            <div class="workspace-inline-tags">
              <span class="workspace-inline-tag workspace-inline-tag--soft">已注册</span>
              <span class="workspace-inline-tag" :class="row.appKey ? 'workspace-inline-tag--active' : 'workspace-inline-tag--warning'">
                {{ row.appKey ? '已分配 appKey' : '未分配 appKey' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="170">
          <template #default="{ row }">
            <div class="workspace-table-note">{{ row.registerTime || '--' }}</div>
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
<script setup name='UserManage' lang='ts'>
import { computed, onMounted, reactive } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageUserApi } from '@/api/admin/userApi';
import { useResource } from '@/hooks/useResource';
import { getImage } from '@/util/AssetsImageUtil';
let searchFormData = reactive({
  username: ''
})
let {
  searchData,
  tableData,
  loadTable,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageUserApi })
const { toAddressable } = useResource()

function formatCount(value: number | string) {
  const num = Number(value || 0)
  if (!Number.isFinite(num)) {
    return '--'
  }
  return num.toLocaleString('zh-CN')
}

const totalUserCountDisplay = computed(() => formatCount(tableData.total))
const currentResultCountDisplay = computed(() => formatCount(tableData.rows.length))
const assignedKeyCount = computed(() => tableData.rows.filter((row: { appKey?: string }) => String(row.appKey || '').trim()).length)
const missingKeyCount = computed(() => tableData.rows.filter((row: { appKey?: string }) => !String(row.appKey || '').trim()).length)
const avatarConfiguredCount = computed(() => tableData.rows.filter((row: { avatarPath?: string }) => String(row.avatarPath || '').trim()).length)
const missingAvatarCount = computed(() => tableData.rows.filter((row: { avatarPath?: string }) => !String(row.avatarPath || '').trim()).length)
const assignedKeyCountDisplay = computed(() => formatCount(assignedKeyCount.value))
const missingKeyCountDisplay = computed(() => formatCount(missingKeyCount.value))
const avatarConfiguredCountDisplay = computed(() => formatCount(avatarConfiguredCount.value))
const missingAvatarCountDisplay = computed(() => formatCount(missingAvatarCount.value))
function convertAvatarPath(avatarPath: string) {
  if (avatarPath) {
    return toAddressable(avatarPath)
  }
  return getImage('default.png')
}

onMounted(() => {
  loadTable()
})
</script>
<style scoped>
.admin-user-manage-page {
  gap: 16px;
}

.admin-user-summary-panel,
.admin-user-focus-panel {
  padding: 18px 20px;
}

.admin-user-tip-grid {
  margin-top: 14px;
}

.admin-user-tip-card {
  position: relative;
  overflow: hidden;
}

.admin-user-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.admin-user-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.admin-user-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.admin-user-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-user-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex-shrink: 0;
  background: rgba(148, 163, 184, 0.9);
}

.admin-user-tip-card__dot--success {
  background: #12b76a;
  box-shadow: 0 0 0 4px rgba(18, 183, 106, 0.12);
}

.admin-user-tip-card__dot--warning {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.14);
}
</style>
