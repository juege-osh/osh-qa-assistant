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
          <el-form-item>
            <el-button class="workspace-btn workspace-btn--ghost" @click="rebuildCurrentLib"
              :disabled="!hasLibId">
              重建当前知识库索引
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
            <el-button class="workspace-btn workspace-btn--ghost" @click="rebuildFile(scope.row)"
              :disabled="scope.row.status !== 1">
              重建索引
            </el-button>
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

    <el-dialog v-model="previewDialogVisible" title="文件预览" width="920px" class="file-preview-dialog">
      <section class="preview-shell">
        <section class="preview-meta">
          <div class="preview-chip">文件：{{ previewData.fileName || '-' }}</div>
          <div class="preview-chip">字符数：{{ previewData.charCount ?? '-' }}</div>
          <div class="preview-chip">状态：{{ previewData.statusDesc || previewData.status || '-' }}</div>
          <div class="preview-chip">切分后 chunk：{{ previewData.chunkCount ?? 0 }}</div>
        </section>

        <section class="preview-summary">
          <div class="preview-summary-copy">
            <div class="preview-summary-title">当前切分规则</div>
            <div class="preview-summary-desc">
              现在入库前会按同一套规则完成切分。你可以先看前几个 chunk 的长度和内容，再决定是否需要调整 chunk 大小或切分策略。
            </div>
          </div>
          <div class="preview-config-grid">
            <div class="preview-config-card">
              <span class="preview-config-label">切分策略</span>
              <strong class="preview-config-value">{{ formatSplitStrategy(previewData.splitConfig?.strategy) }}</strong>
            </div>
            <div class="preview-config-card">
              <span class="preview-config-label">chunk 大小</span>
              <strong class="preview-config-value">{{ previewData.splitConfig?.chunkSize ?? '-' }}</strong>
            </div>
            <div class="preview-config-card">
              <span class="preview-config-label">语义段长度上限</span>
              <strong class="preview-config-value">{{ previewData.splitConfig?.semanticSectionMaxChars ?? '-' }}</strong>
            </div>
            <div class="preview-config-card">
              <span class="preview-config-label">最小 chunk</span>
              <strong class="preview-config-value">{{ previewData.splitConfig?.minChunkSizeChars ?? '-' }}</strong>
            </div>
            <div class="preview-config-card">
              <span class="preview-config-label">最小保留长度</span>
              <strong class="preview-config-value">{{ previewData.splitConfig?.minChunkLengthToEmbed ?? '-' }}</strong>
            </div>
            <div class="preview-config-card">
              <span class="preview-config-label">最大 chunk 数</span>
              <strong class="preview-config-value">{{ previewData.splitConfig?.maxNumChunks ?? '-' }}</strong>
            </div>
            <div class="preview-config-card">
              <span class="preview-config-label">保留分隔符</span>
              <strong class="preview-config-value">{{ previewData.splitConfig?.keepSeparator ? '是' : '否' }}</strong>
            </div>
            <div class="preview-config-card">
              <span class="preview-config-label">预览 chunk 数</span>
              <strong class="preview-config-value">{{ previewData.splitConfig?.previewChunkLimit ?? '-' }}</strong>
            </div>
          </div>
        </section>

        <section class="preview-playground">
          <div class="preview-playground-copy">
            <div class="preview-summary-title">切分试算</div>
            <div class="preview-summary-desc">
              这里的修改只影响当前预览，不会直接改数据库。确认效果后，再使用“重建索引”把新规则真正应用到知识库。
            </div>
          </div>
          <div class="preview-playground-grid">
            <el-select v-model="previewDraft.strategy" style="width: 140px">
              <el-option label="语义优先" value="semantic" />
              <el-option label="纯 Token" value="token" />
            </el-select>
            <el-input-number v-model="previewDraft.chunkSize" :min="100" :max="4000" :step="50" controls-position="right" />
            <el-input-number v-model="previewDraft.minChunkSizeChars" :min="50" :max="2000" :step="50" controls-position="right" />
            <el-input-number v-model="previewDraft.semanticSectionMaxChars" :min="100" :max="5000" :step="100" controls-position="right" />
            <el-input-number v-model="previewDraft.previewChunkLimit" :min="1" :max="30" controls-position="right" />
            <el-switch v-model="previewDraft.keepSeparator" inline-prompt active-text="保留分隔符" inactive-text="移除分隔符" />
          </div>
          <div class="preview-playground-labels">
            <span>策略</span>
            <span>chunk 大小</span>
            <span>最小 chunk</span>
            <span>语义段上限</span>
            <span>预览条数</span>
            <span>分隔符</span>
          </div>
          <div class="preview-playground-actions">
            <el-button class="workspace-btn workspace-btn--ghost" @click="resetPreviewDraft">
              恢复当前规则
            </el-button>
            <el-button type="primary" class="workspace-btn workspace-btn--primary" :loading="previewSplitLoading" @click="previewSplit">
              试算切分
            </el-button>
          </div>
        </section>

        <section class="preview-tabbar">
          <el-segmented v-model="previewTab" :options="previewTabOptions" />
        </section>

        <section v-if="previewTab === 'content'">
          <pre class="preview-content">{{ previewData.content || '暂无内容' }}</pre>
        </section>

        <section v-else class="chunk-list">
          <article v-for="chunk in previewData.chunks" :key="chunk.index" class="chunk-card">
            <div class="chunk-card-head">
              <div class="chunk-card-title">Chunk {{ chunk.index }}</div>
              <div class="chunk-card-meta">{{ chunk.length }} 字</div>
            </div>
            <pre class="chunk-card-content">{{ chunk.content || '暂无内容' }}</pre>
          </article>
          <div v-if="!previewData.chunks.length" class="chunk-empty-state">
            当前没有可预览的切分结果，请先确认文件是否已成功读取。
          </div>
        </section>
      </section>
    </el-dialog>
  </div>
