<template>
  <div>
    <el-container style="height:100vh">
      <!-- 左侧导航 -->
      <el-aside width="260px" style="border-right:1px solid #e4e7ed;overflow-y:auto">
        <el-menu :default-active="activeIndex" @select="handleSelect">
          <el-menu-item v-for="(api,index) in apiList" :key="api.id" :index="api.id">
            <span>{{ (index+1) +"."+api.name }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 右侧内容 -->
      <el-main style="overflow-y:auto;padding:20px">
        <!-- 通用信息 -->
        <h3>通用请求信息</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="请求方式">POST</el-descriptions-item>
          <el-descriptions-item label="Content-Type">application/json</el-descriptions-item>
          <el-descriptions-item label="baseUrl">http://localhost:9000/consumer</el-descriptions-item>
        </el-descriptions>
        <div v-for="(api,index) in apiList" :key="api.id" :id="api.id" style="padding-top:60px;margin-top:-60px">
          <h2>{{ (index+1) +"."+api.name }}</h2>
          <!-- 具体接口 -->
          <el-descriptions :column="1" border>
            <el-descriptions-item v-if="api.desc" label="描述">
                {{ api.desc }}
            </el-descriptions-item>
            <el-descriptions-item label="使用场景">{{ api.name }}</el-descriptions-item>
            <el-descriptions-item label="接口地址">{{ api.url }}</el-descriptions-item>
          </el-descriptions>

          <h4>请求入参</h4>
          <el-table :data="api.params" border>
            <el-table-column prop="name" label="参数名" width="180" />
            <el-table-column prop="desc" label="说明" />
            <el-table-column prop="type" label="类型" width="120" />
            <el-table-column prop="required" label="是否必填" width="100" />
          </el-table>
          <h4>响应结果</h4>
          <el-text>流式输出结果,结束标志:[DONE]</el-text>
        </div>
      </el-main>
    </el-container>
  </div>
</template>
<script setup name='Doc' lang='ts'>
import type { AnyObjsDefine } from '@/types/common'
import { ref, onMounted } from 'vue'

// 接口列表
const apiList = [
  {
    id: 'apiChat',
    name: '智能问答',
    url: '/api/chat',
    params: [
      { name: 'appKey', desc: '系统颁发的appKey,在用户中心查看', type: 'String', required: '是' },
      { name: 'userInput', desc: '用户输入', type: 'String', required: '是' },
      { name: 'appId', desc: '应用标识,在应用列表中查看', type: 'String', required: '是' },
      { name: 'chatId', desc: '唯一会话标识,可自己生成,用于区分不同会话', type: 'String', required: '否' },
    ]
  },
] as AnyObjsDefine

const activeIndex = ref('apiChat')

function handleSelect(index: string) {
  activeIndex.value = index
  document.getElementById(index)?.scrollIntoView({ behavior: 'smooth' })
}

</script>
<style scoped>
h2,h3,h4{
  padding: 10px 0;
}
</style>
