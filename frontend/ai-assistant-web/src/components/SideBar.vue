<template>
  <div class="side-bar" :class="{ 'is-collapsed': collapsed }">
    <div class="side-content">
      <div class="side-head">
        <div class="side-copy" v-if="!collapsed">
          <div class="side-title">{{ title }}</div>
          <div class="side-desc">{{ description }}</div>
        </div>
        <button
          class="collapse-toggle"
          type="button"
          :aria-label="collapsed ? `展开${title}导航` : `折叠${title}导航`"
          @click="emit('update:collapsed', !collapsed)"
        >
          <el-icon>
            <component :is="collapsed ? Expand : Fold" />
          </el-icon>
        </button>
      </div>
      <!-- 菜单列表 -->
      <el-scrollbar class="menu-scroll workspace-menu-scroll">
        <!-- collapse:是否水平折叠收起菜单 -->
        <el-menu :key="defaultActivePath" @select="handleSelect" :default-active="defaultActivePath" :collapse="collapsed"
          :collapse-transition="false" active-text-color="#635bff">
          <template v-for="menu in menuList" :key="menu.path">
            <MenuItem :item="menu">
            </MenuItem>
          </template>
        </el-menu>
      </el-scrollbar>
    </div>

    <!-- 用户信息卡片 -->
    <div class="user-card" v-if="!collapsed">
      <el-dropdown trigger="click" placement="top-start" popper-class="workspace-user-menu-popper">
        <div class="user-trigger">
          <div class="user-avatar">
            <el-avatar v-if="userStore.userInfo.avatarPath" :src="toAddressable(userStore.userInfo.avatarPath)" :size="38"></el-avatar>
            <el-avatar v-else :size="38">{{ userStore.userInfo.username?.charAt(0).toUpperCase() }}</el-avatar>
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
            <el-dropdown-item v-if="isWorkspaceUser" @click="openUserProfileDialog">
              <el-icon><User /></el-icon>
              修改个人信息
            </el-dropdown-item>
            <el-dropdown-item v-if="isWorkspaceUser" @click="openUserPasswordDialog">
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
      <el-dropdown trigger="click" placement="right" popper-class="workspace-user-menu-popper">
        <div class="user-avatar-only">
          <el-avatar v-if="userStore.userInfo.avatarPath" :src="toAddressable(userStore.userInfo.avatarPath)" :size="34"></el-avatar>
          <el-avatar v-else :size="34">{{ userStore.userInfo.username?.charAt(0).toUpperCase() }}</el-avatar>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="isWorkspaceUser" @click="openUserProfileDialog">
              <el-icon><User /></el-icon>
              修改个人信息
            </el-dropdown-item>
            <el-dropdown-item v-if="isWorkspaceUser" @click="openUserPasswordDialog">
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

    <UserAccountDialogs ref="accountDialogsRef" />
  </div>
</template>
<script setup name='SideBar' lang='ts'>
import { reactive, ref, onMounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router';
import MenuItem from '@/components/MenuItem.vue'
import type { MenuItemDefine, MenuItemsDefine, UserRoutesDefine } from '@/types/common.d.ts';
import { useUserStore } from '@/store/useUserStore';
import { Expand, Fold, User, Lock, SwitchButton, ArrowUp } from '@element-plus/icons-vue';
import { clearAll } from '@/util/storageUtil';
import { useResource } from '@/hooks/useResource';
import UserAccountDialogs from '@/components/UserAccountDialogs.vue'
import type { UserAccountDialogsExpose } from '@/components/UserAccountDialogs.vue'

withDefaults(defineProps<{
  collapsed: boolean
  title?: string
  description?: string
}>(), {
  title: '助手工作台',
  description: '统一管理账号、应用、知识库与问答记录。'
})

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
const accountDialogsRef = ref<UserAccountDialogsExpose>()

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

function openUserProfileDialog() {
  accountDialogsRef.value?.openUpdateProfileDialog()
}

function openUserPasswordDialog() {
  accountDialogsRef.value?.openUpdatePwd()
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
  position: relative;
  height: 100%;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 18px;
  border: 1px solid var(--space-border);
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 251, 255, 0.96) 100%);
  box-shadow: var(--space-card-shadow);
  transition: padding .22s ease, border-color .22s ease, box-shadow .22s ease;
}

