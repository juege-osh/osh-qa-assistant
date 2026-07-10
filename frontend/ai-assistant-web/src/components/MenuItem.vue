<template>
  <div class="workspace-menu-node">
    <el-sub-menu v-if="item.children && item.children.length > 0"
                :index="item.path"
                class="workspace-menu-node__branch">
      <template v-slot:title>
        <div class="workspace-menu-node__title">
          <div class="workspace-menu-node__main">
            <el-icon v-if="item.icon" class="workspace-menu-node__icon"><component :is="item.icon"></component></el-icon>
            <span v-else class="workspace-menu-node__dot" aria-hidden="true"></span>
            <span class="workspace-menu-node__label">{{ item.menuName }}</span>
          </div>
        </div>
      </template>
      <!-- 递归遍历,此写法支持n级菜单 -->
      <template v-for="child in item.children" :key="child.path">
        <menu-item :item="child"></menu-item>
      </template>
    </el-sub-menu>
    <el-menu-item v-else
                  :index="item.path"
                  class="workspace-menu-node__leaf">
      <div class="workspace-menu-node__title">
        <div class="workspace-menu-node__main">
          <el-icon v-if="item.icon" class="workspace-menu-node__icon"><component :is="item.icon"></component></el-icon>
          <span v-else class="workspace-menu-node__dot" aria-hidden="true"></span>
          <span class="workspace-menu-node__label">{{ item.menuName }}</span>
        </div>
      </div>
    </el-menu-item>
  </div>
</template>
<script setup name='MenuItem' lang='ts'>
import type { MenuItemDefine } from '@/types/common.d.ts';
defineProps<{
  item: MenuItemDefine
}>()
</script>
<style scoped>
.workspace-menu-node {
  min-width: 0;
}

.workspace-menu-node__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-width: 0;
}

.workspace-menu-node__main {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  width: 100%;
}

.workspace-menu-node__icon {
  flex-shrink: 0;
}

.workspace-menu-node__label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-menu-node__dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.72);
  flex-shrink: 0;
  margin-left: 4px;
  margin-right: 6px;
}
</style>
