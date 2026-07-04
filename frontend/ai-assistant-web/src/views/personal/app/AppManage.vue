<template>
  <div class="page-shell app-manage-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">我的应用</span>
        <span class="workspace-context-note">统一查看应用绑定、模型配置和公开访问准备情况，优先判断下一步是补配置还是直接验证对话。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">结果 {{ currentResultCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">已绑定 {{ boundAppCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">待绑定 {{ unboundAppCountDisplay }}</span>
      </div>
    </section>

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">我的应用</div>
        <div class="toolbar-title">应用列表</div>
        <div class="toolbar-desc">
          先按名称缩小范围，再回看知识库绑定、模型配置和公开访问准备情况。
        </div>
      </div>
      <div class="toolbar-actions workspace-toolbar-actions">
        <el-form :model="searchData" :inline="true" class="workspace-toolbar-form">
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--lg">
            <el-input type="text" placeholder="按应用名称搜索" v-model="searchData.appName" clearable></el-input>
          </el-form-item>
          <el-form-item>
            <el-button @click="loadTable" type="primary" class="workspace-btn workspace-btn--primary">查询</el-button>
          </el-form-item>
          <el-form-item>
            <el-button @click="addDialogVisible = true" type="primary" :icon="Plus" class="workspace-btn workspace-btn--primary">新增应用</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <section class="workspace-section-card card-list-panel">
      <el-row :gutter="10">
        <el-col v-for="row in tableData.rows" class="card-wrapper" :key="row.id" :lg="8">
          <el-card class="workspace-resource-card" shadow="never">
            <div class="workspace-resource-card__body">
              <div class="workspace-entity-cell">
                <div class="workspace-entity-media workspace-entity-media--avatar">
                  <el-avatar :src="convertIconPath(row.iconPath)"></el-avatar>
                </div>
                <div class="workspace-entity-copy">
                  <div class="workspace-entity-name">{{ row.appName }}</div>
                  <div class="workspace-entity-meta">
                    <span>最后编辑于 {{ row.modifiedTime || '--' }}</span>
                  </div>
                </div>
              </div>
              <div class="workspace-inline-tags">
                <span class="workspace-inline-tag workspace-inline-tag--soft">ID {{ row.id }}</span>
                <span class="workspace-inline-tag workspace-inline-tag--soft">{{ row.chatModel || '系统默认模型' }}</span>
              </div>
              <div class="workspace-resource-card__section">
                <div class="workspace-resource-card__row workspace-resource-card__row--tight">
                  <span class="workspace-resource-card__label">知识库</span>
                  <el-tooltip
                    :content="row.libName || '暂未绑定知识库'"
                    placement="top"
                    :disabled="!(row.libName && String(row.libName).length > 18)"
                  >
                    <span class="workspace-resource-card__value workspace-resource-card__value--truncate">
                      {{ row.libName || '暂未绑定知识库' }}
                    </span>
                  </el-tooltip>
                  <div class="workspace-resource-card__actions-inline workspace-resource-card__actions-inline--compact">
                    <el-link
                      v-if="row.libId"
                      class="bind-unbind-link"
                      @click="openRecallDebug(row.libId)"
                      type="primary"
                    >
                      检索调试
                    </el-link>
                    <el-link v-if="row.libName" class="bind-unbind-link" @click="unbindLib(row.id)" type="primary">解绑</el-link>
                    <el-link v-else class="bind-unbind-link" @click="openBindLibDialog(row.id)" type="primary">绑定</el-link>
                  </div>
                </div>
                <div class="workspace-resource-card__row">
                  <span class="workspace-resource-card__label">公开访问</span>
                  <span
                    :class="[
                      'workspace-resource-card__value',
                      getPublishStatusClass(row.id)
                    ]"
                  >
                    {{ getPublishStatusText(row.id) }}
                  </span>
                </div>
              </div>
              <div class="workspace-resource-card__desc">
                {{ row.appDesc || '暂无描述，建议补充应用定位、适用问题和使用边界。' }}
              </div>
            </div>
            <template #footer>
              <div class="workspace-resource-card__footer">
                <el-button @click="openUpdateDialog(row.id)" class="workspace-btn workspace-btn--ghost">编辑</el-button>
                <el-button @click="openPublishDialog(row.id)" class="workspace-btn workspace-btn--ghost">公开发布</el-button>
                <el-button @click="startChat(row.id)" type="primary" class="workspace-btn workspace-btn--primary">开始聊天</el-button>
                <el-button @click="deleteById(row.id)" class="workspace-btn workspace-btn--ghost workspace-btn--danger">删除</el-button>
              </div>
            </template>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <section class="mt-dot5 workspace-section-card pagination-panel">
      <el-pagination @size-change="handlePageSizeChange" @current-change="handlePageNowChange"
        :current-page="searchData.pageNow" :page-sizes="[10, 30, 50]" :page-size="searchData.pageSize"
        layout="total, sizes, prev, pager, next, jumper" :total="tableData.total">
      </el-pagination>
    </section>
    <!-- 新增对话框 -->
    <AddApp :addDialogVisible="addDialogVisible" @closeDialog="addDialogVisible = false" @addSuccess="handleAddSuccess">
    </AddApp>
    <!-- 更新对话框 -->
    <UpdateApp :updateDialogVisible="updateDialogVisible" @closeDialog="updateDialogVisible = false"
      :idToUpdate="idToUpdate" @updateSuccess="handleUpdateSuccess"></UpdateApp>
    <BindLib :bind-lib-dialog-visible="bindLibDialogVisible" @closeDialog="bindLibDialogVisible = false"
      :idToBindLib="idToBindLib" @bind-success="handleBindSuccess"></BindLib>
    <PublishApp
      :publish-dialog-visible="publishDialogVisible"
      @closeDialog="publishDialogVisible = false"
      :idToPublish="idToPublish"
      @publishSuccess="handlePublishSuccess"
    ></PublishApp>
  </div>
</template>
<script setup name='AppManage' lang='ts'>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageAppApi, deleteAppByIdApi, checkChatConditionApi, queryPublishConfigApi, unBindLibApi } from '@/api/workspace/appApi';
import { getImage } from '@/util/AssetsImageUtil';
import { Plus } from '@element-plus/icons-vue';
import AddApp from '@/views/personal/app/AddApp.vue';
import UpdateApp from '@/views/personal/app/UpdateApp.vue';
import BindLib from '@/views/personal/app/BindLib.vue';
import PublishApp from '@/views/personal/app/PublishApp.vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useResource } from '@/hooks/useResource';

