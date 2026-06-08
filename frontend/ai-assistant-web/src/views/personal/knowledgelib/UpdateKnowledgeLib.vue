<template>
  <div>
    <el-dialog title="更新" :model-value="updateDialogVisible" @open="handleOpen" width="30%"
      :before-close="handleCancel">
      <el-form ref="updateForm" :model="formData" :rules="rules" label-width="120px">
        <el-form-item label="名称:" prop="libName">
          <el-input v-model="formData.libName"></el-input>
        </el-form-item>
        <el-form-item label="描述:" prop="libDesc">
          <el-input :rows="6" type="textarea" v-model="formData.libDesc"></el-input>
        </el-form-item>
        <el-form-item label="图标:" prop="iconPath">
          <FileUpload :file-list="uploadInfo.fileList" @file-list-change="handleFileListChange" ref="fileUploadRef">
          </FileUpload>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="onSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script setup name='UpdateKnowledgeLib' lang='ts'>
import { nextTick, reactive, ref } from "vue"
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
<style scoped>
.el-tag {
  margin-right: 1rem;
}
</style>
