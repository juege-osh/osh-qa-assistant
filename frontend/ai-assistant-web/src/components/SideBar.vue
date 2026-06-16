<template>
  <div class="side-bar" :class="{ 'is-collapsed': collapsed }">
    <div class="side-content">
      <div class="side-head">
        <div class="side-copy" v-if="!collapsed">
          <div class="side-eyebrow">Workspace</div>
          <div class="side-title">助手工作台</div>
          <div class="side-desc">统一管理账号、应用、知识库与问答记录。</div>
        </div>
        <button
          class="collapse-toggle"
          type="button"
          :aria-label="collapsed ? '展开工作台导航' : '折叠工作台导航'"
          @click="emit('update:collapsed', !collapsed)"
        >
          <el-icon>
            <component :is="collapsed ? Expand : Fold" />
          </el-icon>
        </button>
      </div>
      <!-- 菜单列表 -->
      <el-scrollbar class="menu-scroll">
        <!-- collapse:是否水平折叠收起菜单 -->
        <el-menu @select="handleSelect" :default-active="defaultActivePath" :collapse="collapsed"
          :collapse-transition="false" active-text-color="#ffd04b">
          <template v-for="menu in menuList" :key="menu.path">
            <MenuItem :item="menu">
            </MenuItem>
          </template>
        </el-menu>
      </el-scrollbar>
    </div>

    <!-- 用户信息卡片 -->
    <div class="user-card" v-if="!collapsed">
      <el-dropdown trigger="click" placement="top-start" popper-class="user-dropdown-popper">
        <div class="user-trigger">
          <div class="user-avatar">
            <el-avatar v-if="userStore.userInfo.avatarPath" :src="toAddressable(userStore.userInfo.avatarPath)" :size="40"></el-avatar>
            <el-avatar v-else :size="40">{{ userStore.userInfo.username?.charAt(0).toUpperCase() }}</el-avatar>
          </div>
          <div class="user-info">
            <div class="user-name">你好，{{ userStore.userInfo.username }}</div>
            <div class="user-status">
              <span class="status-dot"></span>
              已登录
            </div>
          </div>
          <el-icon class="user-arrow">
            <ArrowUp />
          </el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="isWorkspaceUser" @click="openUpdateProfileDialog">
              <el-icon><User /></el-icon>
              修改个人信息
            </el-dropdown-item>
            <el-dropdown-item v-if="isWorkspaceUser" @click="openUpdatePwd">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 折叠状态的用户头像 -->
    <div class="user-card-collapsed" v-else>
      <el-dropdown trigger="click" placement="right" popper-class="user-dropdown-popper">
        <div class="user-avatar-only">
          <el-avatar v-if="userStore.userInfo.avatarPath" :src="toAddressable(userStore.userInfo.avatarPath)" :size="36"></el-avatar>
          <el-avatar v-else :size="36">{{ userStore.userInfo.username?.charAt(0).toUpperCase() }}</el-avatar>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="isWorkspaceUser" @click="openUpdateProfileDialog">
              <el-icon><User /></el-icon>
              修改个人信息
            </el-dropdown-item>
            <el-dropdown-item v-if="isWorkspaceUser" @click="openUpdatePwd">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 修改个人信息对话框 -->
    <el-dialog title="修改个人信息" v-model="updateProfileDiaVisible" @close="handleUpdateProfileDiaCancel" width="500px">
      <el-form ref="updateProfileForm" :model="updateProfileFormData" :rules="updateProfileRules" label-width="120px">
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
      </el-form>
      <template #footer>
        <el-button @click="updateProfileDiaVisible = false">取消</el-button>
        <el-button type="primary" @click="updateProfile()">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog title="修改密码" v-model="updatePwdDiaVisible" @close="handleUpdatePwdDiaCancel()" width="500px">
      <el-form ref="updatePwdForm" :model="updatePwdFormData" :rules="updatePwdRules" label-width="120px">
        <el-form-item label="原始密码" prop="originalPwd">
          <el-input v-model="updatePwdFormData.originalPwd" type="password" placeholder="请输入原始密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd">
          <el-input v-model="updatePwdFormData.newPwd" type="password" placeholder="请输入新密码" show-password></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleUpdatePwdDiaCancel()">取消</el-button>
        <el-button type="primary" @click="updatePwd()">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name='SideBar' lang='ts'>
