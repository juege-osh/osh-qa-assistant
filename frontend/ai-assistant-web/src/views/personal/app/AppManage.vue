<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">应用工作区</div>
      <div class="hero-subtitle">
        为每个业务场景创建独立应用，按需绑定知识库，管理图标、描述与问答入口。应用是对外提供聊天能力的最小交付单元。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">应用总数：{{ tableData.total || 0 }}</span>
        <span class="hero-badge">可随时绑定或解绑知识库</span>
        <span class="hero-badge">支持直接进入聊天调试</span>
      </div>
    </section>

    <section class="toolbar-panel glass-panel">
      <div class="toolbar-copy">
        <div class="toolbar-title">应用列表</div>
        <div class="toolbar-desc">
          先创建应用即可进入聊天调试；如果绑定知识库，回答会优先参考知识库内容。这里保留了所有原有能力，并补强了状态展示与操作引导。
        </div>
      </div>
      <div class="toolbar-actions">
        <el-form :model="searchData" :inline="true">
          <el-form-item>
            <el-input type="text" placeholder="按应用名称搜索" v-model="searchData.appName" clearable style="width: 180px"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button @click="loadTable" type="primary">查询</el-button>
          </el-form-item>
          <el-form-item>
            <el-button @click="addDialogVisible = true" type="success" :icon="Plus">新增应用</el-button>
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
                  <el-link v-if="row.libName" class="bind-unbind-link" @click="unbindLib(row.id)" type="primary">解绑</el-link>
                  <el-link v-else class="bind-unbind-link" @click="openBindLibDialog(row.id)" type="primary">绑定</el-link>
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
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useResource } from '@/hooks/useResource';

// 添加对话框是否显示
let addDialogVisible = ref(false)
let updateDialogVisible = ref(false)
let bindLibDialogVisible = ref(false)
let idToUpdate = ref('')
let idToBindLib = ref('')

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
  font-weight: 900;
  color: #fff;
  letter-spacing: .03em;
}

.time-tip {
  padding-right: .3rem;
  color: var(--space-muted);
}

:deep(.el-card__body) {
  padding: 16px;
}

:deep(.el-card__footer) {
  padding: 12px 16px;
  background: rgba(5, 12, 32, 0.45) !important;
}

.app-card {
  position: relative;
  min-height: 250px;
  border: 1px solid var(--space-border) !important;
  background:
    radial-gradient(circle at 90% 8%, rgba(52, 211, 153, 0.16), transparent 28%),
    linear-gradient(145deg, rgba(13, 23, 54, 0.88), rgba(18, 13, 49, 0.78)) !important;
  transition: transform .2s ease, box-shadow .2s ease;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 26px 80px rgba(0, 0, 0, 0.5), 0 0 36px rgba(52, 211, 153, 0.18) !important;
}

.card-wrapper {
  margin-bottom: 16px;
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
  margin-left: 15px;
  flex: 1;
}

.desc {
  margin-top: 14px;
  color: var(--space-muted);
}

.app-id {
  color: var(--space-muted) !important;
}

.desc-text {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  line-clamp: 3;
  -webkit-line-clamp: 3;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 2em;
  min-height: 6em;
  color: rgba(234, 246, 255, 0.84) !important;
}

.bind-unbind-link {
  display: inline-block;
  margin-left: 20px;
  font-size: .7em;
}
</style>
