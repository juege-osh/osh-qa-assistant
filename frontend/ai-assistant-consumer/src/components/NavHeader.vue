<template>
  <div class="common-header">
    <el-row>
      <el-col :span="20" :offset="2">
        <el-menu :default-active="activeItem" mode="horizontal" :ellipsis="false" @select="handleSelect">
          <div class="nav-left">
            <el-menu-item index="/" class="brand-menu-item">
              <div class="brand-shell">
                <div class="brand-mark" aria-hidden="true"></div>
                <div class="brand-copy">
                  <div class="brand-title">AI Assistant</div>
                  <div class="brand-divider"></div>
                  <div class="brand-subtitle">Knowledge Workspace</div>
                </div>
              </div>
            </el-menu-item>
            <el-menu-item v-if="hasLogin" index="/personal/app/manage"><el-icon>
                <ChatDotRound />
              </el-icon>应用</el-menu-item>
            <el-menu-item v-if="hasLogin" index="/personal/knowledgeLib/manage"><el-icon>
                <Document />
              </el-icon>知识库</el-menu-item>
            <el-menu-item index="/doc"><el-icon>
                <Memo />
              </el-icon>接口文档</el-menu-item>
          </div>
          <div class="nav-right" v-if="hasLogin">
            <el-sub-menu index="/user-submenu">
              <template v-slot:title>
                <div class="avatar">
                  <el-avatar v-if="userStore.userInfo.avatarPath"
                    :src="toAddressable(userStore.userInfo.avatarPath)"></el-avatar>
                  <el-avatar v-else :src="getImage('user.png')"></el-avatar>
                  <div class="greet">你好,{{ userStore.userInfo.username }}</div>
                </div>
              </template>
              <template v-if="hasLogin">
                <el-menu-item index="/user-submenu/profile" @click="openUpdateProfileDialog(userStore.userInfo.id)">修改个人信息</el-menu-item>
                <el-menu-item index="/user-submenu/pwd" @click="openUpdatePwd">修改密码</el-menu-item>
                <el-menu-item index="/user-submenu/logout" @click="handleLogout">退出登录</el-menu-item>
              </template>
            </el-sub-menu>
          </div>
          <div class="nav-right" v-else>
            <el-button type="primary" @click="router.replace('/login')">登录</el-button>
            <el-button type="success" @click="router.replace('/register')">注册</el-button>
          </div>
        </el-menu>
        <!-- 修改个人信息对话框 -->
        <el-dialog title="修改个人信息" v-model="updateProfileDiaVisible" @close="handleUpdateProfileDiaCancel">
          <el-form ref="updateProfileForm" :model="updateProfileFormData" :rules="updateProfileRules"
            label-width="120px">
            <el-form-item label="用户编号:" prop="id">
              <el-input v-model="updateProfileFormData.id" disabled></el-input>
            </el-form-item>
            <el-form-item label="用户名:" prop="username">
              <el-input v-model="updateProfileFormData.username" disabled></el-input>
            </el-form-item>
            <el-form-item label="照片:" prop="avatarPath">
              <FileUpload :file-list="uploadInfo.fileList" @file-list-change="handleFileListChange" ref="fileUploadRef">
              </FileUpload>
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
              <el-input v-model="updatePwdFormData.newPwd" type="password" placeholder="请输入新密码"
                show-password></el-input>
            </el-form-item>
          </el-form>
          <div slot="footer" class="dialog-footer">
            <el-button @click="handleUpdatePwdDiaCancel()">取消</el-button>
            <el-button type="primary" @click="updatePwd()">确定</el-button>
          </div>
        </el-dialog>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name='CommonHeader' lang='ts'>
import { getImage } from '@/util/AssetsImageUtil';
import { useUserStore } from '@/store/useUserStore';
import { Document, Files } from "@element-plus/icons-vue"
import { clearAll } from '@/util/storageUtil';
import { ref, reactive, computed } from 'vue';
import { ElMessage, type UploadUserFile } from 'element-plus';
import { modifyUserByIdApi, queryUserByIdApi, updatePwdApi } from '@/api/userApi';
import { useRoute, useRouter } from 'vue-router';
import FileUpload from '@/components/FileUpload.vue';
import type { AnyObjsDefine } from '@/types/common';
import { useResource } from '@/hooks/useResource';

