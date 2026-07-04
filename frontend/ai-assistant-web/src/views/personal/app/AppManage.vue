<template>
  <div class="page-shell app-manage-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">我的应用</span>
        <span class="workspace-context-note">统一查看应用绑定、模型配置和公开访问准备情况，优先判断下一步是补配置还是直接验证对话。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">结果 {{ currentResultCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">已绑定 {{ boundAppCountDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">待绑定 {{ unboundAppCountDisplay }}</span>
      </div>
    </section>

    <section class="workspace-section-card app-overview-panel workspace-dashboard-panel">
      <div class="app-overview-head workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="panel-title">应用工作区</div>
          <div class="panel-desc workspace-panel-desc">先确认哪些应用已经绑定知识库、哪些还停留在测试阶段，再决定是继续完善配置还是直接进入聊天验证。</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--active">当前结果 {{ currentResultCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">已绑定 {{ boundAppCountDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">待绑定 {{ unboundAppCountDisplay }}</span>
        </div>
      </div>
      <section class="stats-grid workspace-metrics-grid">
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--total">
          <div class="stat-label">当前结果</div>
          <div class="stat-value">{{ currentResultCountDisplay }}</div>
          <div class="stat-help">当前页内可直接继续编辑、绑定或开始聊天的应用数量。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--success">
          <div class="stat-label">已绑定知识库</div>
          <div class="stat-value workspace-stat-value--success">{{ boundAppCountDisplay }}</div>
          <div class="stat-help">已经接入知识内容，适合继续验证命中和回答效果。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--token">
          <div class="stat-label">待绑定应用</div>
          <div class="stat-value workspace-stat-value--warning">{{ unboundAppCountDisplay }}</div>
          <div class="stat-help">还没绑定知识库的应用，更适合先验证交互和基础链路。</div>
        </article>
        <article class="stat-card workspace-stat-card--framed workspace-stat-card--time">
          <div class="stat-label">自定义模型</div>
          <div class="stat-value">{{ customModelCountDisplay }}</div>
          <div class="stat-help">已经指定专用模型的应用，适合继续看效果差异和配置稳定性。</div>
        </article>
      </section>
    </section>

    <section class="workspace-section-card app-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="panel-title panel-title--md">当前关注点</div>
          <div class="workspace-panel-desc">把当前结果翻译成更直接的动作，方便决定先补绑定、继续验证对话，还是细化模型配置。</div>
        </div>
      </div>
      <div class="workspace-tip-grid app-tip-grid">
        <article
          v-for="item in appFocusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'app-tip-card', `app-tip-card--${item.tone}`]"
        >
          <div class="app-tip-card__head">
            <span :class="['app-tip-card__dot', `app-tip-card__dot--${item.tone}`]"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
        </article>
      </div>
    </section>

    <section class="toolbar-panel workspace-section-card workspace-toolbar-panel">
      <div class="toolbar-copy workspace-toolbar-copy">
        <div class="workspace-toolbar-kicker">我的应用</div>
        <div class="toolbar-title">应用列表</div>
        <div class="toolbar-desc">
          先按名称缩小范围，再回看知识库绑定、模型配置和公开访问准备情况。
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
            <el-button @click="addDialogVisible = true" type="primary" :icon="Plus" class="workspace-btn workspace-btn--primary">新增应用</el-button>
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
                <div class="workspace-entity-media workspace-entity-media--avatar">
                  <el-avatar :src="convertIconPath(row.iconPath)"></el-avatar>
                </div>
                <div class="workspace-entity-copy">
                  <div class="workspace-entity-name">{{ row.appName }}</div>
                  <div class="workspace-entity-meta">
                    <span>最后编辑于 {{ row.modifiedTime || '--' }}</span>
                  </div>
                </div>
              </div>
              <div class="workspace-inline-tags">
                <span class="workspace-inline-tag workspace-inline-tag--soft">ID {{ row.id }}</span>
                <span :class="['workspace-inline-tag', row.libId ? 'workspace-inline-tag--success' : 'workspace-inline-tag--warning']">
                  {{ row.libId ? '已绑定知识库' : '待绑定知识库' }}
                </span>
                <span class="workspace-inline-tag workspace-inline-tag--soft">{{ row.chatModel || '系统默认模型' }}</span>
              </div>
              <div class="workspace-resource-card__section">
                <div class="workspace-resource-card__row">
                  <span class="workspace-resource-card__label">知识库</span>
                  <span class="workspace-resource-card__value">{{ row.libName || '暂未绑定知识库' }}</span>
                </div>
                <div class="workspace-resource-card__actions-inline">
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
                <div class="workspace-resource-card__row">
                  <span class="workspace-resource-card__label">公开访问</span>
                  <span class="workspace-resource-card__value">可单独设置</span>
                </div>
              </div>
              <div class="workspace-resource-card__desc">
                {{ row.appDesc || '暂无描述，建议补充应用定位、适用问题和使用边界。' }}
              </div>
            </div>
            <template #footer>
              <div class="workspace-resource-card__footer">
                <el-button @click="openUpdateDialog(row.id)" class="workspace-btn workspace-btn--ghost">编辑</el-button>
                <el-button @click="openPublishDialog(row.id)" class="workspace-btn workspace-btn--ghost">公开发布</el-button>
                <el-button @click="startChat(row.id)" type="primary" class="workspace-btn workspace-btn--primary">开始聊天</el-button>
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageAppApi, deleteAppByIdApi, checkChatConditionApi, unBindLibApi } from '@/api/workspace/appApi';
import { getImage } from '@/util/AssetsImageUtil';
import { Plus } from '@element-plus/icons-vue';
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
const currentRows = computed(() => tableData.rows || [])
const totalAppCountDisplay = computed(() => tableData.total || 0)
const currentResultCountDisplay = computed(() => currentRows.value.length)
const boundAppCountDisplay = computed(() => currentRows.value.filter((row: { libId?: string | number | null }) => Boolean(row.libId)).length)
const unboundAppCountDisplay = computed(() => currentRows.value.filter((row: { libId?: string | number | null }) => !row.libId).length)
const customModelCountDisplay = computed(() => currentRows.value.filter((row: { chatModel?: string | null }) => Boolean(String(row.chatModel || '').trim())).length)
const appFocusCards = computed(() => [
  {
    title: boundAppCountDisplay.value === 0 ? '优先补齐知识库绑定关系' : '已绑定应用可以继续验证回答效果',
    desc: boundAppCountDisplay.value === 0
      ? '当前结果里的应用都还没有绑定知识库，更适合先明确内容来源，再进入公开访问或正式问答。'
      : `当前已有 ${boundAppCountDisplay.value} 个应用接入知识库，适合继续从开始聊天和检索调试里看真实回答表现。`,
    tone: boundAppCountDisplay.value === 0 ? 'warning' : 'success'
  },
  {
    title: unboundAppCountDisplay.value > 0 ? '未绑定应用适合先做轻量链路验证' : '当前应用配置完整度相对更高',
    desc: unboundAppCountDisplay.value > 0
      ? `当前还有 ${unboundAppCountDisplay.value} 个应用未绑定知识库，这一批更适合先确认交互、提示词和公开访问配置是否顺畅。`
      : '当前结果里的应用都已经接入知识内容，下一步更适合继续检查问答稳定性和命中质量。',
    tone: unboundAppCountDisplay.value > 0 ? 'warning' : 'success'
  },
  {
    title: customModelCountDisplay.value > 0 ? '可继续比较默认模型与自定义模型差异' : '仍可按业务场景细化模型策略',
    desc: customModelCountDisplay.value > 0
      ? `当前已有 ${customModelCountDisplay.value} 个应用指定了专用模型，适合继续验证不同模型下的成本、风格和回答稳定性。`
      : '当前应用还主要使用默认模型，如果不同业务场景回答差异明显，可以继续补专用模型策略。',
    tone: customModelCountDisplay.value > 0 ? 'success' : 'warning'
  }
] as const)
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
.app-manage-page {
  gap: 16px;
}

.app-summary-panel,
.app-focus-panel,
.card-list-panel {
  padding: 18px 20px;
}

.app-tip-grid {
  margin-top: 14px;
}

.app-tip-card {
  position: relative;
  overflow: hidden;
}

.app-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.app-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.app-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.app-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.78);
  flex-shrink: 0;
}

.app-tip-card__dot--success {
  background: #12b76a;
}

.app-tip-card__dot--warning {
  background: #f59e0b;
}

.card-wrapper {
  margin-bottom: 14px;
}

.bind-unbind-link {
  font-size: 12px;
}

@media (max-width: 900px) {
  .app-tip-grid {
    grid-template-columns: 1fr;
  }
}
</style>
