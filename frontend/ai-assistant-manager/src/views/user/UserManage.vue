<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">用户资产管理</div>
      <div class="hero-subtitle">
        从平台侧查看全部注册用户、头像、应用凭证与注册时间，便于排查账号归属、接口授权和接入状态。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">用户总数：{{ tableData.total || 0 }}</span>
        <span class="hero-badge">支持用户名检索</span>
        <span class="hero-badge">可核查 appKey 分发情况</span>
      </div>
    </section>

    <section class="toolbar-panel glass-panel">
      <div class="toolbar-copy">
        <div class="toolbar-title">用户列表</div>
        <div class="toolbar-desc">
          适合结合头像、注册时间与 appKey 做账号巡检。如果某个应用侧接入异常，先确认对应账号和 appKey 是否一致。
        </div>
      </div>
      <div class="toolbar-actions">
        <el-form :model="searchData" :inline="true">
          <el-form-item>
            <el-input type="text" placeholder="用户名" v-model="searchData.username" clearable style="width: 180px"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button @click="loadTable" type="primary">查询</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <!-- 表格   -->
    <section class="glass-panel table-panel">
      <el-table :data="tableData.rows" stripe :border="true" style="width: 100%">
        <el-table-column prop="username" label="用户名">
        </el-table-column>
        <el-table-column prop="avatarPath" label="头像">
          <!-- preview-teleported避免预览的图片被表格遮挡 -->
          <template v-slot:default="scope">
            <el-image v-if="scope.row.avatarPath" style="width: 50px;height: 50px;" :src="scope.row.avatarPath"
              :preview-src-list="[scope.row.avatarPath]" :preview-teleported="true" fit="fill"></el-image>
          </template>
        </el-table-column>
        <el-table-column prop="appKey" label="appKey">
        </el-table-column>
        <el-table-column prop="registerTime" label="注册时间">
        </el-table-column>
      </el-table>
    </section>
    <!-- 分页   -->
    <section class="mt-dot5 glass-panel pagination-panel">
      <el-pagination @size-change="handlePageSizeChange" @current-change="handlePageNowChange"
        :current-page="searchData.pageNow" :page-sizes="[10, 30, 50]" :page-size="searchData.pageSize"
        layout="total, sizes, prev, pager, next, jumper" :total="tableData.total">
      </el-pagination>
    </section>
  </div>
</template>
<script setup name='UserManage' lang='ts'>
import { reactive, onMounted } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageUserApi } from '@/api/userApi';
let searchFormData = reactive({
  username: ''
})
let {
  searchData,
  tableData,
  loadTable,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageUserApi })

onMounted(() => {
  loadTable()
})
</script>
<style scoped>
.table-panel,
.pagination-panel {
  padding: 18px;
}
</style>
