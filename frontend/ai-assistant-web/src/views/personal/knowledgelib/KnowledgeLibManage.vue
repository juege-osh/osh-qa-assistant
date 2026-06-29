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
          <el-form-item>
            <el-button class="workspace-btn workspace-btn--ghost" @click="openRecallDebug">检索调试</el-button>
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
                <el-button @click="$router.push('/workspace/uploadFile/manage?libId=' + row.id)"
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
    <el-dialog v-model="recallDebugVisible" title="知识库检索调试" width="1080px" class="recall-debug-dialog">
      <section class="recall-debug-shell">
        <section class="recall-debug-toolbar">
          <el-form :model="recallDebugForm" :inline="true">
            <el-form-item label="知识库">
              <el-select v-model="recallDebugForm.libId" placeholder="请选择知识库" style="width: 220px">
                <el-option v-for="row in tableData.rows" :key="row.id" :label="row.libName" :value="row.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="TopK">
              <el-select v-model="recallDebugForm.topK" style="width: 100px">
                <el-option v-for="count in [3, 5, 8, 10]" :key="count" :label="count" :value="count" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-input
            v-model="recallDebugForm.query"
            type="textarea"
            :rows="3"
            placeholder="输入一个真实用户问题，检查当前知识库召回到了哪些 chunk"
          />
          <div class="recall-debug-actions">
            <el-button class="workspace-btn workspace-btn--ghost" @click="recallDebugForm.query = ''">清空</el-button>
            <el-button type="primary" class="workspace-btn workspace-btn--primary" :loading="recallDebugLoading" @click="runRecallDebug">
              开始调试
            </el-button>
          </div>
        </section>

        <section class="recall-summary" v-if="recallDebugResult.query">
          <div class="recall-summary-card">
            <span class="recall-summary-label">切分策略</span>
            <strong class="recall-summary-value">{{ formatSplitStrategy(recallDebugResult.splitStrategy) }}</strong>
          </div>
          <div class="recall-summary-card">
            <span class="recall-summary-label">原始召回</span>
            <strong class="recall-summary-value">{{ recallDebugResult.rawHitCount }}</strong>
          </div>
          <div class="recall-summary-card">
            <span class="recall-summary-label">重排后</span>
            <strong class="recall-summary-value">{{ recallDebugResult.rerankHitCount }}</strong>
          </div>
          <div class="recall-summary-card">
            <span class="recall-summary-label">TopK</span>
            <strong class="recall-summary-value">{{ recallDebugResult.topK }}</strong>
          </div>
        </section>

        <section class="recall-grid" v-if="recallDebugResult.query">
          <article class="recall-panel">
            <div class="recall-panel-title">原始召回结果</div>
            <div v-if="recallDebugResult.rawHits.length" class="recall-hit-list">
              <div v-for="item in recallDebugResult.rawHits" :key="`raw-${item.documentId}-${item.index}`" class="recall-hit-card">
                <div class="recall-hit-head">
                  <div>
                    <div class="recall-hit-title">#{{ item.index }} {{ item.fileName }}</div>
                    <div class="recall-hit-id">DocID: {{ item.documentId }}</div>
                  </div>
                  <div class="recall-hit-score">{{ formatScore(item.score) }}</div>
                </div>
                <pre class="recall-hit-content">{{ item.content }}</pre>
              </div>
            </div>
            <div v-else class="recall-empty">当前没有召回到任何内容。</div>
          </article>

          <article class="recall-panel">
            <div class="recall-panel-title">重排后结果</div>
            <div v-if="recallDebugResult.rerankHits.length" class="recall-hit-list">
              <div v-for="item in recallDebugResult.rerankHits" :key="`rerank-${item.documentId}-${item.index}`" class="recall-hit-card">
                <div class="recall-hit-head">
                  <div>
                    <div class="recall-hit-title">#{{ item.index }} {{ item.fileName }}</div>
                    <div class="recall-hit-id">DocID: {{ item.documentId }}</div>
                  </div>
                  <div class="recall-hit-score">{{ formatScore(item.score) }}</div>
                </div>
                <pre class="recall-hit-content">{{ item.content }}</pre>
              </div>
            </div>
            <div v-else class="recall-empty">当前没有通过重排阈值的结果。</div>
          </article>
        </section>
      </section>
    </el-dialog>
  </div>
</template>
<script setup name='KnowledgeLibManage' lang='ts'>
import { ref, reactive, onMounted } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageKnowledgeLibApi, deleteKnowledgeLibByIdApi, debugKnowledgeLibRecallApi } from '@/api/workspace/knowledgeLibApi';
import AddKnowledgeLib from '@/views/personal/knowledgelib/AddKnowledgeLib.vue';
import UpdateKnowledgeLib from '@/views/personal/knowledgelib/UpdateKnowledgeLib.vue';
import { Plus, Folder } from '@element-plus/icons-vue';
import { useResource } from '@/hooks/useResource';
import { ElMessage } from 'element-plus';