</template>
<script setup name='UploadFileManage' lang='ts'>
import { ref, reactive, onMounted, computed } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageUploadFileApi,deleteUploadFileByIdApi, updateUploadFileStatusApi, rebuildUploadFileByIdApi, rebuildUploadFileByLibIdApi } from '@/api/workspace/uploadFileApi';
import { previewFileApi, previewFileSplitApi } from '@/api/workspace/filePreviewApi';
import { pageKnowledgeLibApi } from '@/api/workspace/knowledgeLibApi';
import { useRoute } from 'vue-router';
import { Plus, Delete } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { saveItem, getItem } from '@/util/storageUtil';

const STORAGE_LAST_LIB_ID_KEY = 'last-selected-lib-id'
const previewTabOptions = [
  { label: '原文预览', value: 'content' },
  { label: '切分预览', value: 'chunks' }
]

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
const previewTab = ref('chunks')
const previewSplitLoading = ref(false)
const previewTargetId = ref('')
const previewData = reactive({
  fileName: '',
  charCount: 0,
  status: '',
  statusDesc: '',
  content: '',
  chunkCount: 0,
  splitConfig: {
    strategy: '',
    chunkSize: 0,
    semanticSectionMaxChars: 0,
    minChunkSizeChars: 0,
    minChunkLengthToEmbed: 0,
    maxNumChunks: 0,
    keepSeparator: true,
    previewChunkLimit: 0
  },
  chunks: [] as Array<{ index: number; length: number; content: string }>
})
const previewDraft = reactive({
  strategy: 'semantic',
  chunkSize: 800,
  minChunkSizeChars: 350,
  minChunkLengthToEmbed: 5,
  maxNumChunks: 10000,
  keepSeparator: true,
  semanticSectionMaxChars: 1200,
  previewChunkLimit: 8
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
    previewTargetId.value = row.id
    resetPreviewDraft()
    previewTab.value = 'chunks'
    previewDialogVisible.value = true
  })
}

function resetPreviewDraft() {
  previewDraft.strategy = previewData.splitConfig?.strategy || 'semantic'
  previewDraft.chunkSize = previewData.splitConfig?.chunkSize || 800
  previewDraft.minChunkSizeChars = previewData.splitConfig?.minChunkSizeChars || 350
  previewDraft.minChunkLengthToEmbed = previewData.splitConfig?.minChunkLengthToEmbed || 5
  previewDraft.maxNumChunks = previewData.splitConfig?.maxNumChunks || 10000
  previewDraft.keepSeparator = previewData.splitConfig?.keepSeparator ?? true
  previewDraft.semanticSectionMaxChars = previewData.splitConfig?.semanticSectionMaxChars || 1200
  previewDraft.previewChunkLimit = previewData.splitConfig?.previewChunkLimit || 8
}

function previewSplit() {
  if (!previewTargetId.value) {
    ElMessage.warning('请先选择要预览的文件')
    return
  }
  previewSplitLoading.value = true
  previewFileSplitApi({
    id: previewTargetId.value,
    ...previewDraft
  }).then(result => {
    Object.assign(previewData, result.data || {})
    previewTab.value = 'chunks'
    ElMessage.success('已按试算规则更新切分预览')
  }).finally(() => {
    previewSplitLoading.value = false
  })
}

