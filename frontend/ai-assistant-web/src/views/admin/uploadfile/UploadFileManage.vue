<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">文档向量化资产</div>
      <div class="hero-subtitle">
        统一查看所有用户上传的文件、归属应用与知识库、字符规模、召回次数和启用状态，用于评估知识库实际命中与资源使用情况。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">文件总数：{{ tableData.total || 0 }}</span>
        <span class="hero-badge">支持按用户、应用、知识库筛选</span>
        <span class="hero-badge">可观察召回热度</span>
      </div>
    </section>

    <section class="toolbar-panel glass-panel">
      <div class="toolbar-copy">
        <div class="toolbar-title">文件列表</div>
        <div class="toolbar-desc">
          建议优先关注高字符数、低召回次数的文档，这类资源往往需要优化切分方式、标题命名或知识库结构。
        </div>
      </div>
      <div class="toolbar-actions">
        <el-form :model="searchData" :inline="true">
          <el-form-item>
            <el-input type="text" placeholder="所属用户" v-model="searchData.username" clearable style="width: 150px"></el-input>
          </el-form-item>
          <el-form-item>
            <el-input type="text" placeholder="所属应用" v-model="searchData.appName" clearable style="width: 150px"></el-input>
          </el-form-item>
          <el-form-item>
            <el-input type="text" placeholder="知识库名称" v-model="searchData.libName" clearable style="width: 150px"></el-input>
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
        <el-table-column prop="appName" label="所属应用">
        </el-table-column>
        <el-table-column prop="libName" label="知识库名称">
        </el-table-column>
        <el-table-column prop="fileName" label="文件名称">
        </el-table-column>
        <el-table-column prop="charCount" label="字符数">
        </el-table-column>
        <el-table-column prop="recallCount" label="召回次数">
        </el-table-column>
        <el-table-column prop="statusDesc" label="状态">
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间">
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
<script setup name='UploadFileManage' lang='ts'>
import { ref, reactive, onMounted } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageUploadFileApi } from '@/api/admin/uploadFileApi';

let searchFormData = reactive({
  username: '',
  appName: '',
  libName: '',
})

let {
  searchData,
  tableData,
  loadTable,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageUploadFileApi })

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
