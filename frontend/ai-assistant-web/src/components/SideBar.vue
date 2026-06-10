<template>
  <div class="side-bar" :class="{ 'is-collapsed': collapsed }">
    <div class="side-head">
      <div class="side-copy" v-if="!collapsed">
        <div class="side-eyebrow">Workspace</div>
        <div class="side-title">助手工作台</div>
        <div class="side-desc">统一管理账号、应用、知识库与问答记录。</div>
      </div>
      <button
        class="collapse-toggle"
        type="button"
        :aria-label="collapsed ? '展开工作台导航' : '折叠工作台导航'"
        @click="emit('update:collapsed', !collapsed)"
      >
        <el-icon>
          <component :is="collapsed ? Expand : Fold" />
        </el-icon>
      </button>
    </div>
    <!-- 菜单列表 -->
    <el-scrollbar>
      <!-- collapse:是否水平折叠收起菜单 -->
      <el-menu @select="handleSelect" :default-active="defaultActivePath" :collapse="collapsed"
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
import { Expand, Fold } from '@element-plus/icons-vue';

defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  'update:collapsed': [collapsed: boolean]
}>()

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
  padding: 18px 14px 14px;
  border: 1px solid rgba(227, 232, 241, 0.7);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: 0 18px 36px rgba(37, 48, 71, 0.07);
  transition: padding .22s ease, border-color .22s ease, box-shadow .22s ease;
}

.side-bar.is-collapsed {
  padding-left: 10px;
  padding-right: 10px;
}

.side-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 6px 10px 16px;
}

.side-copy {
  min-width: 0;
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

.collapse-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border: 1px solid rgba(217, 224, 236, 0.9);
  border-radius: 12px;
  background: rgba(248, 251, 255, 0.92);
  color: var(--space-text-soft);
  cursor: pointer;
  transition: background .2s ease, color .2s ease, border-color .2s ease, transform .2s ease;
}

.collapse-toggle:hover {
  border-color: rgba(64, 158, 255, 0.24);
  background: rgba(64, 158, 255, 0.08);
  color: var(--space-primary);
  transform: translateY(-1px);
}

.el-menu {
  margin-top: 8px;
  border: 0;
  background: transparent;
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

:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:focus),
:deep(.el-menu-item:hover),
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.12), rgba(64, 158, 255, 0.04)) !important;
  color: var(--space-primary) !important;
  box-shadow: inset 3px 0 0 var(--space-primary);
}

.side-bar.is-collapsed :deep(.el-sub-menu__title:hover),
.side-bar.is-collapsed :deep(.el-menu-item:focus),
.side-bar.is-collapsed :deep(.el-menu-item:hover),
.side-bar.is-collapsed :deep(.el-menu-item.is-active) {
  box-shadow: none;
}

@media (max-width: 992px) {
  .side-head {
    padding-bottom: 12px;
  }
}
</style>
