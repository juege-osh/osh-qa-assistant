<template>
  <div>
    <el-dialog title="绑定知识库" :model-value="bindLibDialogVisible" @open="handleOpen" :before-close="handleCancel" width="40%">
      <el-form ref="bindForm" :model="formData" :rules="rules" label-width="200px">
        <el-form-item label="名称:" prop="appName">
          <el-input v-model="formData.appName" disabled></el-input>
        </el-form-item>
        <el-form-item label="知识库:" prop="libId">
          <el-select v-model="formData.libId" style="width: 150px">
            <el-option v-for="one in pageData.knowledgeLibs" :key="one.id" :label="one.libName"
              :value="one.id"></el-option>
          </el-select>
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
import { bindLibApi, queryAppByIdApi } from '@/api/appApi';
import { ElMessage } from 'element-plus';
import type { AnyObjsDefine } from '@/types/common';
import { listAvailableLibApi } from '@/api/knowledgeLibApi';

let formData = reactive({
  id: '',
  appName: '',
  libId: '',
})
let rules = reactive({
  libId: [{ required: true, message: "请选择知识库", trigger: "blur" }],
})
let pageData = reactive({
  knowledgeLibs: [] as AnyObjsDefine
})
// props
const props = defineProps<{
  bindLibDialogVisible: boolean,
  idToBindLib: string
}>()

let emitter = defineEmits(["closeDialog", "bindSuccess"])

let bindForm = ref()
function onSubmit() {
  bindForm.value.validate((valid: boolean) => {
    if (!valid) return
    bindLibApi(formData).then(result => {
      ElMessage({ "message": result.msg, "type": "success" })
      bindForm.value.resetFields()
      emitter("bindSuccess")
    })
  })
}
// 取消对话框
function handleCancel() {
  bindForm.value.resetFields()
  emitter("closeDialog")
}
function queryAppById() {
  queryAppByIdApi(props.idToBindLib)
    .then(result => {
      Object.assign(formData, result.data)
      listAvailable()
    })
}
// 列出知识库
function listAvailable() {
 listAvailableLibApi().then(result => {
    if(result.data) {
      pageData.knowledgeLibs = result.data
      formData.libId = result.data[0].id
    }
  })
}
// 打开对话框回调
function handleOpen() {
  queryAppById()
}
</script>
<style scoped></style>
