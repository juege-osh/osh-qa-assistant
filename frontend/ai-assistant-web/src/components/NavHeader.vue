<template>
  <div class="common-header">
    <el-row>
      <el-col :span="24">
        <div class="header-shell">
          <el-menu
            :default-active="activeItem"
            mode="horizontal"
            :ellipsis="false"
            class="header-menu"
            @select="handleSelect"
          >
            <div class="nav-left">
              <el-menu-item index="/" class="brand-menu-item">
                <div class="brand-shell">
                  <div class="brand-mark" aria-hidden="true"></div>
                  <div class="brand-copy">
                    <div class="brand-title">OSH Wisdom</div>
                    <div class="brand-divider"></div>
                    <div class="brand-subtitle">智能知识平台</div>
                  </div>
                </div>
              </el-menu-item>

              <div v-if="navItems.length" class="nav-main">
                <el-menu-item
                  v-for="item in navItems"
                  :key="item.index"
                  :index="item.index"
                  class="nav-link-item"
                >
                  <el-icon>
                    <component :is="item.icon" />
                  </el-icon>
                  {{ item.label }}
                </el-menu-item>
              </div>
            </div>

            <div class="nav-right" v-if="hasLogin">
              <el-dropdown
                class="user-dropdown"
                trigger="click"
                placement="bottom-end"
                popper-class="header-user-popper"
              >
                <div class="user-trigger">
                  <div class="avatar">
                    <el-avatar
                      v-if="userStore.userInfo.avatarPath"
                      :src="toAddressable(userStore.userInfo.avatarPath)"
                    ></el-avatar>
                    <el-avatar v-else :src="getImage('user.png')"></el-avatar>
                    <div class="user-copy">
                      <div class="user-headline">
                        <div class="greet">你好，{{ userStore.userInfo.username }}</div>
                        <div class="user-state">
                          <span class="status-dot"></span>
                          已登录
                        </div>
                      </div>
                      <div class="user-role">{{ isAdmin ? '管理后台' : '工作台用户' }}</div>
                    </div>
                  </div>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-if="isWorkspaceUser"
                      @click="openUpdateProfileDialog(userStore.userInfo.id)"
                    >修改个人信息</el-dropdown-item>
                    <el-dropdown-item
                      v-if="isWorkspaceUser"
                      @click="openUpdatePwd"
                    >修改密码</el-dropdown-item>
                    <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <div class="nav-right nav-right-public" v-else-if="!isAuthPage">
              <el-button type="primary" @click="router.replace('/login')">登录</el-button>
              <el-button @click="router.replace('/register')">用户注册</el-button>
            </div>
          </el-menu>
        </div>
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
import { Avatar, ChatDotRound, Document, Memo, User } from "@element-plus/icons-vue"
import { clearAll } from '@/util/storageUtil';
import { ref, reactive, computed } from 'vue';
import { ElMessage, type UploadUserFile } from 'element-plus';
import { modifyUserByIdApi, queryUserByIdApi, updatePwdApi } from '@/api/workspace/userApi';
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
let isAdmin = computed<boolean>(() => {
  return userStore.userInfo.role === 'ADMIN'
})
let isWorkspaceUser = computed<boolean>(() => {
  return userStore.userInfo.role === 'USER'
})
// 是否在登录/注册页
let isAuthPage = computed<boolean>(() => {
  const authPaths = ['/login', '/register', '/forget-password']
  return authPaths.includes(route.path)
})
const navItems = computed(() => {
  const items = []
  if (isAdmin.value) {
    items.push(
      { index: '/admin/manager/manage', label: '管理台', icon: User },
      { index: '/admin/user/manage', label: '用户', icon: Avatar }
    )
  }
  if (isWorkspaceUser.value) {
    items.push(
      { index: '/workspace/app/manage', label: '应用', icon: ChatDotRound },
      { index: '/workspace/knowledgeLib/manage', label: '知识库', icon: Document }
    )
  }
  if (hasLogin.value) {
    items.push({ index: '/doc', label: '接口文档', icon: Memo })
  }
  return items
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

<style src="@/assets/css/nav-header.css"></style>
