<template>
  <div>
    <el-dialog title="新增" :model-value="addDialogVisible" @open="handleOpen" :before-close="handleCancel" width="40%">
      <el-form ref="addForm" :model="formData" :rules="rules" label-width="200px">
        <el-form-item label="名称:" prop="appName">
          <el-input v-model="formData.appName"></el-input>
        </el-form-item>
        <el-form-item label="图标:" prop="iconPath">
          <FileUpload @file-list-change="handleFileListChange" ref="fileUploadRef"></FileUpload>
        </el-form-item>
        <el-form-item label="描述:" prop="appDesc">
          <el-input :rows="6" type="textarea" v-model="formData.appDesc"></el-input>
        </el-form-item>
        <el-form-item label="超出知识库范围是否回答:" prop="outLibEnable">
          <el-radio v-model="formData.outLibEnable" label="0">否</el-radio>
          <el-radio v-model="formData.outLibEnable" label="1">是</el-radio>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="handleCancel()">取消</el-button>
        <el-button type="primary" @click="onSubmit()">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script setup name='AddApp' lang='ts'>
import { ref, reactive } from 'vue'
import { addAppApi } from '@/api/appApi';
import { ElMessage } from 'element-plus';
import type { AnyObjsDefine } from '@/types/common';
import FileUpload from '@/components/FileUpload.vue';
import { listAvailableLibApi } from '@/api/knowledgeLibApi';

let formData = reactive({
  appName: '',
  iconPath: '',
  appDesc: '',
  outLibEnable: '0',
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
// 列出知识库
function listAvailable() {
 listAvailableLibApi().then(result => {
    if(result.data) {
      pageData.knowledgeLibs = result.data
    }
  })
}
// 打开对话框回调
function handleOpen() {
  listAvailable()
}
</script>
<style scoped></style>
