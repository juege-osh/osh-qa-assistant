<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">应用管理总览</div>
      <div class="hero-subtitle">
        从平台维度查看所有用户创建的应用，检查归属账号、绑定知识库、图标信息与创建更新时间，便于统一运维和质量巡检。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">应用总数：{{ tableData.total || 0 }}</span>
        <span class="hero-badge">支持按名称快速检索</span>
        <span class="hero-badge">可用于排查绑定与归属问题</span>
      </div>
    </section>

    <section class="toolbar-panel glass-panel">
      <div class="toolbar-copy">
        <div class="toolbar-title">应用列表</div>
        <div class="toolbar-desc">
          管理端以审查和巡检为主，重点关注应用归属、知识库绑定、描述完整性以及最近一次修改时间。
        </div>
      </div>
      <div class="toolbar-actions">
        <el-form :model="searchData" :inline="true">
          <el-form-item>
            <el-input type="text" placeholder="按应用名称搜索" v-model="searchData.appName" clearable style="width: 180px"></el-input>
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
        <el-table-column prop="username" label="所属用户">
        </el-table-column>
        <el-table-column prop="appName" label="应用名称">
        </el-table-column>
        <el-table-column prop="libName" label="绑定知识库">
        </el-table-column>
        <el-table-column prop="appDesc" label="应用描述">
        </el-table-column>
        <el-table-column prop="iconPath" label="图标">
          <template v-slot:default="scope">
            <el-image v-if="scope.row.iconPath" style="width: 50px;height: 50px;"
              :src="scope.row.iconPath" :preview-src-list="[scope.row.iconPath]"
              :preview-teleported="true" fit="fill"></el-image>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间">
        </el-table-column>
        <el-table-column prop="modifiedTime" label="修改时间">
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
<script setup name='AppManage' lang='ts'>
import { ref, reactive, onMounted } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageAppApi } from '@/api/appApi';

let searchFormData = reactive({
  appName: ''
})

let {
  searchData,
  tableData,
  loadTable,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageAppApi })

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