function rebuildFile(row: { id: string; fileName?: string; status?: number }) {
  ElMessageBox.confirm(
    `确认按当前切分规则重建「${row.fileName || '该文件'}」的索引吗？`,
    '重建索引确认',
    {
      type: 'warning',
      confirmButtonText: '确认重建',
      cancelButtonText: '取消'
    }
  ).then(() => {
    rebuildUploadFileByIdApi(row.id).then(result => {
      ElMessage.success(result.msg)
      loadTable()
    })
  }).catch(() => {})
}

function rebuildCurrentLib() {
  if (!hasLibId.value) {
    return
  }
  ElMessageBox.confirm(
    '确认按当前切分规则重建当前知识库下所有启用文件的索引吗？这个过程会覆盖旧的向量切分结果。',
    '批量重建确认',
    {
      type: 'warning',
      confirmButtonText: '确认重建',
      cancelButtonText: '取消'
    }
  ).then(() => {
    rebuildUploadFileByLibIdApi(String(searchData.libId)).then(result => {
      ElMessage.success(result.msg)
      loadTable()
    })
  }).catch(() => {})
}

function formatSplitStrategy(strategy?: string) {
  if (strategy === 'semantic') {
    return '语义优先'
  }
  if (strategy === 'token') {
    return '纯 Token'
  }
  return strategy || '-'
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
  margin-bottom: 16px;
}

.preview-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-chip {
  padding: 8px 12px;
  border: 1px solid var(--space-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--space-text);
  font-size: 13px;
  font-weight: 600;
}

.preview-summary {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1.8fr);
  gap: 16px;
  padding: 18px;
  border: 1px solid rgba(64, 158, 255, 0.14);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
}

.preview-summary-title {
  color: var(--space-text);
  font-size: 15px;
  font-weight: 700;
}

.preview-summary-desc {
  margin-top: 8px;
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.preview-config-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.preview-config-card {
  padding: 14px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.98), rgba(239, 246, 255, 0.98));
}

.preview-config-label {
  display: block;
  color: var(--space-text-soft);
  font-size: 12px;
}

.preview-config-value {
  display: block;
  margin-top: 8px;
  color: var(--space-primary-strong);
  font-size: 19px;
  line-height: 1.1;
}

.preview-tabbar {
  display: flex;
  justify-content: flex-start;
}

.preview-playground {
  padding: 18px;
  border: 1px solid rgba(64, 158, 255, 0.14);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
}

.preview-playground-grid,
.preview-playground-labels {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  align-items: center;
}

.preview-playground-grid {
  margin-top: 14px;
}

.preview-playground-labels {
  margin-top: 8px;
  color: var(--space-text-soft);
  font-size: 12px;
}

.preview-playground-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

:deep(.file-preview-dialog .el-dialog__body) {
  padding-top: 12px;
}

:deep(.preview-tabbar .el-segmented) {
  padding: 4px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.82);
}

:deep(.preview-tabbar .el-segmented__item) {
  min-width: 112px;
  font-weight: 600;
}

:deep(.preview-tabbar .el-segmented__item.is-selected) {
  color: #fff;
  background: linear-gradient(135deg, var(--space-primary-strong), #0f766e);
}

.preview-content {
  max-height: 42vh;
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

.chunk-list {
  display: grid;
  gap: 12px;
  max-height: 42vh;
  overflow: auto;
  padding-right: 4px;
}

.chunk-card {
  border: 1px solid rgba(64, 158, 255, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.98);
  overflow: hidden;
}

.chunk-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(64, 158, 255, 0.1);
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.95), rgba(241, 245, 249, 0.95));
}

.chunk-card-title {
  color: var(--space-text);
  font-size: 14px;
  font-weight: 700;
}

.chunk-card-meta {
  color: var(--space-text-soft);
  font-size: 12px;
  font-weight: 600;
}

.chunk-card-content {
  margin: 0;
  padding: 16px;
  max-height: 180px;
  overflow: auto;
  color: var(--space-text);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  background: rgba(255, 255, 255, 0.9);
}

.chunk-empty-state {
  padding: 24px 18px;
  border: 1px dashed rgba(148, 163, 184, 0.5);
  border-radius: 18px;
  color: var(--space-text-soft);
  text-align: center;
  background: rgba(248, 250, 252, 0.9);
}

@media (max-width: 900px) {
  .preview-summary {
    grid-template-columns: 1fr;
  }

  .preview-config-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .preview-playground-grid,
  .preview-playground-labels {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
