<template>
  <div>
    <el-dialog title="更新" :model-value="updateDialogVisible" @open="handleOpen" width="40%"
      :before-close="handleCancel">
      <el-form ref="updateForm" :model="formData" :rules="rules" label-width="200px">
        <el-form-item label="名称:" prop="appName">
          <el-input v-model="formData.appName"></el-input>
        </el-form-item>
        <el-form-item label="图标:" prop="iconPath">
          <FileUpload :file-list="uploadInfo.fileList" @file-list-change="handleFileListChange" ref="fileUploadRef">
          </FileUpload>
        </el-form-item>
        <el-form-item label="描述:" prop="appDesc">
          <el-input :rows="6" type="textarea" v-model="formData.appDesc"></el-input>
        </el-form-item>
        <el-form-item label="超出知识库范围是否回答:" prop="outLibEnable">
          <el-radio v-model="formData.outLibEnable" :label="0">否</el-radio>
          <el-radio v-model="formData.outLibEnable" :label="1">是</el-radio>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="onSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script setup name='UpdateCategory' lang='ts'>
import { nextTick, reactive, ref } from "vue"
import { queryAppByIdApi, modifyAppByIdApi } from "@/api/appApi"
import { ElMessage } from "element-plus";
import type { UploadUserFile } from 'element-plus';
import type { AnyObjsDefine } from "@/types/common";
import FileUpload from '@/components/FileUpload.vue';
import { listAvailableLibApi } from "@/api/knowledgeLibApi";
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
.el-tag {
  margin-right: 1rem;
}
</style>
