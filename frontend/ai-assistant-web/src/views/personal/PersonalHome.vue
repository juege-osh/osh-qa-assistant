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
  height: 100%;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.personal-grid {
  height: 100%;
  display: grid;
  grid-template-columns: minmax(250px, 0.8fr) minmax(0, 2.6fr);
  gap: 22px;
  align-items: stretch;
}

.personal-home.sidebar-collapsed .personal-grid {
  grid-template-columns: 96px minmax(0, 1fr);
}

.personal-main {
  min-width: 0;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

@media (max-width: 1200px) {
  .personal-grid {
    grid-template-columns: minmax(240px, 0.9fr) minmax(0, 2.2fr);
  }
}

@media (max-width: 992px) {
  .personal-grid,
  .personal-home.sidebar-collapsed .personal-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}
</style>
