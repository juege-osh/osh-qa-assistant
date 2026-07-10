<template>
  <div>
    <el-dialog class="workspace-form-dialog manager-form-dialog" title="新增管理员" :model-value="addDialogVisible" :before-close="handleCancel"
      width="680px">
      <div class="dialog-intro">
        新增后，该账号就能进入管理台。建议只给需要运维权限的人开通。
      </div>
      <section class="workspace-info-card">
        <div class="workspace-info-grid workspace-info-grid--compact">
          <div class="workspace-info-item">
            <div class="workspace-info-label">权限范围</div>
            <div class="workspace-info-value">可进入管理台</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">账号状态</div>
            <div class="workspace-info-value">创建后立即可用</div>
          </div>
          <div class="workspace-info-item workspace-info-item--full">
            <div class="workspace-info-label">创建建议</div>
            <div class="workspace-info-value workspace-note-strong">优先使用清晰的真实姓名和头像，方便后续巡检、排班和权限核查。</div>
          </div>
        </div>
      </section>
      <el-form ref="addForm" :model="formData" :rules="rules" label-position="top">
        <div class="workspace-form-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="formData.username" maxlength="30" show-word-limit placeholder="请输入登录用户名"></el-input>
            <div class="field-help">建议使用容易识别的英文名或工号，不要和现有账号重复。</div>
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="formData.realName" maxlength="20" show-word-limit placeholder="请输入真实姓名"></el-input>
            <div class="field-help">用于管理台展示和运维协作识别。</div>
          </el-form-item>
          <el-form-item label="登录密码" prop="pwd">
            <el-input type="password" v-model="formData.pwd" show-password clearable placeholder="请输入初始密码"></el-input>
            <div class="field-help">建议创建后让对方尽快改成自己的常用密码。</div>
          </el-form-item>
          <el-form-item label="性别" prop="sex">
            <el-radio-group v-model="formData.sex" class="workspace-radio-group">
              <el-radio label="1" border>男</el-radio>
              <el-radio label="0" border>女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="头像" prop="avatarPath" class="workspace-form-span-2 upload-card-field">
            <div class="workspace-upload-panel">
              <FileUpload
                @file-list-change="handleFileListChange" ref="fileUploadRef"></FileUpload>
              <div class="field-help">建议上传清晰头像，方便后台审查与识别。</div>
            </div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="workspace-dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="handleCancel()">取消</el-button>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="onSubmit()">创建管理员</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name='AddManager' lang='ts'>
import { ref, reactive } from 'vue'
import { addManagerApi } from '@/api/admin/managerApi';
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
