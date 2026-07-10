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
                  popper-class="workspace-user-menu-popper"
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
                      @click="openUserProfileDialog"
                    >修改个人信息</el-dropdown-item>
                    <el-dropdown-item
                      v-if="isWorkspaceUser"
                      @click="openUserPasswordDialog"
                    >修改密码</el-dropdown-item>
                    <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <div class="nav-right nav-right-public" v-else-if="!isAuthPage">
              <div class="nav-auth-actions">
                <el-button class="nav-auth-btn nav-auth-btn--ghost" @click="router.replace('/register')">注册</el-button>
                <el-button type="primary" class="nav-auth-btn nav-auth-btn--primary" @click="router.replace('/login')">登录</el-button>
              </div>
            </div>
          </el-menu>
        </div>
        <UserAccountDialogs ref="accountDialogsRef" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup name='CommonHeader' lang='ts'>
import { getImage } from '@/util/AssetsImageUtil';
import { useUserStore } from '@/store/useUserStore';
import { Avatar, ChatDotRound, Document, Memo, User } from "@element-plus/icons-vue"
import { clearAll } from '@/util/storageUtil';
import { ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useResource } from '@/hooks/useResource';
import UserAccountDialogs from '@/components/UserAccountDialogs.vue';
import type { UserAccountDialogsExpose } from '@/components/UserAccountDialogs.vue';

let router = useRouter()
let route = useRoute()
let userStore = useUserStore()
const accountDialogsRef = ref<UserAccountDialogsExpose>()
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

function openUserProfileDialog() {
  accountDialogsRef.value?.openUpdateProfileDialog()
}

function openUserPasswordDialog() {
  accountDialogsRef.value?.openUpdatePwd()
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
</script>

<style src="@/assets/css/nav-header.css"></style>
