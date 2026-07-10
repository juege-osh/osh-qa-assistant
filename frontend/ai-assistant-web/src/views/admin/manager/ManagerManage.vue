<template>
  <div class="page-shell manager-manage-page">
    <section class="manager-page-header">
      <div class="manager-page-title-group">
        <h1 class="manager-page-title">管理员账号</h1>
        <span class="manager-page-count">共 {{ totalManagerCountDisplay }} 人</span>
      </div>
      <div class="workspace-inline-tags manager-page-status">
        <span class="workspace-inline-tag workspace-inline-tag--success">有登录 {{ loggedInCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">未登录 {{ noLoginCountDisplay }}</span>
      </div>
    </section>

    <section class="manager-list-toolbar">
      <el-form :model="searchData" :inline="true" class="manager-filter-form">
        <el-form-item>
          <el-input v-model="searchData.username" clearable placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="searchData.realName" clearable placeholder="姓名" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="searchData.sex">
            <el-option v-for="item in formCfg.sexList" :key="item.sex" :label="item.sexName" :value="item.sex" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="loadTable">查询</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" class="workspace-btn workspace-btn--primary" @click="addDialogVisible = true">
        新增管理员账号
      </el-button>
    </section>

    <section class="workspace-section-card table-panel manager-table-panel">
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
                <div class="manager-mobile-details">
                  <span>{{ row.sexDesc || '未设置性别' }}</span>
                  <span>{{ row.lastLoginTime || '暂无登录记录' }}</span>
                </div>
              </div>
              <el-button
                v-if="!isCurrentManager(row.id)"
                :icon="Delete"
                class="workspace-btn workspace-btn--ghost workspace-btn--danger workspace-btn--sm manager-mobile-delete"
                @click="deleteById(row.id)"
              >
                删除
              </el-button>
              <span v-else class="workspace-inline-tag workspace-inline-tag--active manager-current-account">当前账号</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="身份信息"
          min-width="220"
          class-name="manager-table-column--secondary"
          label-class-name="manager-table-column--secondary"
        >
          <template #default="{ row }">
            <div class="workspace-inline-tags">
              <span class="workspace-inline-tag workspace-inline-tag--soft">{{ row.sexDesc || '未设置性别' }}</span>
              <span class="workspace-inline-tag workspace-inline-tag--active">管理员账号</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="最近登录"
          min-width="180"
          class-name="manager-table-column--secondary"
          label-class-name="manager-table-column--secondary"
        >
          <template #default="{ row }">
            <div class="workspace-table-note">{{ row.lastLoginTime || '暂无登录记录' }}</div>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="140"
          class-name="manager-table-column--action"
          label-class-name="manager-table-column--action"
        >
          <template #default="scope">
            <span v-if="isCurrentManager(scope.row.id)" class="workspace-inline-tag workspace-inline-tag--active">当前账号</span>
            <el-button
              v-else
              :icon="Delete"
              class="workspace-btn workspace-btn--ghost workspace-btn--danger workspace-btn--sm"
              @click="deleteById(scope.row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="workspace-section-card pagination-panel">
      <el-pagination
        :current-page="searchData.pageNow"
        :page-size="searchData.pageSize"
        :page-sizes="[10, 30, 50]"
        :total="tableData.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageSizeChange"
        @current-change="handlePageNowChange"
      />
    </section>

    <AddManager
      :addDialogVisible="addDialogVisible"
      @closeDialog="addDialogVisible = false"
      @addSuccess="handleAddSuccess"
    />
  </div>
</template>

<script setup name="ManagerManage" lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'
import AddManager from '@/views/admin/manager/AddManager.vue'
import { useTable } from '@/hooks/useTable'
import { deleteManagerByIdApi, pageManagerApi } from '@/api/admin/managerApi'
import { useResource } from '@/hooks/useResource'
import { getImage } from '@/util/AssetsImageUtil'
import { useUserStore } from '@/store/useUserStore'

const addDialogVisible = ref(false)
const userStore = useUserStore()
const searchFormData = reactive({
  username: '',
  realName: '',
  sex: '',
})
const formCfg = {
  sexList: [
    { sex: '', sexName: '全部' },
    { sex: '1', sexName: '男' },
    { sex: '0', sexName: '女' },
  ]
}

const {
  searchData,
  tableData,
  loadTable,
  deleteById,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageManagerApi, deleteByIdApi: deleteManagerByIdApi })

const { toAddressable } = useResource()

function formatCount(value: number | string) {
  const count = Number(value || 0)
  return Number.isFinite(count) ? count.toLocaleString('zh-CN') : '--'
}

const totalManagerCountDisplay = computed(() => formatCount(tableData.total))
const loggedInCountDisplay = computed(() => {
  return formatCount(tableData.rows.filter((row: { lastLoginTime?: string }) => Boolean(String(row.lastLoginTime || '').trim())).length)
})
const noLoginCountDisplay = computed(() => {
  return formatCount(tableData.rows.filter((row: { lastLoginTime?: string }) => !String(row.lastLoginTime || '').trim()).length)
})

function isCurrentManager(id: string | number) {
  return String(id) === String(userStore.userInfo.id)
}

function convertAvatarPath(avatarPath: string) {
  return avatarPath ? toAddressable(avatarPath) : getImage('default.png')
}

function handleAddSuccess() {
  addDialogVisible.value = false
  loadTable()
}

onMounted(loadTable)
</script>

<style scoped>
.manager-manage-page {
  gap: 20px;
}

.manager-page-header,
.manager-list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.manager-page-header {
  min-height: 44px;
  padding: 2px 0 14px;
  border-bottom: 1px solid var(--space-border);
}

.manager-page-title-group,
.manager-page-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.manager-page-title {
  margin: 0;
  color: var(--space-text);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
}

.manager-page-count {
  color: var(--space-text-soft);
  font-size: 14px;
}

.manager-list-toolbar {
  padding-bottom: 18px;
  border-bottom: 1px solid var(--space-border);
}

.manager-filter-form {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 10px;
}

.manager-filter-form :deep(.el-form-item) {
  margin: 0;
}

.manager-filter-form :deep(.el-input),
.manager-filter-form :deep(.el-select) {
  width: 160px;
}

.manager-table-panel {
  padding: 0;
  overflow: hidden;
}

.manager-mobile-details,
.manager-mobile-delete,
.manager-current-account {
  display: none;
}

@media (max-width: 900px) {
  .manager-page-header,
  .manager-list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .manager-page-status {
    justify-content: space-between;
  }

  .manager-filter-form :deep(.el-form-item),
  .manager-filter-form :deep(.el-input),
  .manager-filter-form :deep(.el-select) {
    width: 100%;
  }

  .manager-list-toolbar > .workspace-btn {
    width: 100%;
  }

  .manager-table-panel :deep(.manager-table-column--secondary),
  .manager-table-panel :deep(.manager-table-column--action) {
    display: none;
  }

  .manager-table-panel :deep(.el-table__cell:first-child) {
    width: 100%;
  }

  .manager-table-panel :deep(.workspace-entity-cell) {
    align-items: center;
    gap: 10px;
  }

  .manager-table-panel :deep(.workspace-entity-copy) {
    min-width: 0;
  }

  .manager-mobile-details {
    display: flex;
    flex-direction: column;
    gap: 3px;
    margin-top: 6px;
    color: var(--space-text-soft);
    font-size: 12px;
    line-height: 1.45;
  }

  .manager-mobile-delete {
    display: inline-flex;
    flex-shrink: 0;
    margin-left: auto;
  }

  .manager-current-account {
    display: inline-flex;
    flex-shrink: 0;
    margin-left: auto;
  }
}
</style>
