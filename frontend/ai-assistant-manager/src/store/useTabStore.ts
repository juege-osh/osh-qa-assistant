import {defineStore} from 'pinia'
import type { TabDefine, TabArrayDefine } from "@/types/common.d.ts"
import pinia from '@/store/index'
// 命名一般为hook的方式(useXxx)
export const useTabStore = defineStore("tab",{
  // 定义动作的地方
  actions:{
    addTabIfNotExist(tabToAdd:TabDefine){
      // 判断state里面是否已经存在
      let existFlag = this.openedTabInfo.tabs.some(obj => obj.path === tabToAdd.path)
      if(existFlag) {
        return
      }
      this.addTab(tabToAdd);
    },
    addTab(tabToAdd:TabDefine){
      this.openedTabInfo.tabs.push(tabToAdd)
    },
    // 通过tabPane的name(==tabDefine的path)关闭tab
    closeTab(tabName:string) {
      this.openedTabInfo.tabs = this.openedTabInfo.tabs.filter(val => val.path !== tabName)
    },
    closeOthers(locatedTab:TabDefine){
      // 当前光标所在的tab
      this.openedTabInfo.tabs = this.openedTabInfo.tabs.filter(val => {
        if (val.path === locatedTab.path ) {
            return true
        }
      })
    },
    closeLeft(locatedTab:TabDefine){
      let idx = this.openedTabInfo.tabs.findIndex(valObj => valObj.path === locatedTab.path);
      let filteredTabs = this.openedTabInfo.tabs.filter((val,index) => {
          if (index >= idx ) {
              // 下标>=当前光标所在的tab,说明在右边或是自己
              return true
          }
      })
      this.openedTabInfo.tabs = filteredTabs
    },
    closeRight(locatedTab:TabDefine){
      let idx = this.openedTabInfo.tabs.findIndex(valObj => valObj.path === locatedTab.path);
      let filteredTabs = this.openedTabInfo.tabs.filter((val,index) => {
          if (index <= idx ) {
              // 下标<=当前光标所在的tab,说明在左边或是自己
              return true
          }
      })
      this.openedTabInfo.tabs = filteredTabs
    },
    closeAll(){
      this.openedTabInfo.tabs = []
    },
    resetAll(){
      this.$reset()
    }
  },
  // 存储数据的地方
  state(){
    return {
      openedTabInfo: {
        // 要打开的tab数组
        tabs: [] as TabArrayDefine
      }
    }
  }
})

// 在非.vue组件中使用store,需要传入pinia实例
export function useTabStoreExternal() {
  return useTabStore(pinia)
}