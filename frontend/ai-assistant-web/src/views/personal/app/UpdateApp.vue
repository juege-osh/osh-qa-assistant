<template>
  <div>
    <el-dialog
      class="workspace-form-dialog app-form-dialog"
      title="更新应用"
      :model-value="updateDialogVisible"
      @open="handleOpen"
      width="680px"
      :before-close="handleCancel"
    >
      <div class="dialog-intro">
        调整应用名称、图标、描述和问答策略，保存后会立即影响当前应用的使用体验。
      </div>
      <el-form
        ref="updateForm"
        :model="formData"
        :rules="rules"
        class="app-form"
        label-position="top"
      >
        <div class="form-grid">
          <el-form-item label="应用名称" prop="appName" class="field-span-2">
            <el-input v-model="formData.appName" placeholder="例如：管道助手"></el-input>
          </el-form-item>
          <el-form-item label="应用图标" prop="iconPath" class="label-align-item">
            <div class="upload-panel">
              <FileUpload :file-list="uploadInfo.fileList" @file-list-change="handleFileListChange" ref="fileUploadRef">
              </FileUpload>
              <div class="field-help">建议上传清晰方形图标，方便在应用列表和聊天入口里识别。</div>
            </div>
          </el-form-item>
          <el-form-item label="应用描述" prop="appDesc">
            <el-input
              v-model="formData.appDesc"
              :rows="4"
              type="textarea"
              resize="none"
              placeholder="说明这个应用主要解决什么问题、适合谁使用、用户能得到什么结果。"
            ></el-input>
          </el-form-item>
          <el-form-item label="超出知识库范围是否回答" prop="outLibEnable" class="field-span-2">
            <el-radio-group v-model="formData.outLibEnable" class="radio-group">
              <el-radio :label="0" border>否，优先保证依据可靠</el-radio>
              <el-radio :label="1" border>是，可允许扩展回答</el-radio>
            </el-radio-group>
            <div class="field-help">建议知识问答类应用默认选择“否”，优先保障回答可信度。</div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer app-dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="handleCancel">取消</el-button>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="onSubmit">保存更新</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name='UpdateCategory' lang='ts'>
import { nextTick, reactive, ref } from "vue"
import { queryAppByIdApi, modifyAppByIdApi } from "@/api/workspace/appApi"
import { ElMessage } from "element-plus";
import type { UploadUserFile } from 'element-plus';
import type { AnyObjsDefine } from "@/types/common";
import FileUpload from '@/components/FileUpload.vue';
import { listAvailableLibApi } from "@/api/workspace/knowledgeLibApi";
import { useResource } from "@/hooks/useResource";
let formData = reactive({
  id: '',
  appName: '',
  iconPath: '',
  appDesc: '',
  outLibEnable: 0,
})
let rules = reactive({
  appName: [{ required: true, message: "请输入应用名称", trigger: "blur" }],
  appDesc: [{ required: true, message: "请输入应用描述", trigger: "blur" }],
  libId: [{ required: true, message: "请选择知识库", trigger: "blur" }],
  outLibEnable: [{ required: true, message: "请选择是否回答", trigger: "blur" }],
})
let pageData = reactive({
  knowledgeLibs: [] as AnyObjsDefine
})
// 上传文件fileList
let uploadInfo = reactive<{
  fileList: UploadUserFile[]
}>({ fileList: [] })

let {toAddressable} = useResource()
let updateForm = ref()
// props
const props = defineProps<{
  updateDialogVisible: boolean,
  idToUpdate: string
}>()
let emitter = defineEmits(["updateSuccess", "closeDialog"])

// 方法
function onSubmit() {
  updateForm.value.validate((valid: boolean) => {
    if (!valid) return
    modifyAppByIdApi(formData).then(result => {
      ElMessage({ message: result.msg, type: "success" })
      uploadInfo.fileList = []
      updateForm.value.resetFields()
      emitter("updateSuccess")
    })
  })
}
//取消
function handleCancel() {
  uploadInfo.fileList = []
  updateForm.value.resetFields()
  emitter("closeDialog")
}
function queryAppById() {
  // 不然就累加了
  uploadInfo.fileList = []
  queryAppByIdApi(props.idToUpdate)
    .then(result => {
      Object.assign(formData, result.data)
      // 设置图片
      let addressablePath = toAddressable(result.data.iconPath)
      if (addressablePath) {
        uploadInfo.fileList.push({ url: addressablePath, name: '' })
      }
    })
}
// 处理文件列表变更
function handleFileListChange(uploadResultList:AnyObjsDefine) {
  if(uploadResultList && uploadResultList.length > 0) {
    formData.iconPath = uploadResultList[0].relativePath
  }else{
    formData.iconPath = ''
  }
  updateForm.value.validateField('iconPath');
}
// 列出知识库
function listAvailable() {
 listAvailableLibApi(props.idToUpdate).then(result => {
    if(result.data) {
      pageData.knowledgeLibs = result.data
    }
  })
}
// 打开对话框回调
function handleOpen() {
  queryAppById()
  listAvailable()
}
</script>
<style scoped>
:deep(.app-form-dialog) {
  border-radius: 24px !important;
  overflow: hidden;
}

:deep(.app-form-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 24px 26px 10px;
}

:deep(.app-form-dialog .el-dialog__title) {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

:deep(.app-form-dialog .el-dialog__body) {
  padding: 0 26px 4px;
}

:deep(.app-form-dialog .el-dialog__footer) {
  padding: 10px 26px 22px;
}

:deep(.app-form .el-form-item) {
  margin-bottom: 18px;
}

:deep(.app-form .el-form-item__label) {
  padding-bottom: 8px;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.25;
}

:deep(.label-align-item .el-form-item__label)::before {
  content: "*";
  visibility: hidden;
  margin-right: 4px;
}

:deep(.app-form .el-input__wrapper) {
  min-height: 44px;
}

:deep(.app-form .el-input__inner),
:deep(.app-form .el-textarea__inner) {
  font-size: 15px;
}

:deep(.app-form .el-textarea__inner) {
  padding-top: 12px;
  line-height: 1.7;
}

:deep(.app-form .el-radio.is-bordered) {
  min-height: 44px;
  margin: 0;
  padding: 0 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
}

.dialog-intro {
  margin-bottom: 16px;
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.form-grid {
  display: grid;
  grid-template-columns: 172px minmax(0, 1fr);
  gap: 0 18px;
  align-items: start;
}

.field-span-2 {
  grid-column: 1 / -1;
}

.upload-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.field-help {
  margin-top: 6px;
  color: var(--space-text-soft);
  font-size: 12px;
  line-height: 1.55;
}

.app-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.upload-panel .el-upload--picture-card),
:deep(.upload-panel .el-upload-list--picture-card .el-upload-list__item) {
  width: 132px;
  height: 132px;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
