<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">调用记录分析</div>
      <div class="hero-subtitle">
        在这里复盘每一次问答请求，查看调用状态、耗时、失败原因、模型明细和实际输入输出，便于评估知识库和模型效果。
      </div>
      <div class="hero-meta">
        <span class="hero-badge">总调用：{{ overview.totalCount }}</span>
        <span class="hero-badge">成功：{{ overview.successCount }}</span>
        <span class="hero-badge">失败：{{ overview.failCount }}</span>
      </div>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <div class="stat-label">总调用次数</div>
        <div class="stat-value">{{ overview.totalCount }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">成功率</div>
        <div class="stat-value">{{ successRate }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">累计 Token</div>
        <div class="stat-value">{{ overview.totalCostToken }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">平均耗时</div>
        <div class="stat-value">{{ overview.avgCostTime }}ms</div>
      </article>
    </section>

    <section class="toolbar-panel glass-panel">
      <div class="toolbar-copy">
        <div class="toolbar-title">筛选记录</div>
        <div class="toolbar-desc">
          你可以按状态和时间范围过滤记录，快速定位失败请求、慢请求和特定时间段内的模型表现。
        </div>
      </div>
      <div class="toolbar-actions">
        <el-form :model="searchData" :inline="true">
          <el-form-item label="状态:" style="width: 150px;">
            <el-select v-model="searchData.status" style="width: 150px">
              <el-option label="全部" value=""></el-option>
              <el-option label="失败" value="0"></el-option>
              <el-option label="成功" value="1"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item style="width: 200px;">
            <el-date-picker type="datetime" v-model="searchData.startTime" format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间" />
          </el-form-item>
          <el-form-item style="width: 200px;">
            <el-date-picker type="datetime" v-model="searchData.endTime" format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间" />
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
        <el-table-column type="expand">
          <template v-slot:default="props">
            <el-table :data="props.row.detailList" stripe :border="true" style="width: 100%">
              <el-table-column prop="modelName" label="模型名称">
              </el-table-column>
              <el-table-column prop="costToken" label="消费token数">
              </el-table-column>
              <el-table-column prop="statusDesc" label="状态">
                <template v-slot:default="scope">
                  <el-tag v-if="scope.row.status === 1" type="success">{{ scope.row.statusDesc }}</el-tag>
                  <el-tag v-if="scope.row.status === 0" type="danger">{{ scope.row.statusDesc }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="costTime" label="耗时(ms)">
              </el-table-column>
              <el-table-column prop="failReason" label="失败原因">
                <template v-slot:default="scope">
                  <el-tooltip placement="top">
                    <template v-slot:content>
                      <div class="new-line">
                        {{ scope.row.failReason }}
                      </div>
                    </template>
                    <p class="ellipsis">{{ scope.row.failReason }}</p>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="startTime" label="开始时间">
              </el-table-column>
              <el-table-column prop="endTime" label="结束时间">
              </el-table-column>
              <el-table-column prop="userInput" label="查询词">
                <template v-slot:default="scope">
                  <el-tooltip placement="top">
                    <template v-slot:content>
                      <div class="new-line">
                        {{ scope.row.userInput }}
                      </div>
                    </template>
                    <p class="ellipsis">{{ scope.row.userInput }}</p>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="assistantMessage" label="响应结果">
                <template v-slot:default="scope">
                  <el-tooltip placement="top">
                    <template v-slot:content>
                      <div class="new-line">
                        {{ scope.row.assistantMessage }}
                      </div>
                    </template>
                    <p class="ellipsis">{{ scope.row.assistantMessage }}</p>
                  </el-tooltip>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="系统编号">
        </el-table-column>
        <el-table-column prop="appName" label="所属应用">
        </el-table-column>
        <el-table-column prop="libName" label="所属知识库">
        </el-table-column>
        <el-table-column prop="username" label="调用人">
        </el-table-column>
        <el-table-column prop="statusDesc" label="状态">
          <template v-slot:default="scope">
            <el-tag v-if="scope.row.status === 1" type="success">{{ scope.row.statusDesc }}</el-tag>
            <el-tag v-if="scope.row.status === 0" type="danger">{{ scope.row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因">
          <template v-slot:default="scope">
            <el-tooltip placement="top">
              <template v-slot:content>
                <div class="new-line">
                  {{ scope.row.failReason }}
                </div>
              </template>
              <p class="ellipsis">{{ scope.row.failReason }}</p>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)">
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间">
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间">
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
<script setup name='InvokeRecordManage' lang='ts'>
import { computed, reactive, onMounted } from 'vue'
import { useTable } from '@/hooks/useTable';
import { pageInvokeRecordApi, queryInvokeRecordOverviewApi } from '@/api/workspace/invokeRecordApi';
let searchFormData = reactive({
  status: "",
  startTime: null,
  endTime: null
})

let {
  searchData,
  tableData,
  loadTable,
  handlePageSizeChange,
  handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageInvokeRecordApi })

const overview = reactive({
  totalCount: 0,
  successCount: 0,
  failCount: 0,
  totalCostToken: 0,
  avgCostTime: 0
})

const successRate = computed(() => {
  if (!overview.totalCount) {
    return '0%'
  }
  return `${((overview.successCount / overview.totalCount) * 100).toFixed(1)}%`
})

function loadOverview() {
  queryInvokeRecordOverviewApi().then(result => {
    Object.assign(overview, result.data || {})
  })
}

onMounted(() => {
  loadTable()
  loadOverview()
})
</script>
<style scoped>
.table-panel,
.pagination-panel {
  padding: 18px;
}
</style>
