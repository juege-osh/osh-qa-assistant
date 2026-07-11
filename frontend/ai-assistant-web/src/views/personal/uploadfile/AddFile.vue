<template>
  <div class="page-shell add-file-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <el-button text class="workspace-btn workspace-btn--text" @click="$router.back()">返回文档管理</el-button>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">知识库 {{ currentLibDisplay }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">支持格式 {{ supportedFormatCount }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">单文件 ≤ 15MB</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">单次最多 {{ maxUploadFiles }} 个</span>
      </div>
    </section>

    <section class="workspace-section-card add-file-panel">
      <div class="workspace-task-panel-head">
        <div>
          <div class="panel-title">上传文件</div>
        </div>
        <div class="workspace-inline-tags">
          <span class="workspace-inline-tag workspace-inline-tag--active">目标知识库 {{ currentLibDisplay }}</span>
        </div>
      </div>
      <el-form ref="addForm" :model="formData" :rules="rules">
        <el-form-item prop="files" class="workspace-upload-drop">
          <FileUpload :drag="true" :multiple="true" :limit="maxUploadFiles" listType="text"
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
              支持 {{ supportedFormatLabel }}。
            </template>
          </FileUpload>
        </el-form-item>
        <div class="workspace-dialog-footer add-file-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="$router.back()">取消</el-button>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" :loading="submitting" @click="onSubmit()">提交文件</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>
<script setup name='AddFile' lang='ts'>
import { computed, onMounted, reactive, ref } from 'vue';
import FileUpload from '@/components/FileUpload.vue';
import type { AnyObjsDefine } from '@/types/common';
import { addUploadFilesBatchApi } from '@/api/workspace/uploadFileApi';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { UploadFilled } from '@element-plus/icons-vue';
const maxUploadFiles = 10
let formData = reactive({
  libId: '',
  files: [] as Array<{
    storePath: string
    originalFileName: string
  }>,
})
let rules = reactive({
  files: [{ required: true, message: "请上传至少一个文件", trigger: "blur" }],
})
let addForm = ref()
let route = useRoute()
let router = useRouter()
let fileUploadRef = ref()
const submitting = ref(false)
const supportedFormats = [
  'TXT',
  'MARKDOWN',
  'PDF',
  'HTML',
  'XLSX',
  'XLS',
  'PROPERTIES',
  'DOC',
  'DOCX',
  'CSV',
  'PPTX',
  'XML',
  'PPT',
  'MD',
  'HTM'
]
const supportedFormatCount = computed(() => supportedFormats.length)
const supportedFormatLabel = supportedFormats.join('、')
const currentLibDisplay = computed(() => String(formData.libId || '').trim() || '--')

async function onSubmit() {
  if (!formData.libId) {
    ElMessage.warning('请先从知识库列表进入文件管理，再添加文件')
    return
  }
  if (fileUploadRef.value?.hasPendingUploads()) {
    ElMessage.warning('文件仍在上传，请等待全部上传完成后再提交')
    return
  }
  if (fileUploadRef.value?.hasFailedUploads()) {
    ElMessage.warning('存在上传失败的文件，请移除或重新上传后再提交')
    return
  }
  const valid = await addForm.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  addUploadFilesBatchApi({
    files: formData.files.map(file => ({
      ...file,
      libId: formData.libId,
    })),
  }).then(result => {
      ElMessage({ "message": result.msg, "type": "success" })
      fileUploadRef.value.doClearFileList()
      addForm.value.resetFields()
      router.back()
  }).finally(() => {
    submitting.value = false
  })
}
// 处理文件列表变更
function handleFileListChange(uploadResultList: AnyObjsDefine) {
  const uploadedFiles = (uploadResultList || []) as Array<{
    relativePath?: string
    originalFilename?: string
  }>
  formData.files = uploadedFiles
    .filter((file): file is { relativePath: string; originalFilename: string } => Boolean(file.relativePath && file.originalFilename))
    .map(file => ({
      storePath: file.relativePath,
      originalFileName: file.originalFilename,
    }))
  addForm.value?.validateField('files');
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

.add-file-panel {
  padding: 22px 24px 20px;
}

.add-file-upload-icon {
  margin-right: 8px;
  font-size: 20px;
  color: var(--space-primary);
}

.add-file-footer {
  margin-top: 18px;
}

@media (max-width: 900px) {
  .workspace-task-panel-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
