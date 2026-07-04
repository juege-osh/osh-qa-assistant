<template>
  <div class="page-shell add-file-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <el-button text class="workspace-btn workspace-btn--text" @click="$router.back()">返回文档管理</el-button>
        <span class="workspace-context-note">上传完成后会自动回到当前知识库的文档列表，便于继续查看切分和索引状态。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">知识库 {{ currentLibDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">支持格式 {{ supportedFormatCount }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">单文件 ≤ 15MB</span>
      </div>
    </section>

    <section class="workspace-section-card add-file-overview-panel workspace-dashboard-panel">
      <div class="workspace-overview-head workspace-dashboard-head">
        <div>
          <div class="panel-title">上传工作区</div>
          <div class="panel-desc workspace-panel-desc">优先上传结构清晰、信息完整的资料。上传完成后，再回到文档管理页确认切分是否自然、是否需要重建索引。</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--active">目标知识库 {{ currentLibDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">支持多种办公文档</span>
        </div>
      </div>
      <div class="workspace-tip-grid">
        <article v-for="tip in uploadTips" :key="tip.title" class="workspace-tip-card">
          <div class="workspace-tip-card__title">{{ tip.title }}</div>
          <div class="workspace-tip-card__desc">{{ tip.desc }}</div>
        </article>
      </div>
    </section>

    <section class="workspace-section-card add-file-focus-panel">
      <div class="workspace-overview-head">
        <div>
          <div class="panel-title panel-title--md">本次重点</div>
          <div class="workspace-panel-desc">把上传动作翻译成更直接的处理顺序，方便决定先准备资料、先验证切分，还是先回看索引状态。</div>
        </div>
      </div>
      <div class="workspace-tip-grid add-file-tip-grid">
        <article
          v-for="item in uploadFocusCards"
          :key="item.title"
          :class="['workspace-tip-card', 'add-file-focus-card', `add-file-focus-card--${item.tone}`]"
        >
          <div class="add-file-focus-card__head">
            <span :class="['add-file-focus-card__dot', `add-file-focus-card__dot--${item.tone}`]"></span>
            <div class="workspace-tip-card__title">{{ item.title }}</div>
          </div>
          <div class="workspace-tip-card__desc">{{ item.desc }}</div>
        </article>
      </div>
    </section>

    <section class="workspace-section-card add-file-panel">
      <div class="workspace-task-panel-head">
        <div>
          <div class="panel-title">上传文件</div>
          <div class="dialog-intro workspace-task-panel-intro">
            推荐先上传高质量、结构清晰的资料，再到文档管理页查看切分和索引状态。
          </div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--active">目标知识库 {{ currentLibDisplay }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">上传后可立即回看</span>
        </div>
      </div>
      <section class="workspace-dialog-tip-panel add-file-tip-panel">
        上传只是第一步。更稳妥的流程通常是：先放入高质量原文，再回到文档管理页预览 chunk，最后按需要重建索引并开始问答验证。
      </section>
      <el-form ref="addForm" :model="formData" :rules="rules">
        <el-form-item prop="storePath" class="workspace-upload-drop">
          <FileUpload :drag="true" listType="text"
            @file-list-change="handleFileListChange" ref="fileUploadRef">
            <template v-slot:trigger>
              <el-icon class="add-file-upload-icon">
                <UploadFilled />
              </el-icon>
              <span class="el-upload__text">
                拖拽文件到此处，或<em>点击上传</em>
              </span>
            </template>
            <template #default>
              支持 TXT、MARKDOWN、PDF、HTML、XLSX、XLS、PROPERTIES、DOC、DOCX、CSV、PPTX、XML、PPT、MD、HTM。
            </template>
          </FileUpload>
        </el-form-item>
        <div class="workspace-dialog-footer add-file-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="$router.back()">取消</el-button>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="onSubmit()">提交文件</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>
<script setup name='AddFile' lang='ts'>
import { computed, onMounted, reactive, ref } from 'vue';
import FileUpload from '@/components/FileUpload.vue';
import type { AnyObjsDefine } from '@/types/common';
import { addUploadFileApi } from '@/api/workspace/uploadFileApi';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { UploadFilled } from '@element-plus/icons-vue';
let formData = reactive({
  libId: '',
  storePath: '',
  originalFileName: '',
})
let rules = reactive({
  storePath: [{ required: true, message: "请上传文件", trigger: "blur" }],
})
let addForm = ref()
let route = useRoute()
let router = useRouter()
let fileUploadRef = ref()
const currentLibDisplay = computed(() => String(formData.libId || '').trim() || '--')
const supportedFormatCount = '15+'
const uploadTips = [
  {
    title: '优先上传最终版本',
    desc: '避免把草稿、重复资料和无效附件一起放进知识库，减少后续召回噪音。'
  },
  {
    title: '先看切分再重建',
    desc: '上传后先去文档管理页预览 chunk 是否自然，满意后再执行重建索引。'
  },
  {
    title: '控制单文件质量',
    desc: '结构越清晰、章节越完整，后续问答越稳定，也更容易定位效果问题。'
  }
]
const uploadFocusCards = [
  {
    title: '优先上传结构清晰的正式资料',
    desc: '最终版、章节清楚、标题明确的文件更容易得到稳定切分，也更适合后续做真实问答。',
    tone: 'success'
  },
  {
    title: '上传后建议先回看 chunk 预览',
    desc: '如果段落被切得过碎、过粗或语义断裂，先调规则再重建索引，通常比直接问答更高效。',
    tone: 'warning'
  },
  {
    title: '完成索引后再进入聊天和检索调试',
    desc: '这样更容易分清问题究竟出在原文质量、切分策略，还是提示词与展示逻辑。',
    tone: 'success'
  }
] as const

function onSubmit() {
  if (!formData.libId) {
    ElMessage.warning('请先从知识库列表进入文件管理，再添加文件')
    return
  }
  addForm.value.validate((valid: boolean) => {
    if (!valid) return
    addUploadFileApi(formData).then(result => {
      ElMessage({ "message": result.msg, "type": "success" })
      fileUploadRef.value.doClearFileList()
      addForm.value.resetFields()
      router.back()
    })
  })
}
// 处理文件列表变更
function handleFileListChange(uploadResultList: AnyObjsDefine) {
  if (uploadResultList && uploadResultList.length > 0) {
    formData.storePath = uploadResultList[0].relativePath
    formData.originalFileName = uploadResultList[0].originalFilename
  } else {
    formData.storePath = ''
    formData.originalFileName = ''
  }
  addForm.value?.validateField('storePath');
}
// 处理请求入参
function handleLibId() {
  const libIdFromQs = route.query.libId
  if(libIdFromQs) {
    formData.libId = libIdFromQs as string
  } else {
    ElMessage.warning('缺少知识库信息，请先从知识库列表进入文件管理')
    router.replace('/workspace/knowledgeLib/manage')
  }
}
onMounted(() => {
  handleLibId()
})
</script>
<style scoped>
.add-file-page {
  gap: 16px;
}

.add-file-focus-panel {
  padding: 18px 20px;
}

.add-file-panel {
  padding: 22px 24px 20px;
}

.add-file-tip-grid {
  margin-top: 14px;
}

.add-file-focus-card {
  position: relative;
  overflow: hidden;
}

.add-file-focus-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.42);
}

.add-file-focus-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.add-file-focus-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.add-file-focus-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.add-file-focus-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex-shrink: 0;
  background: rgba(148, 163, 184, 0.9);
}

.add-file-focus-card__dot--success {
  background: #12b76a;
  box-shadow: 0 0 0 4px rgba(18, 183, 106, 0.12);
}

.add-file-focus-card__dot--warning {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.14);
}

.add-file-upload-icon {
  margin-right: 8px;
  font-size: 20px;
  color: var(--space-primary);
}

.add-file-footer {
  margin-top: 18px;
}

.add-file-tip-panel {
  margin-bottom: 18px;
}

@media (max-width: 900px) {
  .workspace-task-panel-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
