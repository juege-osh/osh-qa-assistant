<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">知识库</div>
      <div class="hero-subtitle">
        把同一类资料放进一个知识库，后续绑定到应用就能使用。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">知识库总数：{{ tableData.total || 0 }}</span>
        <span class="hero-badge">可直接进入文档管理</span>
        <span class="hero-badge">可做检索调试</span>
      </div>
    </section>

    <section class="toolbar-panel glass-panel">
      <div class="toolbar-copy">
        <div class="toolbar-title">知识库列表</div>
        <div class="toolbar-desc">
          先看文档数量和挂载应用，需要时直接进文档管理。
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
                <div class="left workspace-media">
                  <el-avatar v-if="row.iconPath" :src="toAddressable(row.iconPath)">
                  </el-avatar>
                  <el-icon v-else color="#409eff" class="space-icon--xl">
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
            <el-button class="workspace-btn workspace-btn--ghost" :disabled="!recallDebugResult.query" @click="saveRecallSnapshot">
              保存实验快照
            </el-button>
            <el-button class="workspace-btn workspace-btn--ghost" :disabled="!recallDebugResult.query" @click="copyRecallTaskDraft">
              复制任务草稿
            </el-button>
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

        <section class="recall-config-panel" v-if="recallDebugResult.query">
          <div class="recall-judgement-copy">
            <div class="recall-judgement-title">当前参数快照</div>
            <div class="recall-judgement-desc">
              保存快照时会把这些参数一起记下来，方便回看。
            </div>
          </div>
          <div class="recall-config-grid">
            <div class="recall-config-card">
              <span class="recall-config-label">chunk 大小</span>
              <strong class="recall-config-value">{{ recallDebugResult.splitConfig?.chunkSize ?? '-' }}</strong>
            </div>
            <div class="recall-config-card">
              <span class="recall-config-label">最小 chunk</span>
              <strong class="recall-config-value">{{ recallDebugResult.splitConfig?.minChunkSizeChars ?? '-' }}</strong>
            </div>
            <div class="recall-config-card">
              <span class="recall-config-label">最小保留长度</span>
              <strong class="recall-config-value">{{ recallDebugResult.splitConfig?.minChunkLengthToEmbed ?? '-' }}</strong>
            </div>
            <div class="recall-config-card">
              <span class="recall-config-label">语义段上限</span>
              <strong class="recall-config-value">{{ recallDebugResult.splitConfig?.semanticSectionMaxChars ?? '-' }}</strong>
            </div>
          </div>
          <div class="recall-note-grid">
            <el-input
              v-model="experimentDraft.noteText"
              type="textarea"
              :rows="2"
              placeholder="给这次实验写一个备注，例如：关键词更贴近实际问法 / 标题切分更稳定"
            />
            <el-input
              v-model="experimentDraft.recommendReason"
              type="textarea"
              :rows="2"
              placeholder="如果这版更好，可以提前写推荐理由，例如：Top1 命中更稳定、片段更完整"
            />
          </div>
        </section>

        <section class="recall-judgement" v-if="recallDebugResult.query">
          <div class="recall-judgement-copy">
            <div class="recall-judgement-title">当前问题判断</div>
            <div class="recall-judgement-desc">
              先判断这是没召回、召回错了，还是召回对了但答案组织得不好，再决定后续进入补知识、补切分、补提示词还是补展示。
            </div>
          </div>
          <div class="recall-judgement-grid">
            <div class="recall-judgement-card">
              <span class="recall-judgement-label">建议归类</span>
              <strong class="recall-judgement-value">{{ activeFollowUpLabel }}</strong>
            </div>
            <div class="recall-judgement-card">
              <span class="recall-judgement-label">诊断结论</span>
              <strong class="recall-judgement-value recall-judgement-value--small">{{ recallDiagnosis.title }}</strong>
            </div>
          </div>
          <div class="recall-judgement-tags">
            <el-button
              v-for="option in followUpCategoryOptions"
              :key="option.value"
              class="workspace-btn"
              :class="selectedFollowUpCategory === option.value ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
              @click="selectedFollowUpCategory = option.value"
            >
              {{ option.label }}
            </el-button>
          </div>
          <div class="recall-judgement-note">
            {{ recallDiagnosis.description }}
          </div>
        </section>

        <section class="recall-snapshot-section" v-if="recallSnapshots.length">
          <div class="recall-judgement-copy">
            <div class="recall-judgement-title">实验快照</div>
            <div class="recall-judgement-desc">
              选两条快照就能直接对比。
            </div>
          </div>
          <div class="snapshot-actions">
            <el-button class="workspace-btn workspace-btn--ghost" :disabled="selectedSnapshotIds.length !== 2" @click="copySnapshotCompareDraft">
              复制对比报告
            </el-button>
            <el-button class="workspace-btn workspace-btn--ghost" :disabled="selectedSnapshotIds.length !== 1" @click="markRecommendedSnapshot">
              标记推荐版本
            </el-button>
            <el-button class="workspace-btn workspace-btn--ghost" :disabled="selectedSnapshotIds.length !== 1" @click="publishSnapshot">
              发布为生效版本
            </el-button>
            <el-button class="workspace-btn workspace-btn--ghost" :disabled="!recallSnapshots.length" @click="copySnapshotExperimentDraft">
              复制实验记录
            </el-button>
            <el-button class="workspace-btn workspace-btn--ghost" @click="clearRecallSnapshots">
              清空快照
            </el-button>
          </div>
          <div class="snapshot-list">
            <label v-for="snapshot in recallSnapshots" :key="snapshot.id" class="snapshot-card">
              <input
                type="checkbox"
                :checked="selectedSnapshotIds.includes(snapshot.id)"
                @change="toggleSnapshotSelection(snapshot.id)"
              >
              <div class="snapshot-card-main">
                <div class="snapshot-card-head">
                  <strong>{{ snapshot.versionName || snapshot.query }}</strong>
                  <span>{{ snapshot.savedAt }}</span>
                </div>
                <div class="snapshot-card-subtitle">{{ snapshot.query }}</div>
                <div class="snapshot-card-meta">
                  <span>{{ snapshot.libName }}</span>
                  <span>{{ formatSplitStrategy(snapshot.splitStrategy) }}</span>
                  <span>TopK {{ snapshot.topK }}</span>
                  <span>原始 {{ snapshot.rawHitCount }}</span>
                  <span>重排 {{ snapshot.rerankHitCount }}</span>
                  <span>{{ snapshot.categoryLabel }}</span>
                  <span v-if="snapshot.recommended" class="snapshot-recommended">推荐版本</span>
                  <span v-if="snapshot.active" class="snapshot-active">当前生效版本</span>
                </div>
                <div v-if="snapshot.noteText" class="snapshot-card-subtitle">备注：{{ snapshot.noteText }}</div>
                <div v-if="snapshot.recommendReason" class="snapshot-card-subtitle">推荐理由：{{ snapshot.recommendReason }}</div>
                <div class="snapshot-card-actions">
                  <el-button text class="workspace-btn workspace-btn--text" @click.prevent="renameSnapshot(snapshot.id)">
                    命名版本
                  </el-button>
                  <el-button text class="workspace-btn workspace-btn--text" @click.prevent="removeSnapshot(snapshot.id)">
                    删除
                  </el-button>
                </div>
              </div>
            </label>
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useTable } from '@/hooks/useTable';
import {
  pageKnowledgeLibApi,
  deleteKnowledgeLibByIdApi,
  debugKnowledgeLibRecallApi,
  saveKnowledgeLibExperimentApi,
  listKnowledgeLibExperimentApi,
  renameKnowledgeLibExperimentApi,
  recommendKnowledgeLibExperimentApi,
  publishKnowledgeLibExperimentApi,
  deleteKnowledgeLibExperimentApi
} from '@/api/workspace/knowledgeLibApi';
import AddKnowledgeLib from '@/views/personal/knowledgelib/AddKnowledgeLib.vue';
import UpdateKnowledgeLib from '@/views/personal/knowledgelib/UpdateKnowledgeLib.vue';
import { Plus, Folder } from '@element-plus/icons-vue';
import { useResource } from '@/hooks/useResource';
import { ElMessage, ElMessageBox } from 'element-plus';
import { writeClipboardText } from '@/util/clipboard';

