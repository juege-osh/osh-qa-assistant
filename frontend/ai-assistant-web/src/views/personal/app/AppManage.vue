<template>
  <div class="page-shell app-manage-page">
    <section class="hero-panel">
      <div class="hero-title">应用</div>
      <div class="hero-subtitle">
        先建一个应用，再决定要不要绑定知识库、开始聊天或打开公开访问。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">应用总数：{{ tableData.total || 0 }}</span>
        <span class="hero-badge">未绑定知识库也能先测试</span>
        <span class="hero-badge">公开访问可单独设置</span>
      </div>
    </section>

    <section class="toolbar-panel glass-panel workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">Manage Apps</div>
        <div class="toolbar-title">应用列表</div>
        <div class="toolbar-desc">
          先看名称、知识库和模型，需要时直接编辑、绑定或开聊。
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
            <el-button @click="addDialogVisible = true" type="success" :icon="Plus" class="workspace-btn app-create-btn">新增应用</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <!-- 表格   -->
    <section>
      <el-row :gutter="10">
        <el-col v-for="row in tableData.rows" class="card-wrapper" :key="row.id" :lg="8">
          <el-card class="app-card" shadow="hover">
            <!-- 中间描述 -->
            <div class="content">
              <div class="one-row">
                <div class="left">
                  <el-avatar :src="convertIconPath(row.iconPath)">
                  </el-avatar>
                </div>
                <div class="right">
                  <div>
                    <span class="title">{{ row.appName }}</span>
                  </div>
                  <div>
                    <el-text class="time" truncated>
                      <span class="time-tip">最后编辑于</span>
                      <span>{{ row.modifiedTime }}</span>
                    </el-text>
                  </div>
                </div>
              </div>
              <div class="desc">
                <div>
                  <el-text class="app-id">
                    应用标识:{{ row.id }}
                  </el-text>
                </div>
                <div>
                  <el-text class="app-id">
                    知识库:
                    <span>{{ row.libName }}</span>
                  </el-text>
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
                <div>
                  <el-text class="app-id">
                    模型:
                    <span>{{ row.chatModel || '系统默认' }}</span>
                  </el-text>
                </div>
                <div>
                  <el-text class="app-id">
                    公开发布:
                    <span>可单独设置</span>
                  </el-text>
                </div>
                <div>
                  <el-text class="desc-text">
                    描述:{{ row.appDesc }}
                  </el-text>
                </div>
              </div>
            </div>
            <!-- 底部操作 -->
            <template #footer>
              <div class="footer">
                <el-button @click="openUpdateDialog(row.id)" type="primary">编辑</el-button>
                <el-button @click="openPublishDialog(row.id)" type="primary" plain>公开发布</el-button>
                <el-button @click="startChat(row.id)" type="primary">开始聊天</el-button>
                <el-button @click="deleteById(row.id)" type="primary">删除</el-button>
              </div>
            </template>
          </el-card>
        </el-col>
      </el-row>
    </section>
    <!-- 分页   -->
    <section class="mt-dot5">
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
import { ref, reactive, onMounted } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageAppApi, deleteAppByIdApi, checkChatConditionApi, unBindLibApi } from '@/api/workspace/appApi';
import { getImage } from '@/util/AssetsImageUtil';
import { Plus, Edit, Delete } from '@element-plus/icons-vue';
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
</script>
<style scoped>
.title {
  font-weight: 700;
  color: var(--space-text);
  letter-spacing: .02em;
  font-size: 14px;
}

.app-manage-page {
  gap: 16px;
}

.app-create-btn {
  border: 0 !important;
  background: linear-gradient(135deg, #7ed957 0%, #64c934 58%, #4fb423 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 14px 28px rgba(91, 191, 44, 0.24) !important;
}

.app-create-btn:hover {
  background: linear-gradient(135deg, #8de164 0%, #6fd33a 58%, #58bb28 100%) !important;
  box-shadow: 0 16px 32px rgba(91, 191, 44, 0.3) !important;
}

.time-tip {
  padding-right: .3rem;
  color: var(--space-muted);
  font-size: 12px;
}

:deep(.el-card__body) {
  padding: 14px;
}

:deep(.el-card__footer) {
  padding: 10px 14px;
  background: #fafafa !important;
}

.app-card {
  position: relative;
  min-height: 220px;
  border: 1px solid var(--space-border) !important;
  background: #ffffff !important;
  transition: box-shadow .2s ease;
}

.app-card:hover {
  transform: none;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1) !important;
}

.card-wrapper {
  margin-bottom: 14px;
}

.one-row,
.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.one-row {
  justify-self: left;
}

.right {
  margin-left: 12px;
  flex: 1;
}

.desc {
  margin-top: 10px;
  color: var(--space-muted);
  font-size: 13px;
}

.app-id {
  color: var(--space-muted) !important;
  font-size: 12px;
}

.desc-text {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  line-clamp: 3;
  -webkit-line-clamp: 3;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.8em;
  min-height: 5em;
  color: var(--space-muted) !important;
  font-size: 13px;
}

.bind-unbind-link {
  display: inline-block;
  margin-left: 20px;
  font-size: .7em;
}
</style>