// 添加对话框是否显示
let addDialogVisible = ref(false)
let updateDialogVisible = ref(false)
let bindLibDialogVisible = ref(false)
let publishDialogVisible = ref(false)
let idToUpdate = ref('')
let idToBindLib = ref('')
let idToPublish = ref('')

let searchFormData = reactive({
  appName: ''
})

let {
  searchData,
  tableData,
  loadTable,
  deleteById,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageAppApi, deleteByIdApi: deleteAppByIdApi })

let router = useRouter()
let {toAddressable} = useResource()
const publishStatusMap = ref<Record<string, { enabled: number; accessType: string }>>({})
const currentRows = computed(() => tableData.rows || [])
const totalAppCountDisplay = computed(() => tableData.total || 0)
const currentResultCountDisplay = computed(() => currentRows.value.length)
const boundAppCountDisplay = computed(() => currentRows.value.filter((row: { libId?: string | number | null }) => Boolean(row.libId)).length)
const unboundAppCountDisplay = computed(() => currentRows.value.filter((row: { libId?: string | number | null }) => !row.libId).length)
// 应用图片处理
function convertIconPath(iconPath: string) {
  if (iconPath) {
    return toAddressable(iconPath)
  } else {
    return getImage('default.png')
  }
}
// 打开更新对话框
function openUpdateDialog(id: string) {
  idToUpdate.value = id
  updateDialogVisible.value = true
}
// 打开绑定对话框
function openBindLibDialog(id: string) {
  idToBindLib.value = id
  bindLibDialogVisible.value = true
}
function openPublishDialog(id: string) {
  idToPublish.value = id
  publishDialogVisible.value = true
}