let updateProfileForm = ref()
let updatePwdForm = ref()
let updatePwdDiaVisible = ref(false)
// 控制对话框是否可见
let updateProfileDiaVisible = ref(false)
let updatePwdFormData = reactive({
  id: '',
  originalPwd: '',
  newPwd: '',
})
let updateProfileRules = reactive({
})
let updatePwdRules = reactive({
  originalPwd: [
    { required: true, message: "请输入原始密码", trigger: "blur" },
  ],
  newPwd: [
    { required: true, message: "请输入新密码", trigger: "blur" },
  ]
})
let updateProfileFormData = reactive({
  id: '',
  username: '',
  avatarPath: ''
})
let router = useRouter()
let route = useRoute()
let userStore = useUserStore()
// 上传文件fileList
let uploadInfo = reactive<{
  fileList: UploadUserFile[]
}>({ fileList: [] })

let {toAddressable} = useResource()

function handleLogout() {
  // 退出登录
  clearAll()
  // 重置所有pinia的状态,避免刷新前在app中又存到storage中了
  userStore.resetAll()
  // 刷新当前页面相当于清空了pinia中的状态数据
  window.location.reload()
}
function handleSelect(index: string) {
  if (!index) return
  router.push(index);
}
let activeItem = computed<string>(() => {
  return route.path
})
// 是否登录
let hasLogin = computed<boolean>(() => {
  let token = userStore.token
  if (token) {
    return true;
  } else {
    return false;
  }
})
// 处理下拉框点击
function openUpdateProfileDialog(id: string) {
  // 不然就累加了
  uploadInfo.fileList = []
  // 查询用户信息回显
  queryUserByIdApi(id).then(result => {
    Object.assign(updateProfileFormData, result.data)
    // 设置图片
    let addressablePath = toAddressable(result.data.avatarPath)
    if (addressablePath) {
      uploadInfo.fileList.push({ url: addressablePath, name: '' })
    }
  })
  updateProfileDiaVisible.value = true
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
// 提交修改个人信息
function updateProfile() {
  updateProfileForm.value.validate((valid: boolean) => {
    if (!valid) return
    modifyUserByIdApi(updateProfileFormData).then(result => {
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
    updatePwdApi(updatePwdFormData).then(result => {
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
.common-header {
  height: 72px;
  background: rgba(5, 12, 32, 0.76);
  border-bottom: 1px solid var(--space-border);
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(18px);
}

.brand-shell {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 16px 10px 10px;
  border: 1px solid rgba(130, 210, 255, 0.22);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(11, 22, 52, 0.92), rgba(18, 32, 72, 0.7));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.03), 0 14px 32px rgba(0, 0, 0, 0.28);
}

.brand-mark {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background:
    radial-gradient(circle at 34% 30%, rgba(255, 255, 255, 0.98), rgba(100, 232, 255, 0.95) 14%, rgba(124, 92, 255, 0.85) 48%, rgba(13, 23, 52, 0.92) 76%);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.12),
    0 10px 26px rgba(72, 214, 255, 0.22);
}

.brand-mark::before,
.brand-mark::after {
  content: "";
  position: absolute;
  border-radius: 999px;
}

.brand-mark::before {
  width: 12px;
  height: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 0 16px rgba(255, 255, 255, 0.55);
}

.brand-mark::after {
  width: 26px;
  height: 26px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  opacity: 0.72;
}

.brand-copy {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 42px;
}

.brand-title {
  color: #fff;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.03em;
  line-height: 1;
}

.brand-divider {
  width: 1px;
  height: 18px;
  background: rgba(130, 210, 255, 0.26);
}

.brand-subtitle {
  color: var(--space-muted);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  line-height: 1;
}

:deep(.el-menu--horizontal) {
  height: 72px;
  justify-content: space-between;
  align-items: center;
  border-bottom: 0;
}

.el-menu {
  background: transparent;
}

:deep(.el-menu--horizontal > .el-menu-item),
:deep(.el-menu--horizontal > .el-sub-menu .el-sub-menu__title) {
  display: flex;
  align-items: center;
  height: 72px;
  line-height: normal;
}

:deep(.el-menu--horizontal > .el-menu-item:first-child),
:deep(.el-menu--horizontal > .el-menu-item:nth-child(5)) {
  border-bottom: none;
  text-decoration: none;
}

:deep(.el-menu--horizontal > .el-menu-item.brand-menu-item) {
  padding: 0 12px 0 0;
}

:deep(.el-menu--horizontal > .el-menu-item:first-child:hover),
:deep(.el-menu--horizontal > .el-menu-item:nth-child(5):hover) {
  background: transparent !important;
  box-shadow: none;
}

.header-search-form {
  width: 400px;
}

.header-search-form .el-form-item {
  margin-bottom: 0;
}

.nav-left,
.nav-right,
.avatar {
  display: flex;
  align-items: center;
}

.nav-left {
  gap: 8px;
}

.nav-right {
  gap: 10px;
}

.avatar {
  justify-content: center;
}

.greet {
  margin-left: 10px;
  color: var(--space-text);
}
</style>
