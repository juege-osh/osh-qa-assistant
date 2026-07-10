<template>
  <div class="side-bar">
    <div class="side-head">
      <div class="side-eyebrow">Workspace</div>
      <div class="side-title">助手工作台</div>
      <div class="side-desc">统一管理账号、应用、知识库与问答记录。</div>
    </div>
    <!-- 菜单列表 -->
    <el-scrollbar>
      <!-- collapse:是否水平折叠收起菜单 -->
      <el-menu @select="handleSelect" :default-active="defaultActivePath" :collapse="false"
        :collapse-transition="false" active-text-color="#ffd04b">
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
// 刷新页面/路由变更时应该激活哪个菜单项
let defaultActivePath = ref('')
// 菜单列表
let menuList = reactive<MenuItemsDefine>([])

// 监听
watch(() => currentRoute.path, (newValue,oldValue) => {
  defaultActivePath.value = resolveActiveMenuPath()
},{
  immediate:true
})

function resolveActiveMenuPath() {
  const activeMenuPath = currentRoute.meta?.activeMenuPath
  if (typeof activeMenuPath === 'string' && activeMenuPath) {
    return activeMenuPath
  }
  return currentRoute.path
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
.side-bar {
  height: 100%;
  padding: 18px 14px 14px;
  border: 1px solid rgba(227, 232, 241, 0.7);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: 0 18px 36px rgba(37, 48, 71, 0.07);
}

.side-head {
  padding: 6px 10px 16px;
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

.el-menu {
  margin-top: 8px;
  border: 0;
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

:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:focus),
:deep(.el-menu-item:hover),
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.12), rgba(64, 158, 255, 0.04)) !important;
  color: var(--space-primary) !important;
  box-shadow: inset 3px 0 0 var(--space-primary);
}
</style>
