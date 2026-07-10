<template>
  <div>
    <el-dialog
      class="workspace-form-dialog app-form-dialog"
      title="新增应用"
      :model-value="addDialogVisible"
      @open="handleOpen"
      :before-close="handleCancel"
      width="680px"
    >
      <el-form
        ref="addForm"
        :model="formData"
        :rules="rules"
        class="app-form"
        label-position="top"
      >
        <div class="workspace-form-grid workspace-form-grid--aside">
          <el-form-item label="应用名称" prop="appName" class="workspace-form-span-2">
            <el-input v-model="formData.appName" placeholder="例如：管道助手"></el-input>
          </el-form-item>
          <el-form-item label="应用图标" prop="iconPath" class="label-align-item app-upload-item">
            <div class="workspace-upload-panel">
              <FileUpload @file-list-change="handleFileListChange" ref="fileUploadRef"></FileUpload>
              <div class="field-help">建议使用清晰的方形图标。</div>
            </div>
          </el-form-item>
          <el-form-item label="应用描述" prop="appDesc">
            <el-input
              v-model="formData.appDesc"
              :rows="4"
              type="textarea"
              resize="none"
              placeholder="一句话说明这个应用帮谁解决什么问题。"
            ></el-input>
          </el-form-item>
          <el-form-item label="超出知识库范围是否回答" prop="outLibEnable" class="workspace-form-span-2">
            <el-radio-group v-model="formData.outLibEnable" class="workspace-radio-group">
              <el-radio label="0" border>否，优先保证依据可靠</el-radio>
              <el-radio label="1" border>是，可允许扩展回答</el-radio>
            </el-radio-group>
            <div class="field-help">知识问答场景通常选“否”。</div>
          </el-form-item>
          <el-form-item label="应用补充提示词" prop="customPrompt" class="workspace-form-span-2">
            <el-input
              v-model="formData.customPrompt"
              :rows="4"
              type="textarea"
              resize="none"
              placeholder="想固定回答语气或结构时再填，例如：先给结论，再列步骤。"
            ></el-input>
            <div class="field-help">留空就使用默认规则。</div>
          </el-form-item>
          <el-form-item label="聊天模型名称" prop="chatModel" class="workspace-form-span-2">
            <el-input
              v-model="formData.chatModel"
              placeholder="可选，例如：MiniMax-M2.7-highspeed。留空则走系统默认模型。"
            ></el-input>
            <div class="field-help">留空就使用默认模型。</div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="workspace-dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="handleCancel()">取消</el-button>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="onSubmit()">创建应用</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name='AddApp' lang='ts'>
import { reactive, ref } from 'vue'
import { addAppApi } from '@/api/workspace/appApi';
import { ElMessage } from 'element-plus';
import type { AnyObjsDefine } from '@/types/common';
import FileUpload from '@/components/FileUpload.vue';

let formData = reactive({
  appName: '',
  iconPath: '',
  appDesc: '',
  outLibEnable: '0',
  customPrompt: '',
  chatModel: ''
})
let rules = reactive({
  appName: [{ required: true, message: "请输入应用名称", trigger: "blur" }],
  appDesc: [{ required: true, message: "请输入应用描述", trigger: "blur" }],
  libId: [{ required: true, message: "请选择知识库", trigger: "blur" }],
  outLibEnable: [{ required: true, message: "请选择是否回答", trigger: "blur" }],
})
defineProps<{ addDialogVisible: boolean }>()

let emitter = defineEmits(["closeDialog", "addSuccess"])

let addForm = ref()
let fileUploadRef = ref()
function onSubmit() {
  addForm.value.validate((valid: boolean) => {
    if (!valid) return
    addAppApi(formData).then(result => {
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
// 打开对话框回调
function handleOpen() {}
</script>
