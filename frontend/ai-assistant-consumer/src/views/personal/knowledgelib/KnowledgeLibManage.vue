<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">知识库中心</div>
      <div class="hero-subtitle">
        在这里集中管理知识库资产，包括文档数量、字符规模、归属应用与内容描述。知识库是问答质量的核心，建议按业务域拆分管理。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">知识库总数：{{ tableData.total || 0 }}</span>
        <span class="hero-badge">支持文档管理与应用绑定</span>
        <span class="hero-badge">适合按业务域拆分内容</span>
      </div>
    </section>

    <section class="toolbar-panel glass-panel">
      <div class="toolbar-copy">
        <div class="toolbar-title">知识库列表</div>
        <div class="toolbar-desc">
          从这里可以快速判断每个知识库的规模、挂载情况和维护状态，并直接进入文档管理做增删改查。
        </div>
      </div>
      <div class="toolbar-actions">
        <el-form :model="searchData" :inline="true">
          <el-form-item>
            <el-input type="text" placeholder="按知识库名称搜索" v-model="searchData.libName" clearable style="width: 180px"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button @click="loadTable" type="primary">查询</el-button>
          </el-form-item>
          <el-form-item>
            <el-button @click="addDialogVisible = true" type="success" :icon="Plus">新增知识库</el-button>
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
                  <el-avatar v-if="row.iconPath" :src="toAddressable(row.iconPath)">
                  </el-avatar>
                  <el-icon v-else color="#409eff" :size="40">
                    <Folder />
                  </el-icon>
                </div>
                <div class="right">
                  <div>
                    <span class="title">{{ row.libName }}</span>
                  </div>
                  <div>
                    <el-text class="middle-row" truncated>
                      <span class="middle-row-item">{{ row.docCount }}个文档</span>
                      <span class="middle-row-item">{{ row.charCount }}个字符</span>
                    </el-text>
                  </div>
                  <div v-if="row.appId">
                    <el-text>
                      <span>所属应用:</span>
                      <span>{{ row.appName }}</span>
                    </el-text>
                  </div>
                </div>
              </div>
              <div>
                <el-text class="desc-text">
                  描述:{{ row.libDesc }}
                </el-text>
              </div>
            </div>
            <!-- 底部操作 -->
            <template #footer>
              <div class="footer">
                <el-button @click="openUpdateDialog(row.id)" type="primary">编辑</el-button>
                <el-button @click="$router.push('/personal/uploadFile/manage?libId=' + row.id)"
                  type="primary">文档管理</el-button>
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
    <AddKnowledgeLib :addDialogVisible="addDialogVisible" @closeDialog="addDialogVisible = false"
      @addSuccess="handleAddSuccess">
    </AddKnowledgeLib>
    <!-- 更新对话框 -->
    <UpdateKnowledgeLib :updateDialogVisible="updateDialogVisible" @closeDialog="updateDialogVisible = false"
      :idToUpdate="idToUpdate" @updateSuccess="handleUpdateSuccess"></UpdateKnowledgeLib>
  </div>
</template>
<script setup name='KnowledgeLibManage' lang='ts'>
import { ref, reactive, onMounted } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageKnowledgeLibApi, deleteKnowledgeLibByIdApi } from '@/api/knowledgeLibApi';
import AddKnowledgeLib from '@/views/personal/knowledgelib/AddKnowledgeLib.vue';
import UpdateKnowledgeLib from '@/views/personal/knowledgelib/UpdateKnowledgeLib.vue';
import { Plus, Edit, Delete } from '@element-plus/icons-vue';
import { useResource } from '@/hooks/useResource';

// 添加对话框是否显示
let addDialogVisible = ref(false)
let updateDialogVisible = ref(false)
let idToUpdate = ref('')

let searchFormData = reactive({
  username: '',
  appName: '',
  libName: '',
})

let {
  searchData,
  tableData,
  loadTable,
  deleteById,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageKnowledgeLibApi, deleteByIdApi: deleteKnowledgeLibByIdApi })
let {toAddressable} = useResource()
// 打开更新对话框
function openUpdateDialog(id: string) {
  idToUpdate.value = id
  updateDialogVisible.value = true
}

function handleAddSuccess() {
  addDialogVisible.value = false
  loadTable()
}

function handleUpdateSuccess() {
  updateDialogVisible.value = false
  loadTable()
}
onMounted(() => {
  loadTable()
})
</script>
<style scoped>
.left {
  background: rgba(100, 232, 255, 0.08);
  border: 1px solid rgba(100, 232, 255, 0.32);
  padding: 3px;
  margin-right: 10px;
  border-radius: 8px;
  width: 60px;
  height: 60px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.right {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.title {
  font-weight: 800;
  color: #fff;
}
.middle-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.middle-row-item {
  padding-right: .5rem;
}

:deep(.el-card__body) {
  padding: 10px;
}

:deep(.el-card__footer) {
  padding: 10px;
}

.card-wrapper {
  margin-bottom: 16px;
}

.one-row {
  display: flex;
  justify-content: left;
  align-items: center;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 最多显示2行,没有也占据2行的位置 */
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
</style>
