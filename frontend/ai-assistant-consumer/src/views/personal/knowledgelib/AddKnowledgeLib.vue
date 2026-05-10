<template>
  <div>
    <el-dialog title="新增" :model-value="addDialogVisible" :before-close="handleCancel" width="30%">
      <el-form ref="addForm" :model="formData" :rules="rules" label-width="120px">
        <el-form-item label="名称:" prop="libName">
          <el-input v-model="formData.libName"></el-input>
        </el-form-item>
        <el-form-item label="描述:" prop="libDesc">
          <el-input :rows="6" type="textarea" v-model="formData.libDesc"></el-input>
        </el-form-item>
        <el-form-item label="图标:" prop="iconPath">
          <FileUpload @file-list-change="handleFileListChange" ref="fileUploadRef"></FileUpload>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="handleCancel()">取消</el-button>
        <el-button type="primary" @click="onSubmit()">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script setup name='AddKnowledgeLib' lang='ts'>
import { ref, reactive } from 'vue'
import { addKnowledgeLibApi } from '@/api/knowledgeLibApi';
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
