<template>
  <div class="tab-panel">
    <section class="glass-panel section-panel">
      <div class="section-head">
        <div>
          <div class="section-title">记录列表</div>
          <div class="section-desc">先看原始调用记录，再决定是否导出或转成验收条目。</div>
        </div>
        <div class="section-actions">
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportCurrentRows">导出当前结果</el-button>
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportAcceptanceDraft">导出验收草稿</el-button>
        </div>
      </div>
    </section>

    <section class="glass-panel table-panel">
      <el-table :data="model.recordRows" stripe :border="true" style="width: 100%">
        <el-table-column type="expand">
          <template #default="props">
            <el-table :data="props.row.detailList" stripe :border="true" style="width: 100%">
              <el-table-column prop="modelName" label="模型名称" />
              <el-table-column prop="costToken" label="消费token数" />
              <el-table-column prop="statusDesc" label="状态">
                <template #default="scope">
                  <el-tag v-if="scope.row.status === 1" type="success">{{ scope.row.statusDesc }}</el-tag>
                  <el-tag v-if="scope.row.status === 0" type="danger">{{ scope.row.statusDesc }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="costTime" label="耗时(ms)" />
              <el-table-column prop="failReason" label="失败原因">
                <template #default="scope">
                  <div class="cell-stack">
                    <el-tooltip placement="top">
                      <template #content>
                        <div class="new-line">
                          {{ scope.row.failReason }}
                        </div>
                      </template>
                      <p class="ellipsis">{{ scope.row.failReason }}</p>
                    </el-tooltip>
                    <el-button
                      v-if="scope.row.failReason"
                      text
                      class="workspace-btn workspace-btn--text record-text-btn"
                      @click="model.openDetailDialog('失败原因', scope.row.failReason)"
                    >
                      查看全文
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="startTime" label="开始时间" />
              <el-table-column prop="endTime" label="结束时间" />
              <el-table-column prop="userInput" label="查询词">
                <template #default="scope">
                  <div class="cell-stack">
                    <el-tooltip placement="top">
                      <template #content>
                        <div class="new-line">
                          {{ scope.row.userInput }}
                        </div>
                      </template>
                      <p class="ellipsis">{{ scope.row.userInput }}</p>
                    </el-tooltip>
                    <div class="record-actions">
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.copyText(scope.row.userInput, '查询词')">复制</el-button>
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.openDetailDialog('查询词', scope.row.userInput)">查看全文</el-button>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="assistantMessage" label="响应结果">
                <template #default="scope">
                  <div class="cell-stack">
                    <el-tooltip placement="top">
                      <template #content>
                        <div class="new-line">
                          {{ scope.row.assistantMessage }}
                        </div>
                      </template>
                      <p class="ellipsis multi-line-ellipsis">{{ scope.row.assistantMessage }}</p>
                    </el-tooltip>
                    <div class="record-actions">
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.copyText(scope.row.assistantMessage, '响应结果')">复制</el-button>
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="model.openDetailDialog('响应结果', scope.row.assistantMessage)">查看全文</el-button>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="验收整理" width="180">
                <template #default="scope">
                  <div class="cell-stack">
                    <el-button
                      text
                      class="workspace-btn workspace-btn--text record-text-btn"
                      @click="model.copyAcceptanceEntry(props.row, scope.row)"
                    >
                      复制验收条目
                    </el-button>
                    <el-button
                      text
                      class="workspace-btn workspace-btn--text record-text-btn"
                      @click="model.openAcceptanceDialog(props.row, scope.row)"
                    >
                      查看验收草稿
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="系统编号" />
        <el-table-column prop="appName" label="所属应用" />
        <el-table-column prop="libName" label="所属知识库" />
        <el-table-column prop="username" label="调用人" />
        <el-table-column prop="statusDesc" label="状态">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 1" type="success">{{ scope.row.statusDesc }}</el-tag>
            <el-tag v-if="scope.row.status === 0" type="danger">{{ scope.row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因">
          <template #default="scope">
            <el-tooltip placement="top">
              <template #content>
                <div class="new-line">
                  {{ scope.row.failReason }}
                </div>
              </template>
              <p class="ellipsis">{{ scope.row.failReason }}</p>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" />
        <el-table-column prop="startTime" label="开始时间" />
        <el-table-column prop="endTime" label="结束时间" />
      </el-table>
    </section>

    <section class="mt-dot5 glass-panel pagination-panel">
      <el-pagination
        :current-page="model.searchData.pageNow"
        :page-sizes="[10, 30, 50]"
        :page-size="model.searchData.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="model.paginationTotal"
        @size-change="model.handlePageSizeChange"
        @current-change="model.handlePageNowChange"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { useInvokeRecordFeatureModel } from '../../composables/useInvokeRecordFeature'

const model = useInvokeRecordFeatureModel()
</script>

<style scoped>
.tab-panel,
.section-panel,
.table-panel,
.pagination-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-panel,
.table-panel,
.pagination-panel {
  padding: 18px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.section-title {
  color: var(--space-text);
  font-size: 16px;
  font-weight: 700;
}

.section-desc {
  margin-top: 6px;
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.section-actions,
.record-actions,
.cell-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.cell-stack {
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.record-text-btn {
  min-height: 24px !important;
  padding: 0 !important;
  font-size: 12px;
}

.multi-line-ellipsis {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: normal;
}

@media (max-width: 900px) {
  .section-head {
    flex-direction: column;
  }
}
</style>
