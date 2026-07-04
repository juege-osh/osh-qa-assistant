<template>
    <div class="page-shell manager-manage-page">
        <section class="workspace-context-strip">
            <div class="workspace-context-copy">
                <span class="workspace-status-pill workspace-status-pill--active">平台巡检</span>
                <span class="workspace-context-note">统一查看管理员席位、登录记录和资料完整度，优先核查长期未使用的后台权限。</span>
            </div>
            <div class="workspace-context-actions">
                <span class="workspace-inline-tag workspace-inline-tag--soft">结果 {{ currentResultCountDisplay }}</span>
                <span class="workspace-inline-tag workspace-inline-tag--soft">有登录 {{ loggedInCountDisplay }}</span>
                <span class="workspace-inline-tag workspace-inline-tag--soft">缺少头像 {{ missingAvatarCountDisplay }}</span>
            </div>
        </section>

        <section class="workspace-section-card manager-overview-panel workspace-dashboard-panel">
            <div class="workspace-overview-head workspace-dashboard-head">
                <div>
                    <div class="panel-title">席位工作区</div>
                    <div class="workspace-panel-desc">先看当前管理员数量、登录记录和头像配置，再决定是继续做权限核查还是清理遗留席位。</div>
                </div>
                <div class="workspace-inline-tags">
                    <span class="workspace-inline-tag workspace-inline-tag--active">当前结果 {{ currentResultCountDisplay }}</span>
                    <span class="workspace-inline-tag workspace-inline-tag--soft">有登录 {{ loggedInCountDisplay }}</span>
                    <span class="workspace-inline-tag workspace-inline-tag--soft">未登录 {{ noLoginCountDisplay }}</span>
                </div>
            </div>

            <section class="stats-grid manager-overview-metrics-grid workspace-metrics-grid">
                <article class="stat-card workspace-stat-card--framed workspace-stat-card--total">
                    <div class="stat-label">管理员总数</div>
                    <div class="stat-value">{{ totalManagerCountDisplay }}</div>
                    <div class="stat-help">当前平台已经开通的全部后台席位数量。</div>
                </article>
                <article class="stat-card workspace-stat-card--framed workspace-stat-card--time">
                    <div class="stat-label">当前结果</div>
                    <div class="stat-value">{{ currentResultCountDisplay }}</div>
                    <div class="stat-help">当前筛选条件下可直接巡检的管理员规模。</div>
                </article>
                <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
                    <div class="stat-label">有登录记录</div>
                    <div class="stat-value workspace-stat-value--success">{{ loggedInCountDisplay }}</div>
                    <div class="stat-help">说明这些席位至少已经实际进入过管理台。</div>
                </article>
                <article class="stat-card workspace-stat-card--framed workspace-stat-card--token">
                    <div class="stat-label">头像已配置</div>
                    <div class="stat-value">{{ avatarConfiguredCountDisplay }}</div>
                    <div class="stat-help">有助于排班、巡检和权限核查时快速识别人员。</div>
                </article>
            </section>
        </section>

        <section class="workspace-section-card summary-panel manager-summary-panel">
            <div class="panel-title">值班建议</div>
            <div class="summary-list">
                <div class="summary-item">{{ managerInspectionSummary }}</div>
                <div class="summary-item">长期没有登录记录的管理员席位，建议确认是否仍有保留必要，避免后台权限闲置。</div>
                <div class="summary-item">新增管理员后，建议同步确认头像和真实姓名，避免值班或问题排查时出现识别混乱。</div>
            </div>
        </section>

        <section class="workspace-section-card manager-focus-panel">
            <div class="workspace-overview-head">
                <div>
                    <div class="panel-title panel-title--md">当前关注点</div>
                    <div class="workspace-panel-desc">把席位状态转换成更直接的巡检动作，方便判断先看登录情况、资料完整度还是权限保留必要性。</div>
                </div>
            </div>
            <div class="workspace-tip-grid manager-tip-grid">
                <article
                    v-for="item in managerFocusCards"
                    :key="item.title"
                    :class="['workspace-tip-card', 'manager-tip-card', `manager-tip-card--${item.tone}`]"
                >
                    <div class="manager-tip-card__head">
                        <span :class="['manager-tip-card__dot', `manager-tip-card__dot--${item.tone}`]"></span>
                        <div class="workspace-tip-card__title">{{ item.title }}</div>
                    </div>
                    <div class="workspace-tip-card__desc">{{ item.desc }}</div>
                </article>
            </div>
        </section>

        <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
            <div class="toolbar-copy workspace-toolbar-copy">
                <div class="workspace-toolbar-kicker">席位巡检</div>
                <div class="toolbar-title">管理员列表</div>
                <div class="toolbar-desc">
                    先按用户名、姓名和性别缩小范围，再回看登录情况和席位保留必要性。
                </div>
            </div>
            <div class="toolbar-actions workspace-toolbar-actions">
                <el-form :model="searchData" :inline="true" class="workspace-toolbar-form">
                    <el-form-item class="workspace-toolbar-field workspace-toolbar-field--sm">
                        <el-input type="text" placeholder="用户名" v-model="searchData.username" clearable></el-input>
                    </el-form-item>
                    <el-form-item class="workspace-toolbar-field workspace-toolbar-field--sm">
                        <el-input type="text" placeholder="姓名" v-model="searchData.realName" clearable></el-input>
                    </el-form-item>
                    <el-form-item label="性别" class="workspace-toolbar-field workspace-toolbar-field--sm">
                        <el-select v-model="searchData.sex">
                            <el-option v-for="one in formCfg.sexList" :key="one.sex" :label="one.sexName"
                                :value="one.sex"></el-option>
                        </el-select>
                    </el-form-item>
                    <el-form-item>
                        <el-button @click="loadTable" type="primary" class="workspace-btn workspace-btn--primary">查询</el-button>
                    </el-form-item>
                    <el-form-item>
                        <el-button @click="addDialogVisible = true" type="primary" :icon="Plus" class="workspace-btn workspace-btn--primary">
                            新增管理员
                        </el-button>
                    </el-form-item>
                </el-form>
            </div>
        </section>

        <!-- 表格   -->
        <section class="workspace-section-card table-panel">
            <el-table :data="tableData.rows" stripe style="width: 100%">
                <el-table-column label="管理员" min-width="320">
                    <template #default="{ row }">
                        <div class="workspace-entity-cell">
                            <div class="workspace-entity-media workspace-entity-media--avatar manager-avatar-media">
                                <el-avatar :src="convertAvatarPath(row.avatarPath)">
                                    {{ (row.realName || row.username || '?').slice(0, 1).toUpperCase() }}
                                </el-avatar>
                            </div>
                            <div class="workspace-entity-copy">
                                <div class="workspace-entity-name">{{ row.realName || row.username || '未命名管理员' }}</div>
                                <div class="workspace-entity-meta">
                                    <span>用户名：{{ row.username || '--' }}</span>
                                    <span>ID {{ row.id || '--' }}</span>
                                </div>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="身份信息" min-width="220">
                    <template #default="{ row }">
                        <div class="workspace-inline-tags">
                            <span class="workspace-inline-tag workspace-inline-tag--soft">{{ row.sexDesc || '未设置性别' }}</span>
                            <span class="workspace-inline-tag workspace-inline-tag--active">管理员席位</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="最近登录" min-width="180">
                    <template #default="{ row }">
                        <div class="workspace-table-note">{{ row.lastLoginTime || '暂无登录记录' }}</div>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="140">
                    <template v-slot:default="scope">
                        <el-button class="workspace-btn workspace-btn--ghost workspace-btn--danger workspace-btn--sm" :icon="Delete" @click="deleteById(scope.row.id)">删除
                        </el-button>
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
        <!-- 新增对话框 -->
        <AddManager :addDialogVisible="addDialogVisible" @closeDialog="addDialogVisible = false"
            @addSuccess="handleAddSuccess">
        </AddManager>
    </div>
