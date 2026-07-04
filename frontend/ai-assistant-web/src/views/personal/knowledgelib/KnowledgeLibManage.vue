<template>
  <div class="page-shell knowledge-manage-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">我的知识库</span>
        <span class="workspace-context-note">统一查看资料规模、应用挂载和检索准备情况，优先判断下一步是补资料还是直接做命中验证。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">知识库 {{ currentResultCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">已挂应用 {{ boundLibCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">文档 {{ totalDocCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">字符量 {{ totalCharCountDisplay }}</span>
      </div>
    </section>

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">我的知识库</div>
        <div class="toolbar-title">知识库列表</div>
        <div class="toolbar-desc">
          先按名称缩小范围，再回看文档规模、应用挂载和检索调试入口。
        </div>
      </div>
      <div class="toolbar-actions workspace-toolbar-actions">
        <el-form :model="searchData" :inline="true" class="workspace-toolbar-form">
          <el-form-item class="workspace-toolbar-field workspace-toolbar-field--md">
            <el-input type="text" placeholder="按知识库名称搜索" v-model="searchData.libName" clearable></el-input>
          </el-form-item>
          <el-form-item>
            <el-button @click="loadTable" type="primary" class="workspace-btn workspace-btn--primary">查询</el-button>
          </el-form-item>
          <el-form-item>
            <el-button @click="addDialogVisible = true" type="primary" :icon="Plus" class="workspace-btn workspace-btn--primary">新增知识库</el-button>
          </el-form-item>
          <el-form-item>
            <el-button class="workspace-btn workspace-btn--ghost" @click="openRecallDebug">检索调试</el-button>
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
                <div class="workspace-entity-media knowledge-lib-media">
                  <el-avatar v-if="row.iconPath" :src="toAddressable(row.iconPath)">
                  </el-avatar>
                  <el-icon v-else color="#635bff" class="space-icon--xl">
                    <Folder />
                  </el-icon>
                </div>
                <div class="workspace-entity-copy">
                  <div class="workspace-entity-name">{{ row.libName }}</div>
                  <div class="workspace-entity-meta">
                    <span>{{ row.docCount }} 个文档</span>
                    <span>{{ row.charCount }} 个字符</span>
                  </div>
                </div>
              </div>
              <div class="workspace-inline-tags">
                <span class="workspace-inline-tag workspace-inline-tag--soft">ID {{ row.id }}</span>
                <span :class="['workspace-inline-tag', row.appId ? 'workspace-inline-tag--success' : 'workspace-inline-tag--warning']">
                  {{ row.appId ? '已挂应用' : '未挂应用' }}
                </span>
              </div>
              <div class="workspace-resource-card__section">
                <div class="workspace-resource-card__row">
                  <span class="workspace-resource-card__label">所属应用</span>
                  <span class="workspace-resource-card__value">{{ row.appName || '暂未绑定应用' }}</span>
                </div>
                <div class="workspace-resource-card__row">
                  <span class="workspace-resource-card__label">文档规模</span>
                  <span class="workspace-resource-card__value">{{ row.docCount }} 个文档 / {{ row.charCount }} 个字符</span>
                </div>
              </div>
              <div class="workspace-resource-card__desc">
                {{ row.libDesc || '暂无描述，建议补充知识范围、适用场景和内容边界。' }}
              </div>
            </div>
            <template #footer>
              <div class="workspace-resource-card__footer">
                <el-button @click="openUpdateDialog(row.id)" class="workspace-btn workspace-btn--ghost">编辑</el-button>
                <el-button @click="$router.push('/workspace/uploadFile/manage?libId=' + row.id)"
                  type="primary" class="workspace-btn workspace-btn--primary">文档管理</el-button>
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
    <AddKnowledgeLib :addDialogVisible="addDialogVisible" @closeDialog="addDialogVisible = false"
      @addSuccess="handleAddSuccess">
    </AddKnowledgeLib>
    <!-- 更新对话框 -->
    <UpdateKnowledgeLib :updateDialogVisible="updateDialogVisible" @closeDialog="updateDialogVisible = false"
      :idToUpdate="idToUpdate" @updateSuccess="handleUpdateSuccess"></UpdateKnowledgeLib>
    <el-dialog v-model="recallDebugVisible" title="知识库检索调试" width="1280px" class="workspace-form-dialog recall-debug-dialog">
      <div class="dialog-intro">
        用一条真实问题检查当前知识库的命中方向、重排效果和后续处理动作，避免只看分数却看不清实际结论。
      </div>
      <section class="workspace-preview-shell recall-debug-shell">
        <section class="workspace-info-card workspace-info-card--flush recall-debug-toolbar">
          <div class="recall-toolbar-copy workspace-section-copy">
            <div class="recall-toolbar-title workspace-section-title">先用一条真实问题做命中检查</div>
            <div class="recall-toolbar-desc workspace-section-desc">看清楚是没召回、召回错了，还是召回对了但组织方式还不够好。</div>
          </div>
          <section class="workspace-dialog-tip-panel recall-toolbar-note">
            尽量直接输入最近真实问过、但回答不稳定的问题。这样更容易判断应该补知识、补切分，还是补提示词与展示。
          </section>
          <div class="workspace-preview-grid workspace-preview-grid--3 recall-toolbar-grid">
            <div class="workspace-preview-field-card">
              <span class="workspace-preview-field-label">知识库</span>
              <el-select v-model="recallDebugForm.libId" placeholder="请选择知识库" class="recall-select recall-select--lib">
                <el-option v-for="row in tableData.rows" :key="row.id" :label="row.libName" :value="row.id" />
              </el-select>
            </div>
            <div class="workspace-preview-field-card">
              <span class="workspace-preview-field-label">TopK</span>
              <el-select v-model="recallDebugForm.topK" class="recall-select recall-select--topk">
                <el-option v-for="count in [3, 5, 8, 10]" :key="count" :label="count" :value="count" />
              </el-select>
            </div>
            <div class="workspace-preview-field-card">
              <span class="workspace-preview-field-label">当前目标</span>
              <div class="recall-target-copy">
                <strong>{{ selectedRecallLibName }}</strong>
                <span>调试结果会围绕当前选中的知识库展开，适合先聚焦一套资料边界。</span>
              </div>
            </div>
            <div class="workspace-preview-field-card recall-query-card">
              <span class="workspace-preview-field-label">调试问题</span>
              <el-input
                class="recall-query-input"
                v-model="recallDebugForm.query"
                type="textarea"
                :rows="3"
                placeholder="输入一个真实用户问题，检查当前知识库召回到了哪些 chunk"
              />
            </div>
          </div>
          <div class="workspace-action-row workspace-action-row--end recall-debug-actions">
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

        <section class="workspace-section-card recall-focus-panel" v-if="recallDebugResult.query">
          <div class="workspace-overview-head">
            <div>
              <div class="workspace-section-title workspace-section-title--md">当前实验重点</div>
              <div class="workspace-panel-desc">先判断这次实验是在看命中、看重排，还是在为后续任务整理依据，避免只盯局部分数而忽略整体结论。</div>
            </div>
          </div>
          <div class="workspace-tip-grid recall-tip-grid">
            <article
              v-for="item in recallFocusCards"
              :key="item.title"
              :class="['workspace-tip-card', 'recall-tip-card', `recall-tip-card--${item.tone}`]"
            >
              <div class="recall-tip-card__head">
                <span :class="['recall-tip-card__dot', `recall-tip-card__dot--${item.tone}`]"></span>
                <div class="workspace-tip-card__title">{{ item.title }}</div>
              </div>
              <div class="workspace-tip-card__desc">{{ item.desc }}</div>
            </article>
          </div>
        </section>

        <section class="workspace-info-card workspace-info-card--flush recall-summary" v-if="recallDebugResult.query">
          <div class="workspace-info-grid recall-summary-grid">
            <div class="recall-summary-card workspace-info-item">
              <span class="recall-summary-label workspace-info-label">切分策略</span>
              <strong class="recall-summary-value workspace-info-value">{{ formatSplitStrategy(recallDebugResult.splitStrategy) }}</strong>
            </div>
            <div class="recall-summary-card workspace-info-item">
              <span class="recall-summary-label workspace-info-label">原始召回</span>
              <strong class="recall-summary-value workspace-info-value">{{ recallDebugResult.rawHitCount }}</strong>
            </div>
            <div class="recall-summary-card workspace-info-item">
              <span class="recall-summary-label workspace-info-label">重排后</span>
              <strong class="recall-summary-value workspace-info-value">{{ recallDebugResult.rerankHitCount }}</strong>
            </div>
            <div class="recall-summary-card workspace-info-item">
              <span class="recall-summary-label workspace-info-label">TopK</span>
              <strong class="recall-summary-value workspace-info-value">{{ recallDebugResult.topK }}</strong>
            </div>
          </div>
        </section>

        <section class="workspace-info-card workspace-info-card--flush recall-config-panel" v-if="recallDebugResult.query">
          <div class="recall-judgement-copy workspace-section-copy">
            <div class="recall-judgement-title workspace-section-title">当前参数快照</div>
            <div class="recall-judgement-desc workspace-section-desc">
              保存快照时会把这些参数一起记下来，方便回看。
            </div>
          </div>
          <div class="recall-config-grid workspace-spec-grid workspace-spec-grid--4">
            <div class="recall-config-card workspace-spec-card">
              <span class="recall-config-label workspace-spec-label">chunk 大小</span>
              <strong class="recall-config-value workspace-spec-value">{{ recallDebugResult.splitConfig?.chunkSize ?? '-' }}</strong>
            </div>
            <div class="recall-config-card workspace-spec-card">
              <span class="recall-config-label workspace-spec-label">最小 chunk</span>
              <strong class="recall-config-value workspace-spec-value">{{ recallDebugResult.splitConfig?.minChunkSizeChars ?? '-' }}</strong>
            </div>
            <div class="recall-config-card workspace-spec-card">
              <span class="recall-config-label workspace-spec-label">最小保留长度</span>
              <strong class="recall-config-value workspace-spec-value">{{ recallDebugResult.splitConfig?.minChunkLengthToEmbed ?? '-' }}</strong>
            </div>
            <div class="recall-config-card workspace-spec-card">
              <span class="recall-config-label workspace-spec-label">语义段上限</span>
              <strong class="recall-config-value workspace-spec-value">{{ recallDebugResult.splitConfig?.semanticSectionMaxChars ?? '-' }}</strong>
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

        <section class="workspace-info-card workspace-info-card--flush recall-judgement" v-if="recallDebugResult.query">
          <div class="recall-judgement-copy workspace-section-copy">
            <div class="recall-judgement-title workspace-section-title">当前问题判断</div>
            <div class="recall-judgement-desc workspace-section-desc">
              先判断这是没召回、召回错了，还是召回对了但答案组织得不好，再决定后续进入补知识、补切分、补提示词还是补展示。
            </div>
          </div>
          <div class="workspace-info-grid recall-judgement-grid">
            <div class="recall-judgement-card workspace-info-item">
              <span class="recall-judgement-label workspace-info-label">建议归类</span>
              <strong class="recall-judgement-value workspace-info-value">{{ activeFollowUpLabel }}</strong>
            </div>
            <div class="recall-judgement-card workspace-info-item">
              <span class="recall-judgement-label workspace-info-label">诊断结论</span>
              <strong class="recall-judgement-value recall-judgement-value--small workspace-info-value">{{ recallDiagnosis.title }}</strong>
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

        <div v-else class="workspace-empty-panel recall-empty-state">
          先选择知识库并输入一个真实用户问题，再开始调试。建议优先使用最近在应用里问过、但回答不稳定的真实问题。
        </div>

        <section class="workspace-info-card workspace-info-card--flush recall-snapshot-section" v-if="recallSnapshots.length">
          <div class="recall-judgement-copy workspace-section-copy">
            <div class="recall-judgement-title workspace-section-title">实验快照</div>
            <div class="recall-judgement-desc workspace-section-desc">
              选两条快照就能直接对比。
            </div>
          </div>
          <div class="workspace-action-row workspace-action-row--end snapshot-actions">
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
          <div class="workspace-selection-list">
            <label
              v-for="snapshot in recallSnapshots"
              :key="snapshot.id"
              :class="[
                'workspace-selection-card',
                selectedSnapshotIds.includes(snapshot.id) ? 'workspace-selection-card--selected' : ''
              ]"
            >
              <input
                class="workspace-selection-check"
                type="checkbox"
                :checked="selectedSnapshotIds.includes(snapshot.id)"
                @change="toggleSnapshotSelection(snapshot.id)"
              >
              <div class="workspace-selection-card__main">
                <div class="workspace-selection-card__head">
                  <strong class="workspace-selection-card__title">{{ snapshot.versionName || snapshot.query }}</strong>
                  <span>{{ snapshot.savedAt }}</span>
                </div>
                <div class="workspace-selection-card__desc">{{ snapshot.query }}</div>
                <div class="workspace-selection-card__meta">
                  <span class="workspace-inline-tag workspace-inline-tag--soft">{{ snapshot.libName }}</span>
                  <span class="workspace-inline-tag workspace-inline-tag--soft">{{ formatSplitStrategy(snapshot.splitStrategy) }}</span>
                  <span class="workspace-inline-tag workspace-inline-tag--soft">TopK {{ snapshot.topK }}</span>
                  <span class="workspace-inline-tag workspace-inline-tag--soft">原始 {{ snapshot.rawHitCount }}</span>
                  <span class="workspace-inline-tag workspace-inline-tag--soft">重排 {{ snapshot.rerankHitCount }}</span>
                  <span class="workspace-inline-tag workspace-inline-tag--soft">{{ snapshot.categoryLabel }}</span>
                  <span v-if="snapshot.recommended" class="workspace-inline-tag workspace-inline-tag--success snapshot-recommended">推荐版本</span>
                  <span v-if="snapshot.active" class="workspace-inline-tag workspace-inline-tag--active snapshot-active">当前生效版本</span>
                </div>
                <div v-if="snapshot.noteText" class="workspace-selection-card__desc">备注：{{ snapshot.noteText }}</div>
                <div v-if="snapshot.recommendReason" class="workspace-selection-card__desc">推荐理由：{{ snapshot.recommendReason }}</div>
                <div class="workspace-selection-card__actions">
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

        <section class="workspace-inspect-grid recall-results-grid" v-if="recallDebugResult.query">
          <article class="workspace-section-card workspace-inspect-panel">
            <div class="workspace-overview-head workspace-inspect-head recall-panel-head">
              <div class="recall-panel-copy">
                <div class="recall-panel-title workspace-section-title">原始召回结果</div>
                <div class="recall-panel-desc workspace-panel-desc">先看知识库有没有拿到可用候选，判断是完全未命中，还是命中方向还不够准。</div>
              </div>
              <div class="workspace-inline-tags recall-panel-metrics">
                <span class="workspace-inline-tag workspace-inline-tag--soft">命中 {{ recallDebugResult.rawHitCount }}</span>
                <span class="workspace-inline-tag workspace-inline-tag--soft">TopK {{ recallDebugResult.topK }}</span>
              </div>
            </div>
            <div class="recall-top-summary" :class="{ 'recall-top-summary--empty': !recallDebugResult.rawHits.length }">
              <div class="recall-top-summary__label">当前 Top1</div>
              <div class="recall-top-summary__title">{{ rawTopDisplay }}</div>
            </div>
            <div v-if="recallDebugResult.rawHits.length" class="workspace-inspect-list space-scrollbar">
              <div v-for="item in recallDebugResult.rawHits" :key="`raw-${item.documentId}-${item.index}`" class="recall-hit-card workspace-result-card">
                <div class="recall-hit-head workspace-result-card__head">
                  <div class="recall-hit-copy">
                    <div class="recall-hit-title workspace-result-card__title">#{{ item.index }} {{ item.fileName }}</div>
                    <div class="recall-hit-id workspace-result-card__submeta">DocID: {{ item.documentId }}</div>
                  </div>
                  <div class="recall-hit-score workspace-result-card__meta">{{ formatScore(item.score) }}</div>
                </div>
                <pre class="workspace-result-card__content workspace-result-card__content--scroll space-scrollbar">{{ item.content }}</pre>
              </div>
            </div>
            <div v-else class="recall-empty workspace-empty-panel">当前没有召回到任何内容。</div>
          </article>

          <article class="workspace-section-card workspace-inspect-panel">
            <div class="workspace-overview-head workspace-inspect-head recall-panel-head">
              <div class="recall-panel-copy">
                <div class="recall-panel-title workspace-section-title">重排后结果</div>
                <div class="recall-panel-desc workspace-panel-desc">再看重排是否把更相关的内容推到了前面，判断当前切分和组织方式是否已经有效。</div>
              </div>
              <div class="workspace-inline-tags recall-panel-metrics">
                <span class="workspace-inline-tag workspace-inline-tag--active">保留 {{ recallRetentionRate }}</span>
                <span class="workspace-inline-tag workspace-inline-tag--soft">命中 {{ recallDebugResult.rerankHitCount }}</span>
              </div>
            </div>
            <div class="recall-top-summary" :class="{ 'recall-top-summary--empty': !recallDebugResult.rerankHits.length }">
              <div class="recall-top-summary__label">当前 Top1</div>
              <div class="recall-top-summary__title">{{ rerankTopDisplay }}</div>
            </div>
            <div v-if="recallDebugResult.rerankHits.length" class="workspace-inspect-list space-scrollbar">
              <div v-for="item in recallDebugResult.rerankHits" :key="`rerank-${item.documentId}-${item.index}`" class="recall-hit-card workspace-result-card">
                <div class="recall-hit-head workspace-result-card__head">
                  <div class="recall-hit-copy">
                    <div class="recall-hit-title workspace-result-card__title">#{{ item.index }} {{ item.fileName }}</div>
                    <div class="recall-hit-id workspace-result-card__submeta">DocID: {{ item.documentId }}</div>
                  </div>
                  <div class="recall-hit-score workspace-result-card__meta">{{ formatScore(item.score) }}</div>
                </div>
                <pre class="workspace-result-card__content workspace-result-card__content--scroll space-scrollbar">{{ item.content }}</pre>
              </div>
            </div>
            <div v-else class="recall-empty workspace-empty-panel">当前没有通过重排阈值的结果。</div>
          </article>
        </section>
      </section>
    </el-dialog>
  </div>
</template>
<script setup name='KnowledgeLibManage' lang='ts'>
import { ref, reactive, onMounted, computed, watch } from 'vue'
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
import { useRoute, useRouter } from 'vue-router';

// 添加对话框是否显示
let addDialogVisible = ref(false)
let updateDialogVisible = ref(false)
let idToUpdate = ref('')
let recallDebugVisible = ref(false)
let recallDebugLoading = ref(false)
const route = useRoute()
const router = useRouter()
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
const currentRows = computed(() => tableData.rows || [])
const totalLibCountDisplay = computed(() => tableData.total || 0)
const currentResultCountDisplay = computed(() => currentRows.value.length)
const boundLibCountDisplay = computed(() => currentRows.value.filter((row: { appId?: string | number | null }) => Boolean(row.appId)).length)
const totalDocCountDisplay = computed(() => formatCount(currentRows.value.reduce((sum: number, row: { docCount?: number | string }) => sum + Number(row.docCount || 0), 0)))
const totalCharCountDisplay = computed(() => formatCount(currentRows.value.reduce((sum: number, row: { charCount?: number | string }) => sum + Number(row.charCount || 0), 0)))
const rawTopDisplay = computed(() => {
  const rawTop = recallDebugResult.rawHits[0]
  return rawTop?.fileName || '未命中'
})
const rerankTopDisplay = computed(() => {
  const rerankTop = recallDebugResult.rerankHits[0]
  return rerankTop?.fileName || '未命中'
})
const recallRetentionRate = computed(() => {
  const rawCount = Number(recallDebugResult.rawHitCount || 0)
  const rerankCount = Number(recallDebugResult.rerankHitCount || 0)
  if (!rawCount) {
    return '0%'
  }
  return `${((rerankCount / rawCount) * 100).toFixed(0)}%`
})
const selectedRecallLibName = computed(() => {
  const target = tableData.rows.find((row: { id?: string | number; libName?: string }) => String(row.id) === String(recallDebugForm.libId))
  return target?.libName || '未选择知识库'
})
const recallFocusCards = computed(() => {
  const rawCount = Number(recallDebugResult.rawHitCount || 0)
  const rerankCount = Number(recallDebugResult.rerankHitCount || 0)
  return [
    {
      title: rawCount > 0 ? `原始召回到 ${rawCount} 条候选` : '当前没有召回到候选内容',
      desc: rawCount > 0
        ? `本次问题来自「${selectedRecallLibName.value}」，下一步重点看命中的方向对不对，而不只是看有没有召回。`
        : '建议先检查问题关键词、知识范围和资料是否足够覆盖当前问法，再决定补知识还是调整提问方式。',
      tone: rawCount > 0 ? 'success' : 'danger'
    },
    {
      title: rerankCount > 0 ? `重排后保留 ${recallRetentionRate.value}` : '当前没有可用的重排结果',
      desc: rerankCount > 0
        ? `Top1 当前是「${rerankTopDisplay.value}」，可以继续判断它是不是最接近真实答案的片段。`
        : '如果原始召回有结果但重排后为空，优先检查切分粒度、资料完整性和相似度质量。',
      tone: rerankCount > 0 ? 'warning' : rawCount > 0 ? 'warning' : 'danger'
    },
    {
      title: `建议归类到「${activeFollowUpLabel.value}」`,
      desc: `${recallDiagnosis.value.title}。${recallSnapshots.value.length ? `当前已保存 ${recallSnapshots.value.length} 条实验快照，可以继续做版本对比。` : '如果这次实验有代表性，建议直接保存一条实验快照，方便后续回看和对比。'}`,
      tone: selectedFollowUpCategory.value === 'knowledge' || selectedFollowUpCategory.value === 'observe' ? 'danger' : selectedFollowUpCategory.value === 'chunking' || selectedFollowUpCategory.value === 'prompt' ? 'warning' : 'success'
    }
  ] as const
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

function formatCount(value: number | string) {
  const num = Number(value || 0)
  if (!Number.isFinite(num)) {
    return '--'
  }
  return num.toLocaleString('zh-CN')
}
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

function openRecallDebugFromRoute() {
  const targetLibId = String(route.query.debugLibId || '').trim()
  if (!targetLibId || !tableData.rows.length) {
    return
  }
  const targetLib = tableData.rows.find((row: any) => String(row.id) === targetLibId)
  if (!targetLib) {
    return
  }
  recallDebugForm.libId = targetLib.id
  recallDebugVisible.value = true
  loadRecallSnapshots()
  const nextQuery = { ...route.query }
  delete nextQuery.debugLibId
  router.replace({
    path: route.path,
    query: nextQuery
  })
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

watch(
  () => tableData.rows,
  () => {
    openRecallDebugFromRoute()
  }
)

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
.knowledge-manage-page {
  gap: 16px;
}

.knowledge-summary-panel {
  padding: 18px 20px;
}

.card-list-panel {
  padding: 18px 20px;
}

.card-wrapper {
  margin-bottom: 14px;
}

.recall-tip-grid {
  margin-top: 14px;
}

.recall-tip-card {
  position: relative;
  overflow: hidden;
}

.recall-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.recall-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.recall-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.recall-tip-card--danger::before {
  background: linear-gradient(180deg, #ef4444 0%, #dc2626 100%);
}

.recall-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.recall-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex-shrink: 0;
  background: rgba(148, 163, 184, 0.9);
}

.recall-tip-card__dot--success {
  background: #12b76a;
  box-shadow: 0 0 0 4px rgba(18, 183, 106, 0.12);
}

.recall-tip-card__dot--warning {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.14);
}

.recall-tip-card__dot--danger {
  background: #ef4444;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.12);
}

.recall-debug-toolbar {
  padding: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(249, 251, 255, 0.95) 100%);
}

.recall-debug-dialog :deep(.el-dialog) {
  max-width: calc(100vw - 48px);
}

.recall-toolbar-copy {
  margin-bottom: 12px;
}

.recall-toolbar-note {
  margin-bottom: 14px;
}

.recall-select--lib {
  width: 100%;
}

.recall-select--topk {
  width: 100%;
}

.recall-toolbar-grid {
  margin-top: 0;
}

.recall-query-card {
  grid-column: 1 / -1;
}

.recall-target-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.65;
}

.recall-target-copy strong {
  color: var(--space-text);
  font-size: 14px;
  font-weight: 700;
}

.recall-query-input :deep(.el-textarea__inner) {
  min-height: 112px;
}

.recall-debug-actions {
  margin-top: 14px;
}

.recall-summary {
  padding: 16px 18px;
}

.recall-focus-panel {
  padding: 18px 20px;
}

.recall-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.recall-summary-card {
  min-height: 102px;
}

.recall-summary-value {
  display: block;
  font-size: 22px;
  line-height: 1.1;
  color: var(--space-primary-strong);
}

.recall-config-panel {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(249, 251, 255, 0.95) 100%);
}

