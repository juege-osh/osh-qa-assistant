<template>
  <div ref="tabs" class="workspace-tabs-shell">
    <!-- model-value:表示的是要激活name为activeTabName的tab-pane-->
    <el-tabs :model-value="activeTabName" @tab-click="tabClick" @contextmenu.prevent.native="openContextMenu"
      @tab-remove="closeTab">
      <el-tab-pane :name="item.path" v-for="item in openedTabs" :key="item.path" :label="item.text"
        :closable="true">
      </el-tab-pane>
    </el-tabs>

    <ul v-show="contextMenuVisible" :style="{ left: left + 'px', top: top + 'px' }" class="context-menu">
      <li class="context-menu__item" @click="refreshTab"><el-icon>
          <Refresh />
        </el-icon> 刷新页面</li>
      <li class="context-menu__item" @click="closeCurrent()"><el-icon>
          <Close />
        </el-icon> 关闭当前</li>
      <li v-show="otherVisible" class="context-menu__item" @click="closeOthers"><el-icon>
          <CircleClose />
        </el-icon> 关闭其他</li>
      <li v-show="leftVisible" class="context-menu__item" @click="closeLeft"><el-icon>
          <DArrowLeft />
        </el-icon> 关闭左侧</li>
      <li v-show="rightVisible" class="context-menu__item" @click="closeRight"><el-icon>
          <DArrowRight />
        </el-icon> 关闭右侧</li>
      <li class="context-menu__item context-menu__item--danger" @click="closeAll()"><el-icon>
          <CircleClose />
        </el-icon> 全部关闭</li>
    </ul>
  </div>
</template>
<script setup name='Tabs' lang='ts'>
import { ref, watch,reactive,toRef } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useTabStore } from '@/store/useTabStore';
import type { TabsPaneContext } from "element-plus";
import type { TabDefine } from "@/types/common.d.ts";

let tabs = ref()
let activeTabName = ref('')
// 上下文菜单是否显示
let contextMenuVisible = ref(false)
let left = ref(0)
let top = ref(0)
let otherVisible = ref(false)
let leftVisible = ref(false)
let rightVisible = ref(false)
// 右键上下文菜单出现时,光标所在的tab
let locatedTab = reactive<TabDefine>({path:'',text:'',fullPath:""})
let router = useRouter()
let currentRoute = useRoute()
let tabStore = useTabStore()
let openedTabs = toRef(tabStore.openedTabInfo,"tabs")

// 监听contextMenuVisible.value的变化
watch(contextMenuVisible,() => {
  if(contextMenuVisible.value) {
    document.body.addEventListener("click",closeContextMenu)
  }else{
    document.body.removeEventListener("click",closeContextMenu)
  }
})
// 监听路由变化
watch(currentRoute, (newValue, oldValue) => {
  // 设置激活的tab
  activeTabName.value = newValue.path
  if ("/" === newValue.path) return
  // 把路由对象转换成tab存储到pinia中
  let tab: TabDefine = {
    path: newValue.path,
    text: newValue.meta.authorityName as string,
    fullPath: newValue.fullPath,
    query: newValue.query
  }
  tabStore.addTabIfNotExist(tab)
}, {
  immediate: true
})

