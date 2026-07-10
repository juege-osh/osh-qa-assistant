<template>
  <el-dialog
    v-model="updateProfileDiaVisible"
    class="workspace-form-dialog"
    title="修改个人信息"
    width="620px"
    @close="handleUpdateProfileDiaCancel"
  >
    <el-form
      ref="updateProfileForm"
      :model="updateProfileFormData"
      :rules="updateProfileRules"
      class="workspace-account-form"
      label-position="top"
    >
      <div class="workspace-form-grid workspace-form-grid--single">
        <el-form-item label="用户编号" prop="id">
          <el-input v-model="updateProfileFormData.id" disabled></el-input>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="updateProfileFormData.username" disabled></el-input>
        </el-form-item>
        <el-form-item label="头像" prop="avatarPath">
          <div class="workspace-upload-panel">
            <FileUpload :file-list="uploadInfo.fileList" @file-list-change="handleFileListChange" />
            <div class="field-help">更新后会同步展示在左侧导航和与账号相关的协作入口里。</div>
          </div>
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <div class="workspace-dialog-footer">
        <el-button class="workspace-btn workspace-btn--ghost" @click="updateProfileDiaVisible = false">取消</el-button>
        <el-button class="workspace-btn workspace-btn--primary" type="primary" @click="updateProfile">保存</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog
    v-model="updatePwdDiaVisible"
    class="workspace-form-dialog"
    title="修改密码"
    width="620px"
    @close="handleUpdatePwdDiaCancel"
  >
    <el-form
      ref="updatePwdForm"
      :model="updatePwdFormData"
      :rules="updatePwdRules"
      class="workspace-account-form"
      label-position="top"
    >
      <div class="workspace-form-grid workspace-form-grid--single">
        <el-form-item label="原始密码" prop="originalPwd">
          <el-input v-model="updatePwdFormData.originalPwd" type="password" placeholder="请输入原始密码" show-password></el-input>
          <div class="field-help">用于确认当前账号身份。</div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd">
          <el-input v-model="updatePwdFormData.newPwd" type="password" placeholder="请输入新密码" show-password></el-input>
          <div class="field-help">建议至少包含字母和数字，便于兼顾安全性与可记忆性。</div>
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <div class="workspace-dialog-footer">
        <el-button class="workspace-btn workspace-btn--ghost" @click="handleUpdatePwdDiaCancel">取消</el-button>
        <el-button class="workspace-btn workspace-btn--primary" type="primary" @click="updatePwd">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type UploadUserFile } from 'element-plus'
import FileUpload from '@/components/FileUpload.vue'
import { useUserStore } from '@/store/useUserStore'
import { modifyUserByIdApi, queryUserByIdApi, updatePwdApi } from '@/api/workspace/userApi'
import type { AnyObjsDefine } from '@/types/common'
import { useResource } from '@/hooks/useResource'

export interface UserAccountDialogsExpose {
  openUpdateProfileDialog: () => void
  openUpdatePwd: () => void
}

const userStore = useUserStore()
const { toAddressable } = useResource()

const updateProfileForm = ref()
const updatePwdForm = ref()
const updatePwdDiaVisible = ref(false)
const updateProfileDiaVisible = ref(false)

const updatePwdFormData = reactive({
  id: '',
  originalPwd: '',
  newPwd: '',
})

const updateProfileRules = reactive({})

const updatePwdRules = reactive({
  originalPwd: [
    { required: true, message: '请输入原始密码', trigger: 'blur' },
  ],
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
  ]
})

const updateProfileFormData = reactive({
  id: '',
  username: '',
  avatarPath: ''
})

const uploadInfo = reactive<{
  fileList: UploadUserFile[]
}>({
  fileList: []
})

function openUpdateProfileDialog() {
  uploadInfo.fileList = []
  queryUserByIdApi(userStore.userInfo.id).then(result => {
    Object.assign(updateProfileFormData, result.data)
    const addressablePath = toAddressable(result.data.avatarPath)
    if (addressablePath) {
      uploadInfo.fileList.push({ url: addressablePath, name: '' })
    }
    updateProfileDiaVisible.value = true
  })
}

function openUpdatePwd() {
  updatePwdFormData.id = userStore.userInfo.id
  updatePwdDiaVisible.value = true
}

function handleUpdateProfileDiaCancel() {
  uploadInfo.fileList = []
  updateProfileForm.value?.resetFields()
  updateProfileDiaVisible.value = false
}

function handleUpdatePwdDiaCancel() {
  updatePwdForm.value?.resetFields()
  updatePwdDiaVisible.value = false
}

function updateProfile() {
  updateProfileForm.value?.validate((valid: boolean) => {
    if (!valid) return
    modifyUserByIdApi(updateProfileFormData).then(result => {
      ElMessage({ message: result.msg, type: 'success' })
      uploadInfo.fileList = []
      userStore.$patch((state) => {
        state.userInfo.avatarPath = updateProfileFormData.avatarPath
      })
      updateProfileForm.value?.resetFields()
      updateProfileDiaVisible.value = false
    })
  })
}

function updatePwd() {
  updatePwdForm.value?.validate((valid: boolean) => {
    if (!valid) return
    updatePwdApi(updatePwdFormData).then(result => {
      ElMessage({ message: result.msg, type: 'success' })
      updatePwdForm.value?.resetFields()
      updatePwdDiaVisible.value = false
    })
  })
}

function handleFileListChange(uploadResultList: AnyObjsDefine) {
  if (uploadResultList && uploadResultList.length > 0) {
    updateProfileFormData.avatarPath = uploadResultList[0].relativePath
  } else {
    updateProfileFormData.avatarPath = ''
  }
  updateProfileForm.value?.validateField('avatarPath')
}

defineExpose<UserAccountDialogsExpose>({
  openUpdateProfileDialog,
  openUpdatePwd,
})
</script>

<style scoped>
.workspace-account-form {
  margin-top: 2px;
}

.workspace-account-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}
</style>
