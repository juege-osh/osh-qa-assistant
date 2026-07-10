<template>
  <div class="personal-home" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
    <div class="personal-grid">
      <side-bar
        :collapsed="isSidebarCollapsed"
        @update:collapsed="handleSidebarCollapsedChange"
      ></side-bar>
      <div class="personal-main">
        <router-view></router-view>
      </div>
    </div>
  </div>
</template>
<script setup name='PersonalHome' lang='ts'>
import SideBar from '@/components/SideBar.vue';
import { computed, ref, watch } from 'vue';
import { useUserStore } from '@/store/useUserStore';
import { getItem, saveItem } from '@/util/storageUtil';

const userStore = useUserStore()
const sidebarStorageKey = computed(() => `workspace.sidebar.expanded.${userStore.userInfo.id || 'default'}`)
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
.personal-home {
  --sidebar-expanded-width: 268px;
  --sidebar-collapsed-width: 92px;
  height: 100%;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.personal-grid {
  height: 100%;
  display: grid;
  grid-template-columns: var(--sidebar-expanded-width) minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
}

.personal-home.sidebar-collapsed .personal-grid {
  grid-template-columns: var(--sidebar-collapsed-width) minmax(0, 1fr);
}

.personal-main {
  min-width: 0;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 22px;
}

@media (max-width: 1200px) {
  .personal-grid {
    grid-template-columns: 240px minmax(0, 1fr);
  }
}

@media (max-width: 992px) {
  .personal-grid,
  .personal-home.sidebar-collapsed .personal-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .personal-home {
    background: transparent;
  }
}
</style>
