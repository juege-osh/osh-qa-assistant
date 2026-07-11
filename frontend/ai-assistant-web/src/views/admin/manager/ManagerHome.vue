<template>
  <div class="admin-home" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
    <div class="admin-grid">
      <SideBar
        :collapsed="isSidebarCollapsed"
        title="管理控制台"
        description="查看账号、用户与平台资源。"
        @update:collapsed="handleSidebarCollapsedChange"
      />
      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup name="ManagerHome" lang="ts">
import { computed, ref, watch } from 'vue'
import SideBar from '@/components/SideBar.vue'
import { useUserStore } from '@/store/useUserStore'
import { getItem, saveItem } from '@/util/storageUtil'

const userStore = useUserStore()
const sidebarStorageKey = computed(() => `admin.sidebar.expanded.${userStore.userInfo.id || 'default'}`)
const isSidebarCollapsed = ref(false)

function handleSidebarCollapsedChange(collapsed: boolean) {
  isSidebarCollapsed.value = collapsed
  saveItem(sidebarStorageKey.value, String(!collapsed))
}

watch(sidebarStorageKey, (key) => {
  const savedValue = getItem(key)
  isSidebarCollapsed.value = savedValue === null ? false : savedValue === 'false'
}, {
  immediate: true
})
</script>

<style scoped>
.admin-home {
  --sidebar-expanded-width: 268px;
  --sidebar-collapsed-width: 92px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.admin-grid {
  display: grid;
  grid-template-columns: var(--sidebar-expanded-width) minmax(0, 1fr);
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.admin-home.sidebar-collapsed .admin-grid {
  grid-template-columns: var(--sidebar-collapsed-width) minmax(0, 1fr);
}

.admin-main {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border-radius: 22px;
}

@media (max-width: 1200px) {
  .admin-grid {
    grid-template-columns: 240px minmax(0, 1fr);
  }
}

@media (max-width: 992px) {
  .admin-home {
    overflow: auto;
  }

  .admin-grid,
  .admin-home.sidebar-collapsed .admin-grid {
    grid-template-columns: 1fr;
    gap: 12px;
    height: auto;
    min-height: 100%;
  }

  .admin-main {
    min-height: 0;
    overflow: visible;
  }

  .admin-home :deep(.side-bar),
  .admin-home :deep(.side-bar.is-collapsed) {
    height: auto;
    min-height: 0;
    padding: 12px;
  }

  .admin-home :deep(.side-content),
  .admin-home :deep(.menu-scroll) {
    overflow: visible;
  }

  .admin-home :deep(.side-head) {
    align-items: center;
    padding: 2px 2px 10px;
    margin-bottom: 0;
  }

  .admin-home :deep(.side-desc),
  .admin-home :deep(.collapse-toggle),
  .admin-home :deep(.user-card),
  .admin-home :deep(.user-card-collapsed) {
    display: none;
  }

  .admin-home :deep(.workspace-menu-scroll .el-scrollbar__wrap) {
    overflow: visible;
  }

  .admin-home :deep(.workspace-menu-scroll .el-scrollbar__bar) {
    display: none;
  }

  .admin-home :deep(.workspace-menu-scroll .el-scrollbar__view) {
    min-height: 0;
    padding-right: 0;
  }

  .admin-home :deep(.el-menu),
  .admin-home :deep(.el-menu--collapse) {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
    margin-top: 0;
  }

  .admin-home :deep(.el-menu-item),
  .admin-home :deep(.el-menu--collapse .el-menu-item) {
    justify-content: flex-start;
    width: 100%;
    padding: 0 12px !important;
  }

  .admin-home :deep(.el-menu--collapse .el-menu-item .el-icon) {
    margin-right: 10px !important;
  }

  .admin-home :deep(.side-bar.is-collapsed .workspace-menu-node__label) {
    display: block;
  }
}
</style>
