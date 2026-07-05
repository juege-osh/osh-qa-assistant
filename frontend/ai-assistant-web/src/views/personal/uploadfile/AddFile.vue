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