function openRecallDebug(libId?: string | number | null) {
  const targetLibId = String(libId || '').trim()
  if (!targetLibId) {
    ElMessage.warning('请先绑定知识库后再进行检索调试')
    return
  }
  router.push({
    path: '/workspace/knowledgeLib/manage',
    query: {
      debugLibId: targetLibId
    }
  })
}
// 开启聊天
function startChat(id: string) {
  checkChatConditionApi(id).then(result => {
    router.push('/workspace/chat?appId=' + id)
  })
}

function handleAddSuccess() {
  addDialogVisible.value = false
  loadTable()
}

function handleUpdateSuccess() {
  updateDialogVisible.value = false
  loadTable()
}
function handleBindSuccess() {
  bindLibDialogVisible.value = false
  loadTable()
}
function handlePublishSuccess() {
  publishDialogVisible.value = false
  loadTable()
}

function getPublishStatusText(appId: string | number) {
  const status = publishStatusMap.value[String(appId)]
  if (!status || Number(status.enabled) !== 1) {
    return '关闭'
  }
  return status.accessType === 'PASSWORD' ? '密码访问' : '公开访问'
}

function getPublishStatusClass(appId: string | number) {
  const statusText = getPublishStatusText(appId)
  if (statusText === '公开访问') {
    return 'workspace-resource-card__value--success'
  }
  if (statusText === '密码访问') {
    return 'workspace-resource-card__value--warning'
  }
  return 'workspace-resource-card__value--muted'
}

async function loadPublishStatuses() {
  const rows = currentRows.value
  if (!rows.length) {
    publishStatusMap.value = {}
    return
  }

  const statusEntries = await Promise.all(rows.map(async (row: { id: string | number }) => {
    try {
      const result = await queryPublishConfigApi(String(row.id))
      const data = result.data || {}
      return [String(row.id), { enabled: Number(data.enabled ?? 0), accessType: String(data.accessType || 'PUBLIC') }] as const
    } catch {
      return [String(row.id), { enabled: 0, accessType: 'PUBLIC' }] as const
    }
  }))

  publishStatusMap.value = Object.fromEntries(statusEntries)
}
// 解绑知识库
function unbindLib(appId:string) {
  unBindLibApi(appId).then(result => {
    ElMessage.success(result.msg)
    loadTable()
  })
}
onMounted(() => {
  loadTable()
})

watch(
  () => tableData.rows,
  () => {
    loadPublishStatuses()
  }
)
</script>
<style scoped>
.app-manage-page {
  gap: 16px;
}

.app-summary-panel,
.card-list-panel {
  padding: 18px 20px;
}

.workspace-resource-card__row--tight {
  flex-wrap: nowrap;
}

.workspace-resource-card__actions-inline--compact {
  flex-shrink: 0;
  flex-wrap: nowrap;
  margin-left: auto;
  gap: 10px;
}

.workspace-resource-card__value--truncate {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-resource-card__value--success {
  color: #027a48;
}

.workspace-resource-card__value--warning {
  color: #b54708;
}

.workspace-resource-card__value--muted {
  color: var(--space-text-soft);
}

@media (max-width: 640px) {
  .workspace-resource-card__row--tight {
    flex-wrap: wrap;
  }

  .workspace-resource-card__actions-inline--compact {
    width: 100%;
    margin-left: 0;
  }
}

.app-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.78);
  flex-shrink: 0;
}

.app-tip-card__dot--success {
  background: #12b76a;
}

.app-tip-card__dot--warning {
  background: #f59e0b;
}

.card-wrapper {
  margin-bottom: 14px;
}

.bind-unbind-link {
  font-size: 12px;
}

@media (max-width: 900px) {
  .app-tip-grid {
    grid-template-columns: 1fr;
  }
}
</style>
