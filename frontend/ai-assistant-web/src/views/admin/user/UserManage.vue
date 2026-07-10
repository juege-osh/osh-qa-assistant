<template>
  <div class="page-shell admin-user-manage-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">平台巡检</span>
        <span class="workspace-context-note">统一查看账号凭证、头像和注册信息，优先核查接入身份是否完整。</span>
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
          <div class="workspace-panel-desc">先看账号凭证和头像配置是否完整，再决定要继续排查接入身份、账号归属还是发放状态。</div>
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

    <section class="workspace-section-card summary-panel admin-user-summary-panel">
      <div class="panel-title">巡检建议</div>
      <div class="summary-list">
        <div class="summary-item">{{ userInspectionSummary }}</div>
        <div class="summary-item">如果应用侧接入异常，先确认 appKey 是否已经分配，再结合调用记录排查失败原因。</div>
        <div class="summary-item">头像和用户名都不清晰的账号，后续做权限核查和问题定位时辨识成本会更高，适合优先补齐。</div>
      </div>
    </section>

    <section class="workspace-section-card admin-user-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="panel-title panel-title--md">当前关注点</div>
          <div class="workspace-panel-desc">把当前筛选结果翻译成更直接的巡检动作，方便决定先核对凭证、资料还是接入状态。</div>
        </div>
      </div>
      <div class="workspace-tip-grid admin-user-tip-grid">
        <article
          v-for="item in userFocusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'admin-user-tip-card', `admin-user-tip-card--${item.tone}`]"
        >
          <div class="admin-user-tip-card__head">
            <span :class="['admin-user-tip-card__dot', `admin-user-tip-card__dot--${item.tone}`]"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
        </article>
      </div>
    </section>

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">账号巡检</div>
        <div class="toolbar-title">用户列表</div>
        <div class="toolbar-desc">
          先按用户名缩小范围，再核对头像、注册时间和 appKey 是否一致。
        </div>
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
const userInspectionSummary = computed(() => {
  if (!tableData.rows.length) {
    return '当前筛选条件下还没有用户记录，可以调整用户名条件，或者回到其他平台页继续巡检。'
  }
  if (!tableData.rows.some((row: { appKey?: string }) => String(row.appKey || '').trim())) {
    return '当前结果里还没有分配 appKey 的账号，这批用户暂时无法直接完成应用接入，建议优先确认发放流程是否遗漏。'
  }
  if (tableData.rows.some((row: { avatarPath?: string }) => !String(row.avatarPath || '').trim())) {
    return '当前结果中仍有缺少头像的账号，虽然不影响接入，但会增加后台巡检和账号识别成本。'
  }
  return '当前结果里的用户凭证和头像配置整体较完整，适合继续结合调用记录和应用页核查实际接入效果。'
})

const userFocusCards = computed(() => [
  {
    title: missingKeyCount.value > 0 ? '先核查未分配 appKey 的账号' : '当前结果里的凭证整体完整',
    desc: missingKeyCount.value > 0
      ? `当前仍有 ${missingKeyCountDisplay.value} 个账号未分配 appKey，这批用户暂时无法直接完成应用接入。`
      : '可以把更多精力放在真实接入效果和调用异常排查上，而不是先补发凭证。',
    tone: missingKeyCount.value > 0 ? 'warning' : 'success'
  },
  {
    title: missingAvatarCount.value > 0 ? '补齐头像能降低巡检辨识成本' : '账号资料辨识度较好',
    desc: missingAvatarCount.value > 0
      ? `当前有 ${missingAvatarCountDisplay.value} 个账号缺少头像，适合优先补齐，方便后续协作和值班识别。`
      : '用户名和头像信息相对完整，后续做权限核查和问题定位会更顺手。',
    tone: missingAvatarCount.value > 0 ? 'warning' : 'success'
  },
  {
    title: tableData.rows.length ? '应用接入异常先回看调用记录' : '等待更多筛选结果后再判断',
    desc: tableData.rows.length
      ? '如果应用侧反馈异常，建议先确认账号身份和 appKey，再到调用记录里看失败原因与请求内容。'
      : '当前结果为空，建议先放宽筛选条件，拿到更多样本后再继续判断问题分布。',
    tone: tableData.rows.length ? 'warning' : 'success'
  }
] as const)

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
