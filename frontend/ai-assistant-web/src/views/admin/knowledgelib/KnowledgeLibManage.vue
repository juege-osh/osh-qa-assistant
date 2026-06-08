<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">知识库巡检</div>
      <div class="hero-subtitle">
        平台侧统一审查知识库归属、描述质量、图标配置以及创建修改时间，方便定位内容资产是否规范、是否缺少维护。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">知识库总数：{{ tableData.total || 0 }}</span>
        <span class="hero-badge">支持按用户和名称筛选</span>
        <span class="hero-badge">适合巡检内容维护状态</span>
      </div>
    </section>

    <section class="toolbar-panel glass-panel">
      <div class="toolbar-copy">
        <div class="toolbar-title">知识库列表</div>
        <div class="toolbar-desc">
          建议重点关注描述是否完整、归属是否正确、图标是否缺失，以及长时间未更新的知识库是否仍在使用。
        </div>
      </div>
      <div class="toolbar-actions">
        <el-form :model="searchData" :inline="true">
          <el-form-item>
            <el-input type="text" placeholder="所属用户" v-model="searchData.username" clearable style="width: 150px"></el-input>
          </el-form-item>
          <el-form-item>
            <el-input type="text" placeholder="知识库名称" v-model="searchData.libName" clearable style="width: 180px"></el-input>
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
        <el-table-column prop="libName" label="知识库名称">
        </el-table-column>
        <el-table-column prop="libDesc" label="描述">
        </el-table-column>
        <el-table-column prop="iconPath" label="图标">
          <template v-slot:default="scope">
            <el-image v-if="scope.row.iconPath" style="width: 50px;height: 50px;" :src="scope.row.iconPath"
              :preview-src-list="[scope.row.iconPath]" :preview-teleported="true" fit="fill"></el-image>
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
<script setup name='KnowledgeLibManage' lang='ts'>
import { ref, reactive, onMounted } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageKnowledgeLibApi } from '@/api/admin/knowledgeLibApi';

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
} = useTable({ searchFormData, loadTableApi: pageKnowledgeLibApi })

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
