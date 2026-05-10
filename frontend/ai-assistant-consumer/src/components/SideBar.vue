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
  defaultActivePath.value = newValue
},{
  immediate:true
})
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
  padding: 16px;
  border: 1px solid var(--space-border);
  border-radius: 24px;
  background:
    radial-gradient(circle at 12% 12%, rgba(100, 232, 255, 0.16), transparent 28%),
    linear-gradient(180deg, rgba(7, 14, 36, 0.88), rgba(10, 13, 32, 0.78));
  box-shadow: var(--space-card-shadow);
  backdrop-filter: blur(18px);
}

.side-head {
  padding: 6px 8px 16px;
}

.side-eyebrow {
  color: var(--space-primary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
}

.side-title {
  margin-top: 10px;
  font-size: 22px;
  font-weight: 800;
}

.side-desc {
  margin-top: 10px;
  color: var(--space-muted);
  line-height: 1.7;
  font-size: 13px;
}

.el-menu {
  margin-top: 6px;
  border: 0;
}

.el-menu div,
:deep(.el-sub-menu ul.el-menu div) {
  border-bottom: 1px solid rgba(130, 210, 255, 0.1);
}

.el-menu div:last-child,
:deep(.el-sub-menu ul.el-menu div:last-child) {
  border-bottom: 0;
}

:deep(.el-sub-menu__title),
:deep(.el-menu-item) {
  height: 46px;
  margin: 6px 0;
}

:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:focus),
:deep(.el-menu-item:hover),
:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(100, 232, 255, 0.2), rgba(139, 92, 246, 0.24)) !important;
  color: #fff !important;
}
</style>
