<template>
  <div class="page-shell uploadfile-manage-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <el-button text class="workspace-btn workspace-btn--text" @click="$router.push('/workspace/knowledgeLib/manage')">返回知识库</el-button>
        <span class="workspace-context-note">当前文档按知识库独立管理，可继续上传、预览切分或重建索引。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">知识库 {{ currentLibDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">文件 {{ totalFileCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">召回 {{ currentPageRecallDisplay }}</span>
      </div>
    </section>

    <section class="workspace-section-card file-overview-panel workspace-dashboard-panel">
      <div class="file-overview-head workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="panel-title">文档工作区</div>
          <div class="panel-desc workspace-panel-desc">先确认当前知识库、文件状态和召回规模，再决定是继续上传新资料、预览切分，还是直接重建索引。</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--active">知识库 {{ currentLibDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">启用 {{ enabledFileCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">停用 {{ disabledFileCountDisplay }}</span>
        </div>
      </div>
      <section class="stats-grid workspace-metrics-grid">
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--total">
          <div class="stat-label">当前文件</div>
          <div class="stat-value">{{ totalFileCountDisplay }}</div>
          <div class="stat-help">当前页内已经进入管理视图、可继续处理的文件数量。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
          <div class="stat-label">启用中文档</div>
          <div class="stat-value workspace-stat-value--success">{{ enabledFileCountDisplay }}</div>
          <div class="stat-help">已经参与检索的文件，更适合继续看召回和切分效果。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--time">
          <div class="stat-label">停用中文档</div>
          <div class="stat-value workspace-stat-value--warning">{{ disabledFileCountDisplay }}</div>
          <div class="stat-help">暂未参与检索，适合先确认是否需要重新启用或重建。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--token">
          <div class="stat-label">当前页召回</div>
          <div class="stat-value">{{ currentPageRecallDisplay }}</div>
          <div class="stat-help">结合文件状态一起看，更容易判断哪些资料在真正被命中。</div>
        </article>
      </section>
    </section>

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">文档资产</div>
        <div class="toolbar-title">文件列表</div>
        <div class="toolbar-desc">
          先筛文件，再决定是预览内容、调整状态，还是重建索引。
        </div>
      </div>
      <div class="toolbar-actions workspace-toolbar-actions">
        <el-form :model="searchData" :inline="true" class="workspace-toolbar-form">
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--md">
            <el-input type="text" placeholder="文件名" v-model="searchData.fileName" clearable></el-input>
          </el-form-item>
          <el-form-item>
            <el-button @click="loadTable" type="primary" class="workspace-btn workspace-btn--primary">查询</el-button>
          </el-form-item>
          <el-form-item>
            <el-button @click="$router.push('/workspace/uploadFile/toAdd?libId=' + searchData.libId)" type="primary"
              :icon="Plus" class="workspace-btn workspace-btn--primary">
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
    <section class="workspace-section-card table-panel">
      <el-table :data="tableData.rows" stripe style="width: 100%">
        <el-table-column label="文件" min-width="320">
          <template #default="{ row }">
            <div class="workspace-table-stack">
              <div class="workspace-table-heading">{{ row.fileName || '未命名文件' }}</div>
              <div class="workspace-table-subtext">文件 ID：{{ row.id || '--' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数据规模" min-width="220">
          <template #default="{ row }">
            <div class="workspace-inline-tags">
              <span class="workspace-inline-tag workspace-inline-tag--soft">{{ formatCount(row.charCount) }} 字</span>
              <span class="workspace-inline-tag workspace-inline-tag--soft">召回 {{ formatCount(row.recallCount) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="120">
          <template #default="{ row }">
            <span :class="['workspace-inline-tag', getStatusTagClass(row.status)]">{{ row.statusDesc || '未知状态' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            <div class="workspace-table-note">{{ row.createdTime || '--' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280" class-name="op-col">
          <template v-slot:default="scope">
            <div class="workspace-action-group">
              <el-button class="workspace-btn workspace-btn--ghost workspace-btn--sm" @click="openPreview(scope.row)">预览</el-button>
              <el-button class="workspace-btn workspace-btn--ghost workspace-btn--sm" @click="rebuildFile(scope.row)"
                :disabled="scope.row.status !== 1">
                重建索引
              </el-button>
              <el-button type="primary" class="workspace-btn workspace-btn--primary workspace-btn--sm" v-if="scope.row.status === 0"
                @click="updateStatus(scope.row.id,1)">启用
              </el-button>
              <el-button class="workspace-btn workspace-btn--ghost workspace-btn--sm" v-if="scope.row.status === 1"
                @click="updateStatus(scope.row.id,0)">禁用
              </el-button>
              <el-button class="workspace-btn workspace-btn--ghost workspace-btn--danger workspace-btn--sm" :icon="Delete" @click="deleteById(scope.row.id)">删除
              </el-button>
            </div>
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

    <el-dialog v-model="previewDialogVisible" title="文件预览" width="920px" class="workspace-form-dialog workspace-preview-dialog">
      <div class="dialog-intro">
        先确认文件原文、切分规则和 chunk 预览是否自然，再决定要不要调整规则或重建索引。
      </div>
      <section class="workspace-dialog-tip-panel preview-dialog-tip">
        如果当前回答不稳定，优先看 chunk 是否过粗、过碎或语义断裂；如果切分正常，再考虑回到资料内容本身做补充或清理。
      </section>
      <section class="workspace-preview-shell">
        <section class="workspace-info-card workspace-info-card--flush preview-info-card">
          <div class="workspace-info-grid">
            <div class="workspace-info-item workspace-info-item--full">
              <div class="workspace-info-label">文件名称</div>
              <div class="workspace-info-value">{{ previewData.fileName || '-' }}</div>
            </div>
            <div class="workspace-info-item">
              <div class="workspace-info-label">字符数</div>
              <div class="workspace-info-value">{{ previewData.charCount ?? '-' }}</div>
            </div>
            <div class="workspace-info-item">
              <div class="workspace-info-label">状态</div>
              <div class="workspace-info-value">{{ previewData.statusDesc || previewData.status || '-' }}</div>
            </div>
            <div class="workspace-info-item">
              <div class="workspace-info-label">切分后 chunk</div>
              <div class="workspace-info-value">{{ previewData.chunkCount ?? 0 }}</div>
            </div>
            <div class="workspace-info-item">
              <div class="workspace-info-label">当前预览模式</div>
              <div class="workspace-info-value">{{ previewTab === 'content' ? '原文预览' : '切分预览' }}</div>
            </div>
          </div>
        </section>

        <section class="workspace-info-card workspace-info-card--flush preview-summary-panel">
          <div class="preview-summary-copy workspace-section-copy">
            <div class="preview-summary-title workspace-section-title">当前切分规则</div>
            <div class="preview-summary-desc workspace-section-desc">
              先看切出来的段落顺不顺，再决定要不要调规则。
            </div>
          </div>
          <div class="preview-config-grid workspace-spec-grid workspace-spec-grid--4">
            <div class="preview-config-card workspace-spec-card">
              <span class="preview-config-label workspace-spec-label">切分策略</span>
              <strong class="preview-config-value workspace-spec-value">{{ formatSplitStrategy(previewData.splitConfig?.strategy) }}</strong>
            </div>
            <div class="preview-config-card workspace-spec-card">
              <span class="preview-config-label workspace-spec-label">chunk 大小</span>
              <strong class="preview-config-value workspace-spec-value">{{ previewData.splitConfig?.chunkSize ?? '-' }}</strong>
            </div>
            <div class="preview-config-card workspace-spec-card">
              <span class="preview-config-label workspace-spec-label">语义段长度上限</span>
              <strong class="preview-config-value workspace-spec-value">{{ previewData.splitConfig?.semanticSectionMaxChars ?? '-' }}</strong>
            </div>
            <div class="preview-config-card workspace-spec-card">
              <span class="preview-config-label workspace-spec-label">最小 chunk</span>
              <strong class="preview-config-value workspace-spec-value">{{ previewData.splitConfig?.minChunkSizeChars ?? '-' }}</strong>
            </div>
            <div class="preview-config-card workspace-spec-card">
              <span class="preview-config-label workspace-spec-label">最小保留长度</span>
              <strong class="preview-config-value workspace-spec-value">{{ previewData.splitConfig?.minChunkLengthToEmbed ?? '-' }}</strong>
            </div>
            <div class="preview-config-card workspace-spec-card">
              <span class="preview-config-label workspace-spec-label">最大 chunk 数</span>
              <strong class="preview-config-value workspace-spec-value">{{ previewData.splitConfig?.maxNumChunks ?? '-' }}</strong>
            </div>
            <div class="preview-config-card workspace-spec-card">
              <span class="preview-config-label workspace-spec-label">保留分隔符</span>
              <strong class="preview-config-value workspace-spec-value">{{ previewData.splitConfig?.keepSeparator ? '是' : '否' }}</strong>
            </div>
            <div class="preview-config-card workspace-spec-card">
              <span class="preview-config-label workspace-spec-label">预览 chunk 数</span>
              <strong class="preview-config-value workspace-spec-value">{{ previewData.splitConfig?.previewChunkLimit ?? '-' }}</strong>
            </div>
          </div>
        </section>

        <section class="workspace-preview-playground">
          <div class="workspace-section-copy">
            <div class="preview-summary-title workspace-section-title">切分试算</div>
            <div class="preview-summary-desc workspace-section-desc">
              这里只是试算。满意后再重建索引生效。
            </div>
          </div>
          <div class="workspace-preview-grid workspace-preview-grid--3 preview-draft-grid">
            <div class="workspace-preview-field-card">
              <span class="workspace-preview-field-label">切分策略</span>
              <el-select v-model="previewDraft.strategy">
                <el-option label="语义优先" value="semantic" />
                <el-option label="纯 Token" value="token" />
              </el-select>
            </div>
            <div class="workspace-preview-field-card">
              <span class="workspace-preview-field-label">chunk 大小</span>
              <el-input-number v-model="previewDraft.chunkSize" :min="100" :max="4000" :step="50" controls-position="right" />
            </div>
            <div class="workspace-preview-field-card">
              <span class="workspace-preview-field-label">最小 chunk</span>
              <el-input-number v-model="previewDraft.minChunkSizeChars" :min="50" :max="2000" :step="50" controls-position="right" />
            </div>
            <div class="workspace-preview-field-card">
              <span class="workspace-preview-field-label">语义段上限</span>
              <el-input-number v-model="previewDraft.semanticSectionMaxChars" :min="100" :max="5000" :step="100" controls-position="right" />
            </div>
            <div class="workspace-preview-field-card">
              <span class="workspace-preview-field-label">预览条数</span>
              <el-input-number v-model="previewDraft.previewChunkLimit" :min="1" :max="30" controls-position="right" />
            </div>
            <div class="workspace-preview-field-card">
              <span class="workspace-preview-field-label">分隔符</span>
              <el-switch v-model="previewDraft.keepSeparator" inline-prompt active-text="保留" inactive-text="移除" />
            </div>
          </div>
          <div class="workspace-preview-actions">
            <el-button class="workspace-btn workspace-btn--ghost" @click="resetPreviewDraft">
              恢复当前规则
            </el-button>
            <el-button type="primary" class="workspace-btn workspace-btn--primary" :loading="previewSplitLoading" @click="previewSplit">
              试算切分
            </el-button>
          </div>
        </section>

        <section class="workspace-preview-tabs">
          <el-segmented v-model="previewTab" :options="previewTabOptions" />
        </section>

        <section v-if="previewTab === 'content'">
          <pre class="workspace-preview-content space-scrollbar">{{ previewData.content || '暂无内容' }}</pre>
        </section>

        <section v-else class="workspace-result-list space-scrollbar">
          <article v-for="chunk in previewData.chunks" :key="chunk.index" class="chunk-card workspace-result-card">
            <div class="chunk-card-head workspace-result-card__head">
              <div class="chunk-card-title workspace-result-card__title">Chunk {{ chunk.index }}</div>
              <div class="chunk-card-meta workspace-result-card__meta">{{ chunk.length }} 字</div>
            </div>
            <pre class="workspace-result-card__content workspace-result-card__content--scroll space-scrollbar">{{ chunk.content || '暂无内容' }}</pre>
          </article>
          <div v-if="!previewData.chunks.length" class="chunk-empty-state workspace-empty-panel">
            还没有切分结果，先确认文件是否读取成功。
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

const totalFileCountDisplay = computed(() => formatCount(tableData.total))
const currentLibDisplay = computed(() => {
  const currentLibName = String(tableData.rows[0]?.libName || '').trim()
  return currentLibName || String(searchData.libId || '').trim() || '--'
})
const enabledFileCountDisplay = computed(() => {
  return formatCount(tableData.rows.filter((row: { status?: number }) => Number(row.status) === 1).length)
})
const disabledFileCountDisplay = computed(() => {
  return formatCount(tableData.rows.filter((row: { status?: number }) => Number(row.status) !== 1).length)
})
const currentPageRecallDisplay = computed(() => {
  const total = tableData.rows.reduce((sum: number, row: { recallCount?: number | string }) => sum + Number(row.recallCount || 0), 0)
  return formatCount(total)
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

function getStatusTagClass(status: number) {
  return Number(status) === 1 ? 'workspace-inline-tag--success' : 'workspace-inline-tag--warning'
}

function formatCount(value: number | string) {
  const num = Number(value || 0)
  if (!Number.isFinite(num)) {
    return '--'
  }
  return num.toLocaleString('zh-CN')
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
.uploadfile-manage-page {
  gap: 16px;
}

.preview-dialog-tip {
  margin-bottom: 12px;
}

.preview-summary-panel {
  padding: 18px;
}

.chunk-card {
  border-radius: 18px;
}

.chunk-card-head {
  gap: 12px;
}

.chunk-card-title {
  line-height: 1.5;
}

.chunk-card-meta {
  line-height: 1.5;
}

.chunk-empty-state {
  line-height: 1.7;
}

.preview-draft-grid {
  margin-top: 14px;
}

:deep(.op-col .cell) {
  overflow: visible;
  white-space: normal;
}
</style>
