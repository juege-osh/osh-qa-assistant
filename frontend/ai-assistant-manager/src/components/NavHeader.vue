<template>
  <div>
    <el-row>
      <el-col :span="12" class="nav-intro">
        <div class="nav-brand">
          <div class="nav-brand-mark" aria-hidden="true"></div>
          <div class="nav-brand-copy">
            <div class="nav-title">OSH Wisdom</div>
            <div class="nav-divider"></div>
            <div class="nav-subtitle">管理控制台</div>
          </div>
        </div>
      </el-col>
      <el-col :span="4" :offset="8" class="user-info">
        <el-dropdown class="profile-container" trigger="click">
          <span class="cursor-pointer">
            <el-avatar class="avatar" v-if="userStore.userInfo.avatarPath"
              :src="toAddressable(userStore.userInfo.avatarPath)">
            </el-avatar>
            <el-avatar class="avatar" v-else :src="getImage('user.png')"></el-avatar>
            <el-icon class="arrow">
              <arrow-down></arrow-down>
            </el-icon>
          </span>
          <template v-slot:dropdown>
            <el-dropdown-menu>
              <div>
                <el-dropdown-item @click="openUpdateProfileDialog(userStore.userInfo.id)">修改个人信息</el-dropdown-item>
              </div>
              <div>
                <el-dropdown-item @click="openUpdatePwd">修改密码</el-dropdown-item>
              </div>
              <el-dropdown-item @click="handleLogout">退出</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-col>
    </el-row>

    <!-- 修改个人信息对话框 -->
    <el-dialog title="修改个人信息" v-model="updateProfileDiaVisible" @close="handleUpdateProfileDiaCancel">
      <el-form ref="updateProfileForm" :model="updateProfileFormData" :rules="updateProfileRules" label-width="120px">
        <el-form-item label="用户编号:" prop="id">
          <el-input v-model="updateProfileFormData.id" disabled></el-input>
        </el-form-item>
        <el-form-item label="用户名:" prop="username">
          <el-input v-model="updateProfileFormData.username" disabled></el-input>
        </el-form-item>
        <el-form-item label="照片:" prop="avatarPath">
          <FileUpload :file-list="uploadInfo.fileList"
            @file-list-change="handleFileListChange" ref="fileUploadRef"></FileUpload>
        </el-form-item>
        <el-form-item label="真实姓名:" prop="realName">
          <el-input v-model="updateProfileFormData.realName"></el-input>
        </el-form-item>
        <el-form-item label="性别:" prop="sex">
          <el-radio v-model="updateProfileFormData.sex" :label="1">男</el-radio>
          <el-radio v-model="updateProfileFormData.sex" :label="0">女</el-radio>
        </el-form-item>
        <div slot="footer" class="dialog-footer">
          <el-button @click="updateProfileDiaVisible = false">取消</el-button>
          <el-button type="primary" @click="updateProfile()">确定</el-button>
        </div>
      </el-form>
    </el-dialog>
    <!-- 修改密码对话框 -->
    <el-dialog title="修改密码" v-model="updatePwdDiaVisible" @close="handleUpdatePwdDiaCancel()">
      <el-form ref="updatePwdForm" :model="updatePwdFormData" :rules="updatePwdRules" label-width="120px">
        <el-form-item label="原始密码" prop="originalPwd">
          <el-input v-model="updatePwdFormData.originalPwd" type="password" placeholder="请输入原始密码"
            show-password></el-input>
        </el-form-item>
        <el-form-item label="新密码密码" prop="newPwd">
          <el-input v-model="updatePwdFormData.newPwd" type="password" placeholder="请输入新密码" show-password></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="handleUpdatePwdDiaCancel()">取消</el-button>
        <el-button type="primary" @click="updatePwd()">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script setup name='NavHeader' lang='ts'>
import { getImage } from '@/util/AssetsImageUtil';
import { useUserStore } from '@/store/useUserStore';
import { useTabStore } from '@/store/useTabStore'
import { clearAll } from '@/util/storageUtil';
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import type { UploadUserFile } from 'element-plus';
import { modifyManagerByIdApi, modifyPwdApi, queryManagerByIdApi } from '@/api/managerApi';
import FileUpload from '@/components/FileUpload.vue';
import type { AnyObjsDefine } from "@/types/common";
import { useResource } from '@/hooks/useResource';

let updateProfileForm = ref()
let updatePwdForm = ref()
// 控制对话框是否可见
let updateProfileDiaVisible = ref(false)
let updatePwdDiaVisible = ref(false)
let userStore = useUserStore()
let tabStore = useTabStore()
let updateProfileFormData = reactive({
  id: '',
  username: '',
  realName: '',
  sex: -1,
  avatarPath: ''
})
let updatePwdFormData = reactive({
  id: '',
  originalPwd: '',
  newPwd: '',
})
let updateProfileRules = reactive({
  realName: [
    { required: true, message: "请输入姓名", trigger: "blur" },
  ],
  sex: [
    { required: true, message: "请选择性别", trigger: "blur" },
  ]
})
let updatePwdRules = reactive({
  originalPwd: [
    { required: true, message: "请输入原始密码", trigger: "blur" },
  ],
  newPwd: [
    { required: true, message: "请输入新密码", trigger: "blur" },
  ]
})