// 添加对话框是否显示
let addDialogVisible = ref(false)
let updateDialogVisible = ref(false)
let idToUpdate = ref('')
let recallDebugVisible = ref(false)
let recallDebugLoading = ref(false)
const followUpCategoryOptions = [
  { value: 'knowledge', label: '补知识' },
  { value: 'chunking', label: '补切分' },
  { value: 'prompt', label: '补提示词' },
  { value: 'ui', label: '补展示' },
  { value: 'observe', label: '补观测' }
] as const
type FollowUpCategory = typeof followUpCategoryOptions[number]['value']

const recallDebugForm = reactive({
  libId: '',
  query: '',
  topK: 5
})
const selectedFollowUpCategory = ref<FollowUpCategory>('knowledge')
type RecallSnapshot = {
  id: string
  savedAt: string
  libId: string
  libName: string
  versionName: string
  query: string
  topK: number
  splitStrategy: string
  splitConfigJson: string
  rawHitCount: number
  rerankHitCount: number
  diagnosisTitle: string
  category: FollowUpCategory
  categoryLabel: string
  rawTopSummary: string
  rerankTopSummary: string
  noteText: string
  recommendReason: string
  recommended: boolean
  active: boolean
}
const recallDebugResult = reactive({
  query: '',
  topK: 5,
  rawHitCount: 0,
  rerankHitCount: 0,
  splitStrategy: '',
  splitConfig: null as null | Record<string, any>,
  rawHits: [] as Array<{ index: number; fileName: string; documentId: string; score?: number; content: string }>,
  rerankHits: [] as Array<{ index: number; fileName: string; documentId: string; score?: number; content: string }>
})
const experimentDraft = reactive({
  noteText: '',
  recommendReason: ''
})
const recallSnapshots = ref<RecallSnapshot[]>([])
const selectedSnapshotIds = ref<string[]>([])
const recallDiagnosis = computed(() => {
  const rawHitCount = Number(recallDebugResult.rawHitCount || 0)
  const rerankHitCount = Number(recallDebugResult.rerankHitCount || 0)
  const firstRerank = recallDebugResult.rerankHits[0]
  if (rawHitCount === 0) {
    return {
      title: '完全未召回',
      description: '当前问题在原始召回阶段就没有命中任何内容，更像是知识缺失、关键词不一致，或者知识源本身还没有入库。',
      suggestedCategory: 'knowledge' as FollowUpCategory
    }
  }
  if (rawHitCount > 0 && rerankHitCount === 0) {
    return {
      title: '召回到弱相关内容',
      description: '原始召回拿到了候选，但都没通过重排阈值。优先检查切分边界、关键词表达和文档标题结构，再决定是否补知识。',
      suggestedCategory: 'chunking' as FollowUpCategory
    }
  }
  if (firstRerank && Number(firstRerank.score || 0) < 0.45) {
    return {
      title: '召回命中较弱',
      description: '当前虽然有重排结果，但头部结果分数偏低，说明问题和知识片段的贴合度不高，建议优先优化切分与知识组织。',
      suggestedCategory: 'chunking' as FollowUpCategory
    }
  }
  return {
    title: '已召回到较相关内容',
    description: '当前问题已经能召回到相对相关的 chunk。如果线上回答仍然不好，下一步更可能是提示词组织、答案展示或链路观测的问题。',
    suggestedCategory: 'prompt' as FollowUpCategory
  }
})
const activeFollowUpLabel = computed(() => {
  const active = followUpCategoryOptions.find((option) => option.value === selectedFollowUpCategory.value)
  return active?.label || '-'
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
  loadRecallSnapshots()
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
  const previousQuery = recallDebugResult.query
  debugKnowledgeLibRecallApi({
    libId: recallDebugForm.libId,
    query: recallDebugForm.query,
    topK: recallDebugForm.topK
  }).then(result => {
    Object.assign(recallDebugResult, result.data || {})
    if (!result.data?.query || result.data.query !== previousQuery) {
      experimentDraft.noteText = ''
      experimentDraft.recommendReason = ''
    }
    selectedFollowUpCategory.value = recallDiagnosis.value.suggestedCategory
  }).finally(() => {
    recallDebugLoading.value = false
  })
}

async function copyRecallTaskDraft() {
  if (!recallDebugResult.query) {
    ElMessage.warning('请先完成一次检索调试')
    return
  }
  const draft = buildRecallTaskDraft()
  try {
    await writeClipboardText(draft)
    ElMessage.success('已复制检索调试任务草稿')
  } catch {
    ElMessage.error('复制失败，请稍后重试')
  }
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

function buildRecallTaskDraft() {
  const selectedLib = tableData.rows.find((row: any) => String(row.id) === String(recallDebugForm.libId))
  const rerankTop = recallDebugResult.rerankHits[0]
  const rawTop = recallDebugResult.rawHits[0]
  const lines = [
    '# RAG 检索调试后续任务草稿',
    '',
    `- 生成时间：${new Date().toLocaleString()}`,
    `- 知识库：${selectedLib?.libName || recallDebugForm.libId}`,
    `- 调试问题：${recallDebugResult.query}`,
    `- 当前切分策略：${formatSplitStrategy(recallDebugResult.splitStrategy)}`,
    `- 原始召回数：${recallDebugResult.rawHitCount}`,
    `- 重排后召回数：${recallDebugResult.rerankHitCount}`,
    `- 建议归类：${activeFollowUpLabel.value}`,
    `- 诊断结论：${recallDiagnosis.value.title}`,
    '',
    '## 问题判断',
    '',
    recallDiagnosis.value.description,
    '',
    '## 当前命中情况',
    '',
    `- 原始召回 Top1：${rawTop ? `${rawTop.fileName} / 分数 ${formatScore(rawTop.score)}` : '未命中'}`,
    `- 重排后 Top1：${rerankTop ? `${rerankTop.fileName} / 分数 ${formatScore(rerankTop.score)}` : '未命中'}`,
    `- Top1 片段摘要：${rerankTop?.content || rawTop?.content || '暂无可用片段'}`,
    '',
    '## 建议动作',
    '',
    `- 优先从“${activeFollowUpLabel.value}”方向进入任务池。`,
    '- 如果是补知识：补充缺失制度、术语别名、业务流程原文。',
    '- 如果是补切分：优先检查标题边界、段落聚合是否过粗或过碎。',
    '- 如果是补提示词：检查答案结构是否先结论后依据，是否清楚说明不确定性。',
    '- 如果是补展示：检查引用片段、失败反馈、结果可读性是否足够清楚。',
    '- 如果是补观测：把这个问题加入回归样例，后续对比不同切分版本和提示词版本。'
  ]
  return lines.join('\n')
}

function saveRecallSnapshot() {
  if (!recallDebugResult.query) {
    ElMessage.warning('请先完成一次检索调试')
    return
  }
  const selectedLib = tableData.rows.find((row: any) => String(row.id) === String(recallDebugForm.libId))
  const rawTop = recallDebugResult.rawHits[0]
  const rerankTop = recallDebugResult.rerankHits[0]
  const snapshot: RecallSnapshot = {
    id: '',
    savedAt: '',
    libId: String(recallDebugForm.libId),
    libName: selectedLib?.libName || String(recallDebugForm.libId),
    versionName: '',
    query: recallDebugResult.query,
    topK: Number(recallDebugResult.topK || 5),
    splitStrategy: recallDebugResult.splitStrategy,
    splitConfigJson: JSON.stringify(recallDebugResult.splitConfig || {}),
    rawHitCount: Number(recallDebugResult.rawHitCount || 0),
    rerankHitCount: Number(recallDebugResult.rerankHitCount || 0),
    diagnosisTitle: recallDiagnosis.value.title,
    category: selectedFollowUpCategory.value,
    categoryLabel: activeFollowUpLabel.value,
    rawTopSummary: rawTop ? `${rawTop.fileName} / ${formatScore(rawTop.score)}` : '未命中',
    rerankTopSummary: rerankTop ? `${rerankTop.fileName} / ${formatScore(rerankTop.score)}` : '未命中',
    noteText: experimentDraft.noteText.trim(),
    recommendReason: experimentDraft.recommendReason.trim(),
    recommended: false,
    active: false
  }
  saveKnowledgeLibExperimentApi({
    libId: snapshot.libId,
    versionName: snapshot.versionName,
    queryText: snapshot.query,
    topK: snapshot.topK,
    splitStrategy: snapshot.splitStrategy,
    splitConfigJson: snapshot.splitConfigJson,
    rawHitCount: snapshot.rawHitCount,
    rerankHitCount: snapshot.rerankHitCount,
    diagnosisTitle: snapshot.diagnosisTitle,
    categoryCode: snapshot.category,
    categoryLabel: snapshot.categoryLabel,
    rawTopSummary: snapshot.rawTopSummary,
    rerankTopSummary: snapshot.rerankTopSummary,
    noteText: snapshot.noteText,
    recommendReason: snapshot.recommendReason,
    recommended: snapshot.recommended ? 1 : 0
  }).then((result) => {
    ElMessage.success(result.msg || '已保存实验快照')
    loadRecallSnapshots()
  })
}

function clearRecallSnapshots() {
  if (!recallSnapshots.value.length) {
    ElMessage.success('当前没有需要清空的实验快照')
    return
  }
  Promise.all(recallSnapshots.value.map((snapshot) => deleteKnowledgeLibExperimentApi(snapshot.id)))
    .then(() => {
      selectedSnapshotIds.value = []
      recallSnapshots.value = []
      ElMessage.success('已清空实验快照')
    })
}

function toggleSnapshotSelection(snapshotId: string) {
  if (selectedSnapshotIds.value.includes(snapshotId)) {
    selectedSnapshotIds.value = selectedSnapshotIds.value.filter((id) => id !== snapshotId)
    return
  }
  if (selectedSnapshotIds.value.length >= 2) {
    selectedSnapshotIds.value = [...selectedSnapshotIds.value.slice(1), snapshotId]
    return
  }
  selectedSnapshotIds.value = [...selectedSnapshotIds.value, snapshotId]
}

async function copySnapshotCompareDraft() {
  if (selectedSnapshotIds.value.length !== 2) {
    ElMessage.warning('请先勾选两条实验快照')
    return
  }
  const selectedSnapshots = recallSnapshots.value.filter((item) => selectedSnapshotIds.value.includes(item.id))
  const [snapshotA, snapshotB] = selectedSnapshots
  const draft = [
    '# RAG 切分 / 检索实验对比草稿',
    '',
    `- 生成时间：${new Date().toLocaleString()}`,
    '',
    '## 实验 A',
    '',
    `- 保存时间：${snapshotA.savedAt}`,
    `- 知识库：${snapshotA.libName}`,
    `- 问题：${snapshotA.query}`,
    `- 切分策略：${formatSplitStrategy(snapshotA.splitStrategy)}`,
    `- 原始召回：${snapshotA.rawHitCount}`,
    `- 重排后：${snapshotA.rerankHitCount}`,
    `- 建议归类：${snapshotA.categoryLabel}`,
    `- 诊断结论：${snapshotA.diagnosisTitle}`,
    `- 原始 Top1：${snapshotA.rawTopSummary}`,
    `- 重排 Top1：${snapshotA.rerankTopSummary}`,
    '',
    '## 实验 B',
    '',
    `- 保存时间：${snapshotB.savedAt}`,
    `- 知识库：${snapshotB.libName}`,
    `- 问题：${snapshotB.query}`,
    `- 切分策略：${formatSplitStrategy(snapshotB.splitStrategy)}`,
    `- 原始召回：${snapshotB.rawHitCount}`,
    `- 重排后：${snapshotB.rerankHitCount}`,
    `- 建议归类：${snapshotB.categoryLabel}`,
    `- 诊断结论：${snapshotB.diagnosisTitle}`,
    `- 原始 Top1：${snapshotB.rawTopSummary}`,
    `- 重排 Top1：${snapshotB.rerankTopSummary}`,
    '',
    '## 对比判断',
    '',
    '- 哪一版更容易在 Top1 / Top3 召回正确知识：',
    '- 哪一版的重排后结果更稳定：',
    '- 哪一版更适合作为当前知识库默认切分：',
    '- 需要继续补知识、补切分还是补提示词：'
  ].join('\n')

  try {
    await writeClipboardText(draft)
    ElMessage.success('已复制实验对比草稿')
  } catch {
    ElMessage.error('复制失败，请稍后重试')
  }
}

function removeSnapshot(snapshotId: string) {
  deleteKnowledgeLibExperimentApi(snapshotId).then((result) => {
    selectedSnapshotIds.value = selectedSnapshotIds.value.filter((id) => id !== snapshotId)
    ElMessage.success(result.msg || '已删除实验快照')
    loadRecallSnapshots()
  })
}

async function renameSnapshot(snapshotId: string) {
  const snapshot = recallSnapshots.value.find((item) => item.id === snapshotId)
  if (!snapshot) {
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
      '给当前实验快照起一个版本名，例如：IG 切分 V1 / 语义切分 V2 / Prompt+Chunk A 版',
      '命名实验版本',
      {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        inputValue: snapshot.versionName || '',
        inputPattern: /^.{0,40}$/,
        inputErrorMessage: '版本名最多 40 个字符'
      }
    )
    renameKnowledgeLibExperimentApi({
      id: snapshotId,
      versionName: String(value || '').trim()
    }).then((result) => {
      ElMessage.success(result.msg || '已更新版本名称')
      loadRecallSnapshots()
    })
  } catch {
    // 用户取消不提示
  }
}

function markRecommendedSnapshot() {
  if (selectedSnapshotIds.value.length !== 1) {
    ElMessage.warning('请先勾选一条实验快照')
    return
  }
  const targetId = selectedSnapshotIds.value[0]
  const targetSnapshot = recallSnapshots.value.find((item) => item.id === targetId)
  if (!targetSnapshot) {
    ElMessage.warning('未找到对应的实验快照，请刷新后重试')
    return
  }
  recommendKnowledgeLibExperimentApi({
    libId: recallDebugForm.libId,
    id: targetId,
    recommendReason: targetSnapshot?.recommendReason || experimentDraft.recommendReason.trim()
  }).then((result) => {
    ElMessage.success(result.msg || '已标记推荐版本')
    loadRecallSnapshots()
  })
}

function publishSnapshot() {
  if (selectedSnapshotIds.value.length !== 1) {
    ElMessage.warning('请先勾选一条实验快照')
    return
  }
  publishKnowledgeLibExperimentApi({
    libId: recallDebugForm.libId,
    id: selectedSnapshotIds.value[0]
  }).then((result) => {
    ElMessage.success(result.msg || '已发布为当前生效切分版本')
    loadRecallSnapshots()
    loadTable()
  })
}

async function copySnapshotExperimentDraft() {
  if (!recallSnapshots.value.length) {
    ElMessage.warning('当前没有可导出的实验快照')
    return
  }
  const draft = [
    '# RAG 切分实验记录',
    '',
    `- 生成时间：${new Date().toLocaleString()}`,
    `- 快照数量：${recallSnapshots.value.length}`,
    '',
    '## 实验版本列表',
    '',
    ...recallSnapshots.value.flatMap((snapshot, index) => [
      `### 实验 ${index + 1}${snapshot.recommended ? '（推荐）' : ''}`,
      `- 版本名称：${snapshot.versionName || '未命名版本'}`,
      `- 保存时间：${snapshot.savedAt}`,
      `- 知识库：${snapshot.libName}`,
      `- 调试问题：${snapshot.query}`,
      `- TopK：${snapshot.topK}`,
      `- 切分策略：${formatSplitStrategy(snapshot.splitStrategy)}`,
      `- 切分参数：${snapshot.splitConfigJson || '{}'}`,
      `- 原始召回：${snapshot.rawHitCount}`,
      `- 重排后：${snapshot.rerankHitCount}`,
      `- 建议归类：${snapshot.categoryLabel}`,
      `- 诊断结论：${snapshot.diagnosisTitle}`,
      `- 原始 Top1：${snapshot.rawTopSummary}`,
      `- 重排 Top1：${snapshot.rerankTopSummary}`,
      `- 实验备注：${snapshot.noteText || '无'}`,
      `- 推荐理由：${snapshot.recommendReason || '无'}`,
      ''
    ]),
    '## 结论建议',
    '',
    '- 哪个版本适合作为当前知识库默认切分版本：',
    '- 是否需要继续补知识或补提示词：',
    '- 推荐保留的实验版本名称：'
  ].join('\n')

  try {
    await writeClipboardText(draft)
    ElMessage.success('已复制实验记录草稿')
  } catch {
    ElMessage.error('复制失败，请稍后重试')
  }
}

onMounted(() => {
  loadTable()
})

function loadRecallSnapshots() {
  if (!recallDebugForm.libId) {
    recallSnapshots.value = []
    return
  }
  listKnowledgeLibExperimentApi(String(recallDebugForm.libId)).then((result) => {
    const rows = (result.data || []).map((item: any) => ({
      id: String(item.id),
      savedAt: item.createdTime ? new Date(item.createdTime).toLocaleString() : '-',
      libId: String(item.libId),
      libName: tableData.rows.find((row: any) => String(row.id) === String(item.libId))?.libName || String(item.libId),
      versionName: item.versionName || '',
      query: item.queryText,
      topK: Number(item.topK || 5),
      splitStrategy: item.splitStrategy,
      splitConfigJson: item.splitConfigJson || '{}',
      rawHitCount: Number(item.rawHitCount || 0),
      rerankHitCount: Number(item.rerankHitCount || 0),
      diagnosisTitle: item.diagnosisTitle,
      category: item.categoryCode as FollowUpCategory,
      categoryLabel: item.categoryLabel,
      rawTopSummary: item.rawTopSummary || '未命中',
      rerankTopSummary: item.rerankTopSummary || '未命中',
      noteText: item.noteText || '',
      recommendReason: item.recommendReason || '',
      recommended: Number(item.recommended || 0) === 1,
      active: Number(item.active || 0) === 1
    }))
    recallSnapshots.value = rows
    selectedSnapshotIds.value = selectedSnapshotIds.value.filter((id) => rows.some((item: RecallSnapshot) => item.id === id))
  })
}
</script>
<style scoped>
.left {
  margin-right: 10px;
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

.recall-config-panel {
  padding: 18px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.98);
}

.recall-config-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.recall-config-card {
  padding: 14px 16px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.98), rgba(239, 246, 255, 0.98));
}