function tabClick(tab: TabsPaneContext) {
  let clickedTab = openedTabs.value.find(obj => obj.path === tab.paneName) as TabDefine
  router.replace(clickedTab.fullPath)
}
// 打开上下文菜单
function openContextMenu(e:PointerEvent) {
    // 只有真正点击的是tab-pane的时候才打开上下文菜单
    // 每个tab-pane的id="tab-"+tab-pane的name,因为
    // 我配置的name="item.path",故id格式为"tab-/user/manage"之类
    let tabIndex =(e.target as HTMLElement).id.indexOf("tab-")
    if (tabIndex === -1) {
        return
    }
    const contextMenuMinWidth = 184
    // 获取当前组件最外层元素的宽度
    const offsetWidth = tabs.value.offsetWidth
    // 距离左边的最大距离,超过这个距离上下文菜单将被遮盖
    const maxLeft = offsetWidth - contextMenuMinWidth
    const clientX = e.clientX - 15
    if (clientX > maxLeft) {
        left.value = maxLeft
    } else {
        left.value = clientX
    }
    top.value = e.clientY + 20
    // 设置右键菜单可见
    contextMenuVisible.value = true
    let start = tabIndex + "tab-".length;
    // 截取到路由的path,如/user/manage
    let locatedTabPath = (e.target as HTMLElement).id.substring(start)
    // 保存光标所在的tab
    locatedTab = findTabByPath(locatedTabPath) as TabDefine
    // 动态实时的决定菜单项是否显示
    decideLeftVisible()
    decideRightVisible()
    decideOtherVisible()
}
function closeContextMenu(){
  contextMenuVisible.value = false
}
function decideLeftVisible(){
  leftVisible.value = locatedTab.path !== openedTabs.value[0].path
}
function decideRightVisible(){
  rightVisible.value = locatedTab.path !== openedTabs.value[openedTabs.value.length - 1].path
}
function decideOtherVisible(){
  otherVisible.value = openedTabs.value.length > 1
}
function findTabByPath(path:string):TabDefine|undefined {
    return openedTabs.value.find(valObj => valObj.path === path)
}
function closeTab(tabName: string) {
  // 关闭的是激活项
  if (tabName === activeTabName.value) {
        if (openedTabs.value.length > 1) {
            // 获取光标所在tab的下标
            let tabToCloseIndex = openedTabs.value.findIndex(valObj => valObj.path === tabName);
            // 声明类型
            let tabToActive:TabDefine
            // 已经是第一个
            if (tabToCloseIndex === 0) {
                // 激活右侧一个
                tabToActive = openedTabs.value[tabToCloseIndex + 1]
            } else {
              // 激活左侧一个
                tabToActive = openedTabs.value[tabToCloseIndex - 1]
            }
            // 改变地址栏的地址
            router.replace(tabToActive.fullPath)
        } else {
            //只有1个tab时
            router.replace("/")
        }
    }
    // 关闭tab
    tabStore.closeTab(tabName)
}
function refreshTab(){
  router.replace({
        path: '/redirect' + locatedTab.path,
        query: locatedTab.query
    })
}
// 关闭当前tab
function closeCurrent(){
  closeTab(locatedTab.path)
}
// 关闭其他
function closeOthers(){
  tabStore.closeOthers(locatedTab)
  // 当前光标不在激活项上,说明激活项被关闭
  if (locatedTab.path !== activeTabName.value) {
      router.replace(locatedTab.fullPath)
  }
}
function closeLeft(){
  tabStore.closeLeft(locatedTab)
  // 当前光标不在激活项上
  if (locatedTab.path !== activeTabName.value) {
      let activeTab = findTabByPath(activeTabName.value)
      if(!activeTab) { // 说明激活项被关闭
        router.replace(locatedTab.fullPath)
      }
  }
}
function closeRight(){
  tabStore.closeRight(locatedTab)
  // 当前光标不在激活项上
  if (locatedTab.path !== activeTabName.value) {
      let activeTab = findTabByPath(activeTabName.value)
      if(!activeTab) { // 说明激活项被关闭
        router.replace(locatedTab.fullPath)
      }
  }
}
function closeAll(){
  tabStore.closeAll()
  router.replace("/")
}
</script>
<style scoped>
.context-menu {
  margin: 0;
  z-index: 3000;
  position: absolute;
  list-style-type: none;
  width: 184px;
  padding: 8px;
  border: 1px solid rgba(223, 230, 241, 0.96);
  border-radius: 16px;
  font-size: 12px;
  font-weight: 400;
  color: var(--space-text);
  background:
    radial-gradient(circle at top left, rgba(99, 91, 255, 0.06), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(248, 251, 255, 0.96));
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.09);
  backdrop-filter: blur(8px);
}

.workspace-tabs-shell {
  position: relative;
}

.context-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  min-height: 38px;
  padding: 0 12px;
  cursor: pointer;
  border-radius: 12px;
  color: var(--space-text-soft);
  font-weight: 600;
}

.context-menu__item + .context-menu__item {
  margin-top: 4px;
}

.context-menu__item:hover {
  background: rgba(99, 91, 255, 0.08);
  color: var(--space-primary);
}

.context-menu__item--danger:hover {
  background: rgba(254, 242, 242, 0.96);
  color: #dc2626;
}

.context-menu__item :deep(.el-icon) {
  color: currentColor;
}

:deep(.el-tabs) {
  padding: 8px 8px 6px;
  border: 1px solid var(--space-border);
  border-radius: 20px;
  background:
    radial-gradient(circle at top left, rgba(99, 91, 255, 0.04), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 251, 255, 0.94));
  box-shadow: var(--space-card-shadow);
}

:deep(.el-tabs__header) {
  margin: 0;
  padding: 4px 8px 2px;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__nav) {
  gap: 6px;
}

:deep(.el-tabs__nav-next),
:deep(.el-tabs__nav-prev) {
  width: 28px;
  height: 28px;
  margin-top: 3px;
  border: 1px solid transparent;
  border-radius: 999px;
  color: var(--space-text-soft);
}

:deep(.el-tabs__nav-next:hover),
:deep(.el-tabs__nav-prev:hover) {
  color: var(--space-primary);
  border-color: rgba(223, 230, 241, 0.96);
  background: rgba(248, 251, 255, 0.96);
}

:deep(.el-tabs__active-bar) {
  display: none;
}

:deep(.el-tabs__item) {
  height: 36px;
  padding: 0 16px;
  border: 1px solid transparent;
  border-radius: 999px;
  color: var(--space-text-soft);
  font-weight: 600;
  transition: color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

:deep(.el-tabs__item:hover) {
  color: var(--space-text);
  border-color: rgba(223, 230, 241, 0.96);
  background: rgba(248, 251, 255, 0.92);
}

:deep(.el-tabs__item.is-active) {
  color: var(--space-primary);
  border-color: rgba(99, 91, 255, 0.18);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(239, 241, 255, 0.94));
  box-shadow: 0 8px 18px rgba(99, 91, 255, 0.08);
}

:deep(.el-tabs__item .is-icon-close) {
  width: 18px;
  height: 18px;
  margin-left: 8px;
  border-radius: 999px;
  transition: background 0.18s ease, color 0.18s ease;
}

:deep(.el-tabs__item .is-icon-close:hover) {
  background: rgba(99, 91, 255, 0.12);
  color: var(--space-primary);
}
</style>
