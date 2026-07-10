import { defineStore } from 'pinia'
import { getItem, saveItem } from '@/util/storageUtil'
import type { InvokeRecordTabKey } from '../types'

const ACTIVE_TAB_STORAGE_KEY = 'invoke-record.active-tab'

function loadActiveTab(): InvokeRecordTabKey {
  const storedValue = getItem(ACTIVE_TAB_STORAGE_KEY)
  if (storedValue === 'records' || storedValue === 'review' || storedValue === 'acceptance') {
    return storedValue
  }
  return 'records'
}

export const useInvokeRecordTabStore = defineStore('invoke-record-tab', {
  state: () => ({
    activeTab: loadActiveTab() as InvokeRecordTabKey
  }),
  actions: {
    setActiveTab(tab: InvokeRecordTabKey) {
      this.activeTab = tab
      saveItem(ACTIVE_TAB_STORAGE_KEY, tab)
    }
  }
})