.side-bar::before {
  content: "";
  position: absolute;
  inset: 0 auto auto 0;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(210, 221, 244, 0.46), transparent 68%);
  pointer-events: none;
}

.side-bar.is-collapsed {
  padding-left: 10px;
  padding-right: 10px;
}

.side-content {
  position: relative;
  z-index: 1;
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
  padding: 4px 2px 18px;
  margin-bottom: 6px;
  border-bottom: 1px solid rgba(230, 235, 242, 0.88);
  flex-shrink: 0;
}

.side-copy {
  min-width: 0;
}

.side-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--space-text);
}

.side-desc {
  margin-top: 6px;
  color: var(--space-text-soft);
  line-height: 1.65;
  font-size: 13px;
}

.collapse-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border: 1px solid var(--space-border);
  border-radius: 12px;
  background: #ffffff;
  color: var(--space-text-soft);
  cursor: pointer;
  transition: background .2s ease, color .2s ease, border-color .2s ease, transform .2s ease;
}

.collapse-toggle:hover {
  border-color: rgba(99, 91, 255, 0.2);
  background: rgba(239, 241, 255, 0.92);
  color: var(--space-primary);
  transform: translateY(-1px);
}

.collapse-toggle :deep(.el-icon) {
  width: var(--space-icon-lg);
  height: var(--space-icon-lg);
  font-size: var(--space-icon-lg);
}

.menu-scroll {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding-top: 6px;
}

.workspace-menu-scroll :deep(.el-scrollbar__view) {
  min-height: 100%;
  padding-right: 2px;
}

.workspace-menu-scroll :deep(.el-scrollbar__bar.is-vertical) {
  width: 8px;
  right: -2px;
  opacity: 1;
  background: rgba(226, 232, 240, 0.42);
  border-radius: 999px;
}

.workspace-menu-scroll :deep(.el-scrollbar__thumb) {
  opacity: 1;
  border-radius: 999px;
  background: rgba(126, 138, 161, 0.82);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.46);
}

.el-menu {
  margin-top: 4px;
  border: 0;
  background: transparent;
  display: grid;
  gap: 4px;
}

.el-menu div,
:deep(.el-sub-menu ul.el-menu div) {
  border-bottom: 0;
}

.el-menu div:last-child,
:deep(.el-sub-menu ul.el-menu div:last-child) {
  border-bottom: 0;
}

:deep(.el-sub-menu__title),
:deep(.el-menu-item) {
  height: 46px;
  margin: 0;
  padding-left: 14px !important;
  padding-right: 14px !important;
  font-weight: 600;
  border-radius: 14px;
  background: transparent !important;
  color: var(--space-text-soft);
  transition: background .18s ease, color .18s ease, box-shadow .18s ease, border-color .18s ease, transform .18s ease;
}

.workspace-menu-scroll :deep(.el-sub-menu__title .workspace-menu-node__label),
.workspace-menu-scroll :deep(.el-menu-item .workspace-menu-node__label) {
  font-size: 14px;
  line-height: 1.4;
}

.workspace-menu-scroll :deep(.el-sub-menu__title .workspace-menu-node__icon),
.workspace-menu-scroll :deep(.el-menu-item .workspace-menu-node__icon) {
  color: rgba(98, 115, 144, 0.88);
  transition: color .18s ease, transform .18s ease;
}

