<template>
  <div>
    <el-dialog
      class="workspace-form-dialog"
      title="新增知识库"
      :model-value="addDialogVisible"
      :before-close="handleCancel"
      width="640px"
    >
      <el-form ref="addForm" :model="formData" :rules="rules" label-position="top">
        <div class="workspace-form-grid workspace-form-grid--single">
          <el-form-item label="知识库名称" prop="libName">
            <el-input v-model="formData.libName" maxlength="40" show-word-limit placeholder="例如：产品知识库"></el-input>
            <div class="field-help">建议直接体现资料范围或使用场景，后续绑定应用时更容易识别。</div>
          </el-form-item>
          <el-form-item label="知识库描述" prop="libDesc">
            <el-input :rows="5" type="textarea" resize="none" v-model="formData.libDesc" placeholder="说明这个知识库覆盖哪些资料、给谁使用。"></el-input>
            <div class="field-help">建议说明内容边界、典型问题和适用对象，方便后续调试和协作。</div>
          </el-form-item>
          <el-form-item label="知识库图标" prop="iconPath" class="upload-card-field">
            <div class="workspace-upload-panel">
              <FileUpload @file-list-change="handleFileListChange" ref="fileUploadRef"></FileUpload>
              <div class="field-help">建议使用清晰的方形图标，方便在应用绑定和列表页识别。</div>
            </div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="workspace-dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="handleCancel()">取消</el-button>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="onSubmit()">创建知识库</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name='AddKnowledgeLib' lang='ts'>
import { ref, reactive } from 'vue'
import { addKnowledgeLibApi } from '@/api/workspace/knowledgeLibApi';
import { ElMessage } from 'element-plus';
import FileUpload from '@/components/FileUpload.vue';
import type { AnyObjsDefine } from '@/types/common';
let formData = reactive({
  libName: '',
  libDesc: '',
  iconPath: '',
})
let rules = reactive({
  libName: [{ required: true, message: "请输入知识库名称", trigger: "blur" }],
  libDesc: [{ required: true, message: "请输入知识库描述", trigger: "blur" }],
})
defineProps<{ addDialogVisible: boolean }>()

let emitter = defineEmits(["closeDialog", "addSuccess"])

let addForm = ref()
let fileUploadRef = ref()
function onSubmit() {
  addForm.value.validate((valid: boolean) => {
    if (!valid) return
    addKnowledgeLibApi(formData).then(result => {
      ElMessage({ "message": result.msg, "type": "success" })
      fileUploadRef.value.doClearFileList()
      addForm.value.resetFields()
      emitter("addSuccess")
    })
  })
}
// 取消对话框
function handleCancel() {
  fileUploadRef.value.doClearFileList()
  addForm.value.resetFields()
  emitter("closeDialog")
}
// 处理文件列表变更
function handleFileListChange(uploadResultList: AnyObjsDefine) {
  if (uploadResultList && uploadResultList.length > 0) {
    formData.iconPath = uploadResultList[0].relativePath
  } else {
    formData.iconPath = ''
  }
  addForm.value.validateField('iconPath');
}
</script>
<style scoped></style>
