<template>
  <div class="side-bar">
    <div class="side-head" v-show="!isCollapsed">
      <div class="side-eyebrow">Control</div>
      <div class="side-title">管理后台</div>
      <div class="side-desc">管理用户、知识库、文档、应用与调用记录。</div>
    </div>
    <div class="collapse">
      <span @click="toggleCollapse" v-show="isCollapsed">
        <el-icon>
          <Expand />
        </el-icon>
      </span>
      <span @click="toggleCollapse" v-show="!isCollapsed">
        <el-icon>
          <Fold />
        </el-icon>
      </span>
    </div>
    <!-- 菜单列表 -->
    <el-scrollbar>
      <!-- collapse:是否水平折叠收起菜单 -->
      <el-menu @select="handleSelect" :default-active="defaultActivePath" :collapse="isCollapsed"
        :collapse-transition="false" text-color="#fff" active-text-color="#ffd04b">
        <template v-for="menu in menuList" :key="menu.path">
          <MenuItem :item="menu">
          </MenuItem>
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>
<script setup name='SideBar' lang='ts'>
import { reactive, ref, onMounted,watch } from 'vue'
import { useRouter,useRoute } from 'vue-router';
import MenuItem from '@/components/MenuItem.vue'
import type { MenuItemDefine, MenuItemsDefine, UserRoutesDefine } from '@/types/common.d.ts';
import { useUserStore } from '@/store/useUserStore';

let router = useRouter()
let currentRoute = useRoute()
let userStore = useUserStore()
// isCollapsed=true:已折叠 isCollapsed=false:已展开
let isCollapsed = ref(false)
// 刷新页面/路由变更时应该激活哪个菜单项
let defaultActivePath = ref('')
// 菜单列表
let menuList = reactive<MenuItemsDefine>([])

// 监听
watch(() => currentRoute.path, (newValue,oldValue) => {
  defaultActivePath.value = newValue
},{
  immediate:true
})

let emitter = defineEmits(["update:asideWidth"])
function toggleCollapse() {
  // 修改el-aside的宽度
  if (isCollapsed.value) { // 折叠的状态
    emitter("update:asideWidth", "200px")
  } else {
    emitter("update:asideWidth", "65px")
  }
  isCollapsed.value = !isCollapsed.value
}
// 菜单项点击,index: 选中菜单项的 index,我们配置成菜单对应的路由路径
function handleSelect(index: string) {
  router.replace(index)
}

// 计算菜单列表
function calcMenuList() {
  doCalcMenuList(userStore.userInfo.userRoutes, '/')
}
function doCalcMenuList(originalRoutes: UserRoutesDefine, ancestorsPath: string,parentMenu?:MenuItemDefine) {
  originalRoutes.forEach(originalRoute => {
    let isMenu = originalRoute.meta && originalRoute.meta.showInMenu
    let hasChildren = originalRoute.children && originalRoute.children.length > 0
    if (!ancestorsPath.endsWith("/")) {
      // 连接多级路由
      ancestorsPath += '/'
    }
    // 拼接路由的全路径
    let routePath = `${ancestorsPath}${originalRoute.path}`
    if (isMenu) {
      let menu: MenuItemDefine = {
        path: routePath,
        menuName: originalRoute.meta?.authorityName || "",
        icon: originalRoute.meta?.icon || ""
      }
      if(hasChildren) {
        doCalcMenuList(originalRoute.children as UserRoutesDefine,routePath,menu)
      }
      if(parentMenu) {
        // 有父级就放到父级的children中
        if(parentMenu.children) {
          parentMenu.children.push(menu)
        }else {
          parentMenu.children = [menu]
        }
      }else {
        menuList.push(menu)
      }
    }else{
      if(hasChildren) {
        // 透传父级菜单
        doCalcMenuList(originalRoute.children as UserRoutesDefine,routePath,parentMenu)
      }
    }
  })
}
// 声明周期钩子
onMounted(() => {
  calcMenuList()
})
</script>
<style scoped>
.collapse {
  margin: 12px;
  padding: 10px;
  color: var(--space-muted);
  text-align: center;
  cursor: pointer;
  line-height: 1.5;
  border: 1px solid rgba(227, 232, 241, 0.8);
  border-radius: 12px;
  background: rgba(248, 251, 255, 0.96);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.collapse:hover {
  color: var(--space-primary);
  background: rgba(64, 158, 255, 0.08);
}

.side-head {
  padding: 20px 16px 10px;
}

.side-eyebrow {
  color: var(--space-primary);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
}

.side-title {
  margin-top: 10px;
  font-size: 17px;
  font-weight: 800;
  color: var(--space-text);
}

.side-desc {
  margin-top: 10px;
  color: var(--space-text-soft);
  line-height: 1.7;
  font-size: 12px;
}

.el-menu {
  padding: 12px;
  border-right-width: 0;
}

.side-bar ul,
.side-bar li,
:deep(.el-menu) {
  background: transparent !important;
}

:deep(.el-sub-menu__title),
:deep(.el-menu-item) {
  height: 44px;
  margin: 4px 0;
  padding-left: 14px !important;
  padding-right: 14px !important;
  letter-spacing: .02em;
  font-weight: 600;
}

:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:focus),
:deep(.el-menu-item:hover),
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.12), rgba(64, 158, 255, 0.04)) !important;
  color: var(--space-primary) !important;
  box-shadow: inset 3px 0 0 var(--space-primary);
}
</style>