import { reactive, ref, onMounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router';
import MenuItem from '@/components/MenuItem.vue'
import FileUpload from '@/components/FileUpload.vue'
import type { MenuItemDefine, MenuItemsDefine, UserRoutesDefine, AnyObjsDefine } from '@/types/common.d.ts';
import { useUserStore } from '@/store/useUserStore';
import { Expand, Fold, User, Lock, SwitchButton, ArrowUp } from '@element-plus/icons-vue';
import { clearAll } from '@/util/storageUtil';
import { ElMessage, type UploadUserFile } from 'element-plus';
import { modifyUserByIdApi, queryUserByIdApi, updatePwdApi } from '@/api/workspace/userApi';
import { useResource } from '@/hooks/useResource';

defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  'update:collapsed': [collapsed: boolean]
}>()

let router = useRouter()
let currentRoute = useRoute()
let userStore = useUserStore()
let { toAddressable } = useResource()

// 刷新页面/路由变更时应该激活哪个菜单项
let defaultActivePath = ref('')
// 菜单列表
let menuList = reactive<MenuItemsDefine>([])

// 用户相关状态
let updateProfileForm = ref()
let updatePwdForm = ref()
let updatePwdDiaVisible = ref(false)
let updateProfileDiaVisible = ref(false)
let fileUploadRef = ref()

let updatePwdFormData = reactive({
  id: '',
  originalPwd: '',
  newPwd: '',
})

let updateProfileRules = reactive({})

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

// 上传文件fileList
let uploadInfo = reactive<{
  fileList: UploadUserFile[]
}>({ fileList: [] })

let isWorkspaceUser = computed<boolean>(() => {
  return userStore.userInfo.role === 'USER'
})

// 监听
watch(() => currentRoute.path, (newValue, oldValue) => {
  defaultActivePath.value = resolveActiveMenuPath()
}, {
  immediate: true
})

function resolveActiveMenuPath() {
  const activeMenuPath = currentRoute.meta?.activeMenuPath
  if (typeof activeMenuPath === 'string' && activeMenuPath) {
    return activeMenuPath
  }
  return currentRoute.path
}

// 菜单项点击
function handleSelect(index: string) {
  router.replace(index)
}

// 退出登录
function handleLogout() {
  clearAll()
  userStore.resetAll()
  window.location.reload()
}

// 打开修改个人信息对话框
function openUpdateProfileDialog() {
  uploadInfo.fileList = []
  queryUserByIdApi(userStore.userInfo.id).then(result => {
    Object.assign(updateProfileFormData, result.data)
    let addressablePath = toAddressable(result.data.avatarPath)
    if (addressablePath) {
      uploadInfo.fileList.push({ url: addressablePath, name: '' })
    }
  })
  updateProfileDiaVisible.value = true
}

