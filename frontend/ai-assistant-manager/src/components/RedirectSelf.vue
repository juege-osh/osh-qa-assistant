<template>
  <div>
    
  </div>
</template>
<script setup name='RedirectSelf' lang='ts'>
   /*
   一个临时的中转页面,用于实现刷新的功能,流程:originalPath->RedirectSelf->originalPath
   开发步骤:
   1.开发RedirectSelf.vue
   2.配置路由
   3.使用:
      router.push({
        path:"/redirect" + "/login",
        query:{}
      })
   */
   import { onMounted } from 'vue';
   import { useRoute,useRouter } from 'vue-router';
   let router = useRouter()
   let currentRoute = useRoute()
   onMounted(() => {
      const {params, query} = currentRoute
      // 获取原始路径,login
      const {originalPath} = params
      // 立即原样跳回
      router.replace({
        path: "/" + originalPath,
        query
      })

   })
</script>
<style scoped>
   
</style>