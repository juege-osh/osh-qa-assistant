<template>
  <div>
    <el-dialog
      class="workspace-form-dialog"
      title="更新知识库"
      :model-value="updateDialogVisible"
      @open="handleOpen"
      width="640px"
      :before-close="handleCancel"
    >
      <section class="workspace-info-card workspace-dialog-summary-card">
        <div class="workspace-info-grid workspace-info-grid--compact">
          <div class="workspace-info-item">
            <div class="workspace-info-label">知识库 ID</div>
            <div class="workspace-info-value workspace-info-value--mono">{{ formData.id || props.idToUpdate || '--' }}</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">当前图标状态</div>
            <div class="workspace-info-value">{{ formData.iconPath ? '已配置图标' : '暂未配置图标' }}</div>
          </div>
        </div>
      </section>
      <el-form ref="updateForm" :model="formData" :rules="rules" label-position="top">
        <div class="workspace-form-grid workspace-form-grid--single">
          <el-form-item label="知识库名称" prop="libName">
            <el-input v-model="formData.libName" maxlength="40" show-word-limit placeholder="例如：产品知识库"></el-input>
            <div class="field-help">建议保持名称清晰稳定，避免应用绑定后出现识别歧义。</div>
          </el-form-item>
          <el-form-item label="知识库描述" prop="libDesc">
            <el-input :rows="5" type="textarea" resize="none" v-model="formData.libDesc" placeholder="说明这个知识库覆盖哪些资料、给谁使用。"></el-input>
            <div class="field-help">建议补齐内容边界、适用对象和典型问题，便于后续维护与调试。</div>
          </el-form-item>
          <el-form-item label="知识库图标" prop="iconPath" class="upload-card-field">
            <div class="workspace-upload-panel">
              <FileUpload :file-list="uploadInfo.fileList" @file-list-change="handleFileListChange" ref="fileUploadRef">
              </FileUpload>
              <div class="field-help">建议使用清晰的方形图标，方便在应用绑定和列表页识别。</div>
            </div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="workspace-dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="handleCancel">取消</el-button>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="onSubmit">保存更新</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name='UpdateKnowledgeLib' lang='ts'>
import { reactive, ref } from "vue"
import { queryKnowledgeLibByIdApi, modifyKnowledgeLibByIdApi } from "@/api/workspace/knowledgeLibApi"
import { ElMessage, type UploadUserFile } from "element-plus";
import type { AnyObjsDefine } from "@/types/common";
import FileUpload from '@/components/FileUpload.vue';
import { useResource } from "@/hooks/useResource";

let formData = reactive({
  id: '',
  libName: '',
  libDesc: '',
  iconPath: '',
})
let rules = reactive({
  libName: [{ required: true, message: "请输入知识库名称", trigger: "blur" }],
  libDesc: [{ required: true, message: "请输入知识库描述", trigger: "blur" }],
})
let updateForm = ref()
// 上传文件fileList
let uploadInfo = reactive<{
  fileList: UploadUserFile[]
}>({ fileList: [] })

let {toAddressable} = useResource()
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
    modifyKnowledgeLibByIdApi(formData).then(result => {
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
// 处理文件列表变更
function handleFileListChange(uploadResultList:AnyObjsDefine) {
  if(uploadResultList && uploadResultList.length > 0) {
    formData.iconPath = uploadResultList[0].relativePath
  }else{
    formData.iconPath = ''
  }
  updateForm.value.validateField('iconPath');
}
function queryAppById() {
  // 不然就累加了
  uploadInfo.fileList = []
  queryKnowledgeLibByIdApi(props.idToUpdate)
    .then(result => {
      Object.assign(formData, result.data)
       // 设置图片
      let addressablePath = toAddressable(result.data.iconPath)
      if (addressablePath) {
        uploadInfo.fileList.push({ url: addressablePath, name: '' })
      }
    })
}
// 打开对话框回调
function handleOpen() {
  queryAppById()
}
</script>
<style scoped></style>
