<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">文档管理中心</div>
      <div class="hero-subtitle">
        在这里维护当前知识库下的所有文件，包括上传、启用、禁用、删除以及查看字符数和召回次数，用于持续优化知识库质量。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">文件总数：{{ tableData.total || 0 }}</span>
        <span class="hero-badge">支持快速启停</span>
        <span class="hero-badge">可观测召回次数</span>
      </div>
    </section>

    <section class="toolbar-panel glass-panel">
      <div class="toolbar-copy">
        <div class="toolbar-title">文件列表</div>
        <div class="toolbar-desc">
          如果某份文档内容错误或过期，可以先禁用再观察效果；如果召回次数长期很低，建议优化标题、内容结构或文档归属。
        </div>
      </div>
      <div class="toolbar-actions">
        <el-form :model="searchData" :inline="true">
          <el-form-item>
            <el-input type="text" placeholder="文件名" v-model="searchData.fileName" clearable
              style="width: 180px"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button @click="loadTable" type="primary">查询</el-button>
          </el-form-item>
          <el-form-item>
            <el-button @click="$router.push('/personal/uploadFile/toAdd?libId=' + searchData.libId)" type="success"
              :icon="Plus">
              新增文件
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <!-- 表格   -->
    <section class="glass-panel table-panel">
      <el-table :data="tableData.rows" stripe :border="true" style="width: 100%">
        <el-table-column prop="fileName" label="文件名称">
        </el-table-column>
        <el-table-column prop="charCount" label="字符数">
        </el-table-column>
        <el-table-column prop="recallCount" label="召回次数">
        </el-table-column>
        <el-table-column prop="statusDesc" label="状态">
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间">
        </el-table-column>
        <el-table-column label="操作">
          <template v-slot:default="scope">
            <el-button type="primary" plain @click="openPreview(scope.row)">预览</el-button>
            <el-button type="primary" v-if="scope.row.status === 0"
              @click="updateStatus(scope.row.id,1)">启用
            </el-button>
            <el-button type="primary" v-if="scope.row.status === 1"
              @click="updateStatus(scope.row.id,0)">禁用
            </el-button>
            <el-button type="danger" :icon="Delete" @click="deleteById(scope.row.id)">删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
    <!-- 分页   -->
    <section class="mt-dot5 glass-panel pagination-panel">
      <el-pagination @size-change="handlePageSizeChange" @current-change="handlePageNowChange"
        :current-page="searchData.pageNow" :page-sizes="[10, 30, 50]" :page-size="searchData.pageSize"
        layout="total, sizes, prev, pager, next, jumper" :total="tableData.total">
      </el-pagination>
    </section>

    <el-dialog v-model="previewDialogVisible" title="文件预览" width="860px">
      <section class="preview-meta">
        <div class="preview-chip">文件：{{ previewData.fileName || '-' }}</div>
        <div class="preview-chip">字符数：{{ previewData.charCount ?? '-' }}</div>
        <div class="preview-chip">状态：{{ previewData.statusDesc || previewData.status || '-' }}</div>
      </section>
      <pre class="preview-content">{{ previewData.content || '暂无内容' }}</pre>
    </el-dialog>
  </div>
</template>
<script setup name='UploadFileManage' lang='ts'>
import { ref, reactive, onMounted } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageUploadFileApi,deleteUploadFileByIdApi, updateUploadFileStatusApi } from '@/api/uploadFileApi';
import { previewFileApi } from '@/api/filePreviewApi';
import { useRoute } from 'vue-router';
import { Plus, Edit, Delete } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

let searchFormData = reactive({
  fileName: '',
  libId: '',
})

let {
  searchData,
  tableData,
  loadTable,
  deleteById,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageUploadFileApi,deleteByIdApi: deleteUploadFileByIdApi })
let route = useRoute()
const previewDialogVisible = ref(false)
const previewData = reactive({
  fileName: '',
  charCount: 0,
  status: '',
  statusDesc: '',
  content: ''
})

// 处理请求入参
function handleLibId() {
  const libIdFromQs = route.query.libId
  if (libIdFromQs) {
    searchData.libId = libIdFromQs as string
  }
}
// 修改文件状态
function updateStatus(fileId:string,status:number) {
  updateUploadFileStatusApi({
    id:fileId,
    status:status
  }).then(result => {
    ElMessage.success(result.msg)
    loadTable()
  })
}

function openPreview(row: { id: string }) {
  previewFileApi(row.id).then(result => {
    Object.assign(previewData, result.data || {})
    previewDialogVisible.value = true
  })
}

onMounted(() => {
  handleLibId()
  loadTable()
})
</script>
<style scoped>
.table-panel,
.pagination-panel {
  padding: 18px;
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.preview-chip {
  padding: 8px 12px;
  border: 1px solid rgba(52, 211, 153, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--space-text);
}

.preview-content {
  max-height: 56vh;
  overflow: auto;
  padding: 16px;
  border: 1px solid rgba(52, 211, 153, 0.18);
  border-radius: 18px;
  background: rgba(4, 10, 8, 0.78);
  color: rgba(234, 246, 255, 0.9);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