.recall-config-grid {
  margin-top: 14px;
}

.recall-config-card {
  min-height: 96px;
}

.recall-note-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.recall-snapshot-section {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(249, 251, 255, 0.95) 100%);
}

.snapshot-actions {
  margin-top: 14px;
}

.snapshot-recommended {
  color: #0f766e;
  font-weight: 700;
}

.snapshot-active {
  color: var(--space-primary-strong);
  font-weight: 700;
}

.recall-judgement {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(249, 251, 255, 0.95) 100%);
}

.recall-judgement-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.recall-judgement-card {
  min-height: 98px;
}

.recall-judgement-value {
  display: block;
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

.recall-panel-head {
  align-items: stretch;
  gap: 14px;
}

.recall-panel-copy {
  flex: 1;
  min-width: 0;
}

.recall-panel-metrics {
  flex: 0 0 168px;
  max-width: 220px;
  justify-content: flex-end;
  align-content: flex-start;
}

.recall-top-summary {
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid rgba(223, 230, 241, 0.92);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.98), rgba(255, 255, 255, 0.94));
}

.recall-top-summary--empty {
  background: rgba(248, 251, 255, 0.9);
}

.recall-top-summary__label {
  color: var(--space-text-soft);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.recall-top-summary__title {
  margin-top: 6px;
  color: var(--space-text);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.65;
  word-break: break-word;
}

.recall-hit-card {
  border-radius: 16px;
}

.recall-hit-head {
  align-items: flex-start;
  gap: 12px;
}

.recall-hit-copy {
  flex: 1;
  min-width: 0;
}

.recall-hit-title {
  line-height: 1.5;
  word-break: break-word;
}

.recall-hit-id {
  line-height: 1.5;
  word-break: break-all;
}

.recall-hit-score {
  color: var(--space-primary-strong);
  flex-shrink: 0;
  font-weight: 700;
}

.recall-empty {
  line-height: 1.7;
}

.recall-empty-state {
  margin-top: 0;
}

@media (max-width: 1100px) {
  .recall-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .recall-panel-metrics {
    flex: 1 1 auto;
    max-width: none;
    justify-content: flex-start;
  }

  .recall-select--lib,
  .recall-select--topk {
    width: 100%;
  }

  .recall-query-card {
    grid-column: span 2;
  }

  .recall-judgement-grid {
    grid-template-columns: 1fr;
  }

  .recall-results-grid {
    grid-template-columns: 1fr;
  }

  .recall-config-grid,
  .recall-note-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .recall-query-card {
    grid-column: span 1;
  }
}
</style>