// 上传文件fileList
let uploadInfo = reactive<{
  fileList: UploadUserFile[]
}>({ fileList: [] })

let { toAddressable } = useResource()

// 处理下拉框点击
function openUpdateProfileDialog(id: string) {
  // 不然就累加了
  uploadInfo.fileList = []
  // 查询用户信息回显
  queryManagerByIdApi(id).then(result => {
    Object.assign(updateProfileFormData, result.data)
    // 设置图片
    let addressablePath = toAddressable(result.data.avatarPath)
    if (addressablePath) {
      uploadInfo.fileList.push({ url: addressablePath, name: '' })
    }
  })
  updateProfileDiaVisible.value = true
}
function handleLogout() {
   // 退出登录
   clearAll()
   // 重置所有pinia的状态,避免刷新前在app中又存到storage中了
   userStore.resetAll()
   tabStore.resetAll()
   // 刷新当前页面相当于清空了pinia中的状态数据
   window.location.reload()
}
function openUpdatePwd() {
  // 设置要更新的用户id
  updatePwdFormData.id = userStore.userInfo.id
  updatePwdDiaVisible.value = true
}

// 修改个人信息对话框关闭事件回调
function handleUpdateProfileDiaCancel() {
  uploadInfo.fileList = []
  updateProfileForm.value.resetFields()
  updateProfileDiaVisible.value = false
}
// 修改密码对话框关闭事件回调
function handleUpdatePwdDiaCancel() {
  updatePwdForm.value.resetFields()
  updatePwdDiaVisible.value = false
}

let fileUploadRef = ref()
// 提交修改个人信息
function updateProfile() {
  updateProfileForm.value.validate((valid: boolean) => {
    if (!valid) return
    modifyManagerByIdApi(updateProfileFormData).then(result => {
      ElMessage({ "message": result.msg, "type": "success" })
      uploadInfo.fileList = []
      // 在重置之前更新,不然重置了就没有值了
      userStore.$patch((state) => {
        // 把最新的头像更新到pinia的store中
        state.userInfo.avatarPath = updateProfileFormData.avatarPath
      })
      updateProfileForm.value.resetFields()
      updateProfileDiaVisible.value = false
    })
  })
}
// 提交修改密码
function updatePwd() {
  updatePwdForm.value.validate((valid: boolean) => {
    if (!valid) return
    modifyPwdApi(updatePwdFormData).then(result => {
      ElMessage({ "message": result.msg, "type": "success" })
      updatePwdForm.value.resetFields()
      updatePwdDiaVisible.value = false
    })
  })
}

// 处理文件列表变更
function handleFileListChange(uploadResultList:AnyObjsDefine) {
  if(uploadResultList && uploadResultList.length > 0) {
    updateProfileFormData.avatarPath = uploadResultList[0].relativePath
  }else{
    updateProfileFormData.avatarPath = ''
  }
  updateProfileForm.value.validateField('avatarPath');
}
</script>
<style scoped>
.nav-intro {
  height: 64px;
  display: flex;
  align-items: center;
  padding-left: 12px;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 16px 10px 10px;
  border: 1px solid rgba(52, 211, 153, 0.22);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(11, 22, 52, 0.92), rgba(18, 32, 72, 0.7));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.03), 0 14px 32px rgba(0, 0, 0, 0.28);
}

.nav-brand-mark {
  position: relative;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background:
    radial-gradient(circle at 34% 30%, rgba(255, 255, 255, 0.98), rgba(52, 211, 153, 0.95) 14%, rgba(124, 92, 255, 0.85) 48%, rgba(13, 23, 52, 0.92) 76%);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.12),
    0 10px 26px rgba(72, 214, 255, 0.22);
}

.nav-brand-mark::before,
.nav-brand-mark::after {
  content: "";
  position: absolute;
  border-radius: 999px;
}

.nav-brand-mark::before {
  width: 12px;
  height: 12px;
  top: 15px;
  left: 15px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 0 16px rgba(255, 255, 255, 0.55);
}

.nav-brand-mark::after {
  width: 26px;
  height: 26px;
  top: 8px;
  left: 8px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  opacity: 0.72;
}

.nav-brand-copy {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 42px;
}

.nav-title {
  color: #fff;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.03em;
  line-height: 1;
}

.nav-divider {
  width: 1px;
  height: 18px;
  background: rgba(52, 211, 153, 0.26);
}

.nav-subtitle {
  color: var(--space-muted);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  line-height: 1;
}

.user-info {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.profile-container {
  padding: 6px 12px;
  border: 1px solid var(--space-border);
  border-radius: 999px;
  background: rgba(52, 211, 153, 0.08);
  box-shadow: var(--space-glow);
}

.cursor-pointer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  border: 2px solid rgba(52, 211, 153, 0.5);
  box-shadow: 0 0 20px rgba(52, 211, 153, 0.32);
}

.arrow {
  color: var(--space-primary);
}

:deep(.el-dialog) {
  border: 1px solid var(--space-border);
}
</style>
