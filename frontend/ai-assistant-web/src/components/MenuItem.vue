<template>
  <div>
    <el-sub-menu v-if="item.children && item.children.length > 0"
                :index="item.path">
      <template v-slot:title>
        <el-icon v-if="item.icon"><component :is="item.icon"></component></el-icon>
        <span>{{ item.menuName }}</span>
      </template>
      <!-- 递归遍历,此写法支持n级菜单 -->
      <template v-for="child in item.children" :key="child.path">
        <menu-item :item="child"></menu-item>
      </template>
    </el-sub-menu>
    <el-menu-item v-else
                  :index="item.path">
      <el-icon v-if="item.icon"><component :is="item.icon"></component></el-icon>
      <template v-slot:title>{{ item.menuName }}</template>
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
   
</style>