.workspace-menu-scroll :deep(.el-sub-menu__title .workspace-menu-node__dot),
.workspace-menu-scroll :deep(.el-menu-item .workspace-menu-node__dot) {
  transition: background .18s ease, transform .18s ease;
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

.side-bar.is-collapsed :deep(.workspace-menu-node__label) {
  display: none;
}

.side-bar.is-collapsed :deep(.workspace-menu-node__main) {
  justify-content: center;
  gap: 0;
}

:deep(.el-sub-menu .el-menu) {
  margin: 6px 0 10px 12px;
  padding: 8px;
  border: 1px solid rgba(223, 230, 241, 0.9);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.96), rgba(246, 249, 255, 0.92));
}

:deep(.el-sub-menu .el-menu .el-menu-item) {
  height: 42px;
  padding-left: 12px !important;
  padding-right: 12px !important;
  border-radius: 12px;
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: var(--space-primary) !important;
  background: linear-gradient(90deg, rgba(239, 241, 255, 0.96), rgba(245, 247, 255, 0.82)) !important;
  box-shadow: inset 2px 0 0 var(--space-primary);
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title .workspace-menu-node__icon),
:deep(.el-menu-item.is-active .workspace-menu-node__icon),
:deep(.el-sub-menu__title:hover .workspace-menu-node__icon),
:deep(.el-menu-item:hover .workspace-menu-node__icon) {
  color: var(--space-primary) !important;
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title .workspace-menu-node__dot),
:deep(.el-menu-item.is-active .workspace-menu-node__dot),
:deep(.el-sub-menu__title:hover .workspace-menu-node__dot),
:deep(.el-menu-item:hover .workspace-menu-node__dot) {
  background: rgba(99, 91, 255, 0.78);
  transform: scale(1.05);
}

:deep(.el-sub-menu__icon-arrow) {
  margin-top: 0;
  margin-right: 2px;
  color: rgba(98, 115, 144, 0.78);
  font-size: 13px;
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title .el-sub-menu__icon-arrow) {
  color: var(--space-primary);
}

:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:focus),
:deep(.el-menu-item:hover),
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(239, 241, 255, 1), rgba(245, 247, 255, 0.82)) !important;
  color: var(--space-primary) !important;
  box-shadow: inset 2px 0 0 var(--space-primary);
}

.workspace-menu-scroll :deep(.el-menu-item:focus),
.workspace-menu-scroll :deep(.el-sub-menu__title:focus-visible) {
  outline: none;
}

.side-bar.is-collapsed :deep(.el-sub-menu__title:hover),
.side-bar.is-collapsed :deep(.el-menu-item:focus),
.side-bar.is-collapsed :deep(.el-menu-item:hover),
.side-bar.is-collapsed :deep(.el-menu-item.is-active) {
  box-shadow: none;
}

.side-bar.is-collapsed :deep(.el-sub-menu .el-menu) {
  margin-left: 0;
}

/* 用户卡片样式 */
.user-card {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  margin-top: 16px;
  padding: 14px;
  border: 1px solid var(--space-border);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(251, 252, 255, 0.98) 100%);
  box-shadow: none;
  transition: all 0.2s ease;
}

.user-card:hover {
  border-color: var(--space-border-strong);
  box-shadow: var(--space-shadow);
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

.user-arrow {
  width: var(--space-icon-sm);
  height: var(--space-icon-sm);
  font-size: var(--space-icon-sm);
  color: var(--space-text-soft);
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
  background: var(--space-success);
  box-shadow: 0 0 0 2px rgba(18, 183, 106, 0.14);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 2px rgba(18, 183, 106, 0.14);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(18, 183, 106, 0.08);
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
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.user-avatar-only {
  cursor: pointer;
  border: 2px solid rgba(230, 235, 242, 0.92);
  border-radius: 50%;
  transition: all 0.2s ease;
}

.user-avatar-only:hover {
  border-color: rgba(99, 91, 255, 0.34);
  transform: scale(1.05);
  box-shadow: 0 10px 18px rgba(99, 91, 255, 0.12);
}

@media (max-width: 992px) {
  .side-head {
    padding-bottom: 12px;
  }
}
</style>