// 添加对话框是否显示
let addDialogVisible = ref(false)
let updateDialogVisible = ref(false)
let idToUpdate = ref('')
let recallDebugVisible = ref(false)
let recallDebugLoading = ref(false)
const recallDebugForm = reactive({
  libId: '',
  query: '',
  topK: 5
})
const recallDebugResult = reactive({
  query: '',
  topK: 5,
  rawHitCount: 0,
  rerankHitCount: 0,
  splitStrategy: '',
  rawHits: [] as Array<{ index: number; fileName: string; documentId: string; score?: number; content: string }>,
  rerankHits: [] as Array<{ index: number; fileName: string; documentId: string; score?: number; content: string }>
})

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

function openRecallDebug() {
  if (!tableData.rows.length) {
    ElMessage.warning('请先创建知识库后再进行检索调试')
    return
  }
  if (!recallDebugForm.libId) {
    recallDebugForm.libId = tableData.rows[0].id
  }
  recallDebugVisible.value = true
}

function runRecallDebug() {
  if (!recallDebugForm.libId) {
    ElMessage.warning('请先选择知识库')
    return
  }
  if (!String(recallDebugForm.query || '').trim()) {
    ElMessage.warning('请输入一个要调试的真实问题')
    return
  }
  recallDebugLoading.value = true
  debugKnowledgeLibRecallApi({
    libId: recallDebugForm.libId,
    query: recallDebugForm.query,
    topK: recallDebugForm.topK
  }).then(result => {
    Object.assign(recallDebugResult, result.data || {})
  }).finally(() => {
    recallDebugLoading.value = false
  })
}

function formatSplitStrategy(strategy: string) {
  if (strategy === 'semantic') {
    return '语义优先'
  }
  if (strategy === 'token') {
    return '纯 Token'
  }
  return strategy || '-'
}

function formatScore(score?: number) {
  if (score === undefined || score === null) {
    return '-'
  }
  return score.toFixed(3)
}

onMounted(() => {
  loadTable()
})
</script>
<style scoped>
.left {
  background: rgba(64, 158, 255, 0.06);
  border: 1px solid var(--space-border);
  padding: 3px;
  margin-right: 10px;
  border-radius: 6px;
  width: 52px;
  height: 52px;
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
  font-weight: 700;
  color: var(--space-text);
  font-size: 14px;
}
.middle-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.middle-row-item {
  padding-right: .5rem;
  font-size: 12px;
}

:deep(.el-card__body) {
  padding: 10px;
}

:deep(.el-card__footer) {
  padding: 10px;
}

.card-wrapper {
  margin-bottom: 14px;
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
  line-height: 1.8em;
  min-height: 5em;
  color: var(--space-muted) !important;
  font-size: 13px;
}

.recall-debug-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recall-debug-toolbar {
  padding: 18px;
  border: 1px solid rgba(64, 158, 255, 0.14);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
}

.recall-debug-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.recall-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.recall-summary-card {
  padding: 14px 16px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.98), rgba(239, 246, 255, 0.98));
}

.recall-summary-label {
  display: block;
  font-size: 12px;
  color: var(--space-text-soft);
}

.recall-summary-value {
  display: block;
  margin-top: 8px;
  font-size: 22px;
  line-height: 1.1;
  color: var(--space-primary-strong);
}

.recall-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.recall-panel {
  padding: 18px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.98);
}

.recall-panel-title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 700;
  color: var(--space-text);
}

.recall-hit-list {
  display: grid;
  gap: 12px;
  max-height: 52vh;
  overflow: auto;
  padding-right: 4px;
}

.recall-hit-card {
  border: 1px solid rgba(64, 158, 255, 0.1);
  border-radius: 16px;
  overflow: hidden;
  background: rgba(248, 250, 252, 0.92);
}

.recall-hit-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(64, 158, 255, 0.08);
}

.recall-hit-title {
  color: var(--space-text);
  font-size: 14px;
  font-weight: 700;
}

.recall-hit-id {
  margin-top: 4px;
  color: var(--space-text-soft);
  font-size: 12px;
}

.recall-hit-score {
  color: var(--space-primary-strong);
  font-size: 14px;
  font-weight: 700;
}

.recall-hit-content {
  margin: 0;
  padding: 16px;
  max-height: 220px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--space-text);
  font-size: 13px;
  line-height: 1.7;
}

.recall-empty {
  padding: 28px 18px;
  border: 1px dashed rgba(148, 163, 184, 0.45);
  border-radius: 18px;
  text-align: center;
  color: var(--space-text-soft);
  background: rgba(248, 250, 252, 0.88);
}

@media (max-width: 1100px) {
  .recall-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .recall-grid {
    grid-template-columns: 1fr;
  }
}
</style>
