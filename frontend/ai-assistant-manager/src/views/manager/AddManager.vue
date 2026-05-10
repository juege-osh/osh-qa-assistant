<template>
    <div>
        <el-dialog title="新增" :model-value="addDialogVisible" :before-close="handleCancel"
            width="30%">
            <el-form ref="addForm" :model="formData" :rules="rules" label-width="120px">
                <el-form-item label="用户名:" prop="username">
                  <el-input v-model="formData.username"></el-input>
                </el-form-item>
                <el-form-item label="照片:" prop="avatarPath">
                  <FileUpload
                    @file-list-change="handleFileListChange" ref="fileUploadRef"></FileUpload>
                </el-form-item>
                <el-form-item label="真实姓名:" prop="realName">
                  <el-input v-model="formData.realName"></el-input>
                </el-form-item>
                <el-form-item label="密码:" prop="pwd">
                  <el-input type="password" v-model="formData.pwd" show-password clearable></el-input>
                </el-form-item>
                <el-form-item label="性别:" prop="sex">
                  <el-radio v-model="formData.sex" label="1">男</el-radio>
                  <el-radio v-model="formData.sex" label="0">女</el-radio>
                </el-form-item>
            </el-form>
            <div slot="footer" class="dialog-footer">
                <el-button @click="handleCancel()">取消</el-button>
                <el-button type="primary" @click="onSubmit()">确定</el-button>
            </div>
        </el-dialog>
    </div>
</template>
<script setup name='AddManager' lang='ts'>
import { ref, reactive } from 'vue'
import { addManagerApi } from '@/api/managerApi';
import { ElMessage } from 'element-plus';
import type { AnyObjsDefine } from '@/types/common';
import FileUpload from '@/components/FileUpload.vue';

let formData = reactive({
  username: '',
  realName: '',
  sex: '1',
  avatarPath: '',
  pwd: '',
})
let rules = reactive({
      username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
      realName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
      sex: [{ required: true, message: "请选择性别", trigger: "blur" }],
      pwd: [{ required: true, message: "请输入密码", trigger: "blur" }]
})
defineProps<{ addDialogVisible: boolean }>()

let emitter = defineEmits(["closeDialog", "addSuccess"])

let addForm = ref()
let fileUploadRef = ref()
function onSubmit() {
    addForm.value.validate((valid: boolean) => {
        if (!valid) return
        addManagerApi(formData).then(result => {
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
function handleFileListChange(uploadResultList:AnyObjsDefine) {
  if(uploadResultList && uploadResultList.length > 0) {
    formData.avatarPath = uploadResultList[0].relativePath
  }else{
    formData.avatarPath = ''
  }
  addForm.value.validateField('avatarPath');
}
</script>
<style scoped></style>