</template>
<script setup name='ManagerManage' lang='ts'>
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue';
import AddManager from '@/views/admin/manager/AddManager.vue';
import { useTable } from '@/hooks/useTable';
import { pageManagerApi, deleteManagerByIdApi } from '@/api/admin/managerApi';
import { useResource } from '@/hooks/useResource';
import { getImage } from '@/util/AssetsImageUtil';

// 添加对话框是否显示
let addDialogVisible = ref(false)
let searchFormData = reactive({
    username: '',
    realName: '',
    sex: '',
})

let formCfg = reactive({
    sexList: [
        { sex: '', sexName: "全部" },
        { sex: '1', sexName: "男" },
        { sex: '0', sexName: "女" },
    ]
})

let {
    searchData,
    tableData,
    loadTable,
    deleteById,
    handlePageSizeChange,
    handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageManagerApi, deleteByIdApi: deleteManagerByIdApi })
const { toAddressable } = useResource()

function formatCount(value: number | string) {
    const num = Number(value || 0)
    if (!Number.isFinite(num)) {
        return '--'
    }
    return num.toLocaleString('zh-CN')
}

const totalManagerCountDisplay = computed(() => formatCount(tableData.total))
const currentResultCountDisplay = computed(() => formatCount(tableData.rows.length))
const loggedInCount = computed(() => tableData.rows.filter((row: { lastLoginTime?: string }) => String(row.lastLoginTime || '').trim()).length)
const noLoginCount = computed(() => tableData.rows.filter((row: { lastLoginTime?: string }) => !String(row.lastLoginTime || '').trim()).length)
const avatarConfiguredCount = computed(() => tableData.rows.filter((row: { avatarPath?: string }) => String(row.avatarPath || '').trim()).length)
const missingAvatarCount = computed(() => tableData.rows.filter((row: { avatarPath?: string }) => !String(row.avatarPath || '').trim()).length)
const loggedInCountDisplay = computed(() => formatCount(loggedInCount.value))
const noLoginCountDisplay = computed(() => formatCount(noLoginCount.value))
const avatarConfiguredCountDisplay = computed(() => formatCount(avatarConfiguredCount.value))
const missingAvatarCountDisplay = computed(() => formatCount(missingAvatarCount.value))
const managerInspectionSummary = computed(() => {
    if (!tableData.rows.length) {
        return '当前筛选条件下还没有管理员记录，可以调整查询条件，或者直接新增新的后台席位。'
    }
    if (!tableData.rows.some((row: { lastLoginTime?: string }) => String(row.lastLoginTime || '').trim())) {
        return '当前结果中的管理员都还没有登录记录，建议确认这些席位是否已经分发给对应人员并完成首次登录。'
    }
    if (tableData.rows.some((row: { avatarPath?: string }) => !String(row.avatarPath || '').trim())) {
        return '当前结果中仍有缺少头像的管理员，虽然不影响权限，但会增加后台值班和巡检时的辨识成本。'
    }
    return '当前结果中的管理员信息整体较完整，适合继续结合最近登录时间和数量变化做权限巡检。'
})