.recall-config-label {
  display: block;
  color: var(--space-text-soft);
  font-size: 12px;
}

.recall-config-value {
  display: block;
  margin-top: 8px;
  color: var(--space-primary-strong);
  font-size: 18px;
  line-height: 1.2;
}

.recall-note-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.recall-snapshot-section {
  padding: 18px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.98);
}

.snapshot-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.snapshot-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.snapshot-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid rgba(64, 158, 255, 0.1);
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.92);
  cursor: pointer;
}

.snapshot-card input {
  margin-top: 4px;
}

.snapshot-card-main {
  flex: 1;
  min-width: 0;
}

.snapshot-card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--space-text);
  font-size: 14px;
}

.snapshot-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  color: var(--space-text-soft);
  font-size: 12px;
}

.snapshot-card-subtitle {
  margin-top: 6px;
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.snapshot-card-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.snapshot-recommended {
  color: #0f766e;
  font-weight: 700;
}

.snapshot-active {
  color: #1d4ed8;
  font-weight: 700;
}

.recall-judgement {
  padding: 18px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.98);
}

.recall-judgement-title {
  color: var(--space-text);
  font-size: 15px;
  font-weight: 700;
}

.recall-judgement-desc {
  margin-top: 8px;
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.recall-judgement-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.recall-judgement-card {
  padding: 14px 16px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.98), rgba(239, 246, 255, 0.98));
}

.recall-judgement-label {
  display: block;
  color: var(--space-text-soft);
  font-size: 12px;
}

.recall-judgement-value {
  display: block;
  margin-top: 8px;
  color: var(--space-primary-strong);
  font-size: 20px;
  line-height: 1.2;
}

.recall-judgement-value--small {
  font-size: 17px;
}

.recall-judgement-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.recall-judgement-note {
  margin-top: 14px;
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.7;
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

  .recall-judgement-grid {
    grid-template-columns: 1fr;
  }

  .recall-config-grid,
  .recall-note-grid {
    grid-template-columns: 1fr;
  }

  .snapshot-card-head {
    flex-direction: column;
  }
}
</style>
