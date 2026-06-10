<template>
  <div class="add-file mt-dot5">
    <div class="top mb-dot5">
      <div class="back"  @click="$router.back()">
        <div class="back-icon"><el-icon color="#409eff">
            <Back />
          </el-icon></div>
        <div><el-text>返回</el-text></div>
      </div>
    </div>
    <div style="margin-top: 20px;">
      <el-form ref="addForm" :model="formData" :rules="rules">
        <el-form-item prop="storePath" style="width: 100%;">
          <FileUpload :drag="true" listType="text"
            @file-list-change="handleFileListChange" ref="fileUploadRef">
            <template v-slot:trigger>
              <el-icon>
                <UploadFilled />
              </el-icon>
              <span class="el-upload__text">
                拖拽文件到此处，或<em>点击上传</em>
              </span>
            </template>
            <template #default>
              支持 TXT、 MARKDOWN、PDF、 HTML、 XLSX、 XLS、PROPERTIES、 DOC、 DOCX、 CSV、PPTX、 XML、 PPT、 MD、 HTM，每个文件不超过 15MB
            </template>
          </FileUpload>
        </el-form-item>
        <div style="text-align: center;">
          <el-button type="primary" @click="onSubmit()">提交</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>
<script setup name='AddFile' lang='ts'>
import { onMounted, reactive, ref } from 'vue';
import FileUpload from '@/components/FileUpload.vue';
import type { AnyObjsDefine } from '@/types/common';
import { addUploadFileApi } from '@/api/workspace/uploadFileApi';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
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
.top {
  display: flex;
  align-items: center;
  /* 垂直居中 */
}

.back {
  /* 让图标和文字在一行 */
  display: inline-flex;
  /* 垂直居中 */
  align-items: center;
  /* 禁止换行 */
  white-space: nowrap;
  /* 把剩余空间全给右侧，保证 steps 居中 */
  margin-right: auto;
    cursor: pointer;
}

.back-icon {
  margin-right: 0.3rem;
}

.steps {
  flex: 1;
  padding: 0 100px;
  font-size: 12px;
}

:deep(.el-step__title) {
  font-size: 12px;
}
</style>
