<template>
  <div class="page-shell">
    <section class="context-strip">
      <el-button text class="context-back" @click="$router.push('/workspace/knowledgeLib/manage')">返回知识库</el-button>
      <span class="context-note">文档维护属于知识库工作流，从知识库列表进入更适合持续整理内容。</span>
    </section>
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
            <el-button @click="$router.push('/workspace/uploadFile/toAdd?libId=' + searchData.libId)" type="success"
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageUploadFileApi,deleteUploadFileByIdApi, updateUploadFileStatusApi } from '@/api/workspace/uploadFileApi';
import { previewFileApi } from '@/api/workspace/filePreviewApi';
import { pageKnowledgeLibApi } from '@/api/workspace/knowledgeLibApi';
import { useRoute } from 'vue-router';
import { Plus, Edit, Delete } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { saveItem, getItem } from '@/util/storageUtil';

const STORAGE_LAST_LIB_ID_KEY = 'last-selected-lib-id'

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

// 是否已选中知识库
const hasLibId = computed(() => {
  return String(searchData.libId || '').trim().length > 0
})

// 处理请求入参
function handleLibId() {
  const libIdFromQs = route.query.libId
  if (libIdFromQs) {
    searchData.libId = libIdFromQs as string
    saveItem(STORAGE_LAST_LIB_ID_KEY, searchData.libId)
  } else {
    // 尝试从 localStorage 中恢复上次选中的 libId
    const lastLibId = getItem(STORAGE_LAST_LIB_ID_KEY)
    if (lastLibId) {
      searchData.libId = lastLibId
    }
  }
}

/**
 * 当 URL 和 localStorage 中都没有 libId 时，自动加载用户的知识库列表并选中第一个
 */
async function autoSelectLib() {
  try {
    const result = await pageKnowledgeLibApi({ pageNow: 1, pageSize: 1 })
    if (result.data && result.data.length > 0) {
      searchData.libId = String(result.data[0].id)
      saveItem(STORAGE_LAST_LIB_ID_KEY, searchData.libId)
      loadTable()
    } else {
      ElMessage.info('您还没有创建知识库，请先前往知识库管理创建后再查看文件')
    }
  } catch {
    ElMessage.warning('自动加载知识库列表失败，请从知识库列表进入文件管理')
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
  if (hasLibId.value) {
    loadTable()
  } else {
    autoSelectLib()
  }
})
</script>
<style scoped>
.table-panel,
.pagination-panel {
  padding: 18px;
}

.context-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  padding: 0 2px;
}

.context-back {
  padding-left: 0;
  padding-right: 0;
  font-weight: 600;
}

.context-note {
  color: var(--space-text-soft);
  font-size: 13px;
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.preview-chip {
  padding: 8px 12px;
  border: 1px solid var(--space-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--space-text);
}

.preview-content {
  max-height: 56vh;
  overflow: auto;
  padding: 16px;
  border: 1px solid var(--space-border);
  border-radius: 18px;
  background: rgba(4, 10, 8, 0.78);
  color: rgba(234, 246, 255, 0.9);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
