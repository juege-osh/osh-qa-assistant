<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>

<script setup lang="ts" name="App">
import { onMounted,onUnmounted } from 'vue';
import { saveItem } from '@/util/storageUtil';
import { STORAGE_USER_INFO_KEY,STORAGE_USER_TOKEN_KEY } from '@/config/constants';
import { useUserStore } from '@/store/useUserStore';

let userStore = useUserStore();
function toStorage(){
  saveItem(STORAGE_USER_INFO_KEY,userStore.userInfo)
  saveItem(STORAGE_USER_TOKEN_KEY,userStore.token)
}

onMounted(() => {
  // 页面销毁(比如刷新)之前会调用指定的回调
  window.addEventListener("beforeunload",toStorage)
})
onUnmounted(() => {
  window.removeEventListener("beforeunload",toStorage)
})
</script>

<style>
  html,body,#app {
    height: 100%;
    margin: 0;
    padding: 0;
  }
</style>