const managerFocusCards = computed(() => [
    {
        title: noLoginCount.value > 0 ? '优先核查长期未登录席位' : '当前结果里的席位都有登录记录',
        desc: noLoginCount.value > 0
            ? `当前仍有 ${noLoginCountDisplay.value} 个席位没有登录记录，建议确认这些管理员是否仍在实际使用。`
            : '说明这些管理员席位至少都已经真实进入过后台，可以继续看权限范围和操作轨迹。',
        tone: noLoginCount.value > 0 ? 'warning' : 'success'
    },
    {
        title: missingAvatarCount.value > 0 ? '补齐头像有助于值班辨识' : '席位资料整体较完整',
        desc: missingAvatarCount.value > 0
            ? `当前有 ${missingAvatarCountDisplay.value} 个管理员缺少头像，后续排班、巡检和协作时辨识成本会更高。`
            : '姓名、头像和最近登录时间较完整，后续做后台人员核查会更顺畅。',
        tone: missingAvatarCount.value > 0 ? 'warning' : 'success'
    },
    {
        title: tableData.rows.length ? '建议定期清理闲置后台账号' : '等待更多结果后继续判断',
        desc: tableData.rows.length
            ? '如果管理员数量持续增长但登录记录不活跃，建议结合职责分工确认是否有遗留权限需要回收。'
            : '当前结果为空，建议先放宽筛选条件，拿到更多席位样本后再继续判断。',
        tone: tableData.rows.length ? 'danger' : 'success'
    }
] as const)

function convertAvatarPath(avatarPath: string) {
    if (avatarPath) {
        return toAddressable(avatarPath)
    }
    return getImage('default.png')
}

function handleAddSuccess() {
    addDialogVisible.value = false
    loadTable()
}

onMounted(() => { loadTable() })
</script>
<style scoped>
.manager-manage-page {
    gap: 16px;
}

.manager-summary-panel,
.manager-focus-panel {
    padding: 18px 20px;
}

.manager-tip-grid {
    margin-top: 14px;
}

.manager-tip-card {
    position: relative;
    overflow: hidden;
}

.manager-tip-card::before {
    content: "";
    position: absolute;
    inset: 0 auto 0 0;
    width: 4px;
    border-radius: 4px 0 0 4px;
    background: rgba(148, 163, 184, 0.42);
}

.manager-tip-card--success::before {
    background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.manager-tip-card--warning::before {
    background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.manager-tip-card--danger::before {
    background: linear-gradient(180deg, #ef4444 0%, #dc2626 100%);
}

.manager-tip-card__head {
    display: flex;
    align-items: center;
    gap: 8px;
}

.manager-tip-card__dot {
    width: 8px;
    height: 8px;
    border-radius: 999px;
    flex-shrink: 0;
    background: rgba(148, 163, 184, 0.9);
}

.manager-tip-card__dot--success {
    background: #12b76a;
    box-shadow: 0 0 0 4px rgba(18, 183, 106, 0.12);
}

.manager-tip-card__dot--warning {
    background: #f59e0b;
    box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.14);
}

.manager-tip-card__dot--danger {
    background: #ef4444;
    box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.12);
}
</style>