// 打开修改密码对话框
function openUpdatePwd() {
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
      userStore.$patch((state) => {
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
function handleFileListChange(uploadResultList: AnyObjsDefine) {
  if (uploadResultList && uploadResultList.length > 0) {
    updateProfileFormData.avatarPath = uploadResultList[0].relativePath
  } else {
    updateProfileFormData.avatarPath = ''
  }
  updateProfileForm.value.validateField('avatarPath');
}

// 计算菜单列表
function calcMenuList() {
  doCalcMenuList(userStore.userInfo.userRoutes, '/')
}

function doCalcMenuList(originalRoutes: UserRoutesDefine, ancestorsPath: string, parentMenu?: MenuItemDefine) {
  originalRoutes.forEach(originalRoute => {
    let isMenu = originalRoute.meta && originalRoute.meta.showInMenu
    let hasChildren = originalRoute.children && originalRoute.children.length > 0
    if (!ancestorsPath.endsWith("/")) {
      ancestorsPath += '/'
    }
    let routePath = `${ancestorsPath}${originalRoute.path}`
    if (isMenu) {
      let menu: MenuItemDefine = {
        path: routePath,
        menuName: originalRoute.meta?.authorityName || "",
        icon: originalRoute.meta?.icon || ""
      }
      if (hasChildren) {
        doCalcMenuList(originalRoute.children as UserRoutesDefine, routePath, menu)
      }
      if (parentMenu) {
        if (parentMenu.children) {
          parentMenu.children.push(menu)
        } else {
          parentMenu.children = [menu]
        }
      } else {
        menuList.push(menu)
      }
    } else {
      if (hasChildren) {
        doCalcMenuList(originalRoute.children as UserRoutesDefine, routePath, parentMenu)
      }
    }
  })
}

onMounted(() => {
  calcMenuList()
})
</script>
<style scoped>
.side-bar {
  height: 100%;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 18px 14px 14px;
  border: 1px solid rgba(227, 232, 241, 0.7);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: 0 18px 36px rgba(37, 48, 71, 0.07);
  transition: padding .22s ease, border-color .22s ease, box-shadow .22s ease;
}

.side-bar.is-collapsed {
  padding-left: 10px;
  padding-right: 10px;
}

.side-content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.side-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 6px 10px 16px;
  flex-shrink: 0;
}

.side-copy {
  min-width: 0;
}

.side-eyebrow {
  color: var(--space-primary);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.side-title {
  margin-top: 8px;
  font-size: 17px;
  font-weight: 800;
  color: var(--space-text);
}

.side-desc {
  margin-top: 8px;
  color: var(--space-text-soft);
  line-height: 1.7;
  font-size: 12px;
}

.collapse-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border: 1px solid rgba(217, 224, 236, 0.9);
  border-radius: 12px;
  background: rgba(248, 251, 255, 0.92);
  color: var(--space-text-soft);
  cursor: pointer;
  transition: background .2s ease, color .2s ease, border-color .2s ease, transform .2s ease;
}

.collapse-toggle:hover {
  border-color: rgba(64, 158, 255, 0.24);
  background: rgba(64, 158, 255, 0.08);
  color: var(--space-primary);
  transform: translateY(-1px);
}

.menu-scroll {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.el-menu {
  margin-top: 8px;
  border: 0;
  background: transparent;
}

.el-menu div,
:deep(.el-sub-menu ul.el-menu div) {
  border-bottom: 1px solid rgba(227, 232, 241, 0.7);
}

.el-menu div:last-child,
:deep(.el-sub-menu ul.el-menu div:last-child) {
  border-bottom: 0;
}

:deep(.el-sub-menu__title),
:deep(.el-menu-item) {
  height: 44px;
  margin: 4px 0;
  padding-left: 14px !important;
  padding-right: 14px !important;
  font-weight: 600;
}

:deep(.el-menu--collapse) {
  width: auto;
}

:deep(.el-menu--collapse .el-sub-menu__title),
:deep(.el-menu--collapse .el-menu-item) {
  justify-content: center;
  padding-left: 0 !important;
  padding-right: 0 !important;
}

:deep(.el-menu--collapse .el-sub-menu__title .el-icon),
:deep(.el-menu--collapse .el-menu-item .el-icon) {
  margin-right: 0 !important;
}

:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:focus),
:deep(.el-menu-item:hover),
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.12), rgba(64, 158, 255, 0.04)) !important;
  color: var(--space-primary) !important;
  box-shadow: inset 3px 0 0 var(--space-primary);
}

.side-bar.is-collapsed :deep(.el-sub-menu__title:hover),
.side-bar.is-collapsed :deep(.el-menu-item:focus),
.side-bar.is-collapsed :deep(.el-menu-item:hover),
.side-bar.is-collapsed :deep(.el-menu-item.is-active) {
  box-shadow: none;
}

/* 用户卡片样式 */
.user-card {
  flex-shrink: 0;
  margin-top: 16px;
  padding: 14px;
  border: 1px solid rgba(217, 224, 236, 0.6);
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 251, 255, 0.9) 100%);
  box-shadow: 0 2px 8px rgba(37, 48, 71, 0.06);
  transition: all 0.2s ease;
}

.user-card:hover {
  border-color: rgba(64, 158, 255, 0.3);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.12);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  width: 100%;
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--space-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-status {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--space-text-soft);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.1);
  }
}

.user-arrow {
  flex-shrink: 0;
  color: var(--space-text-soft);
  font-size: 14px;
  transition: transform 0.2s ease;
}

.user-trigger:hover .user-arrow {
  transform: translateY(-2px);
}

/* 折叠状态的用户头像 */
.user-card-collapsed {
  flex-shrink: 0;
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.user-avatar-only {
  cursor: pointer;
  border: 2px solid rgba(217, 224, 236, 0.6);
  border-radius: 50%;
  transition: all 0.2s ease;
}

.user-avatar-only:hover {
  border-color: rgba(64, 158, 255, 0.5);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

@media (max-width: 992px) {
  .side-head {
    padding-bottom: 12px;
  }
}
</style>
