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
          你可以按应用、问题关键词、状态和时间范围过滤记录，快速定位失败请求、慢请求和特定场景下的模型表现。
        </div>
      </div>
      <div class="toolbar-actions">
        <el-form :model="searchData" :inline="true">
          <el-form-item>
            <el-input v-model="searchData.appName" placeholder="按应用名称筛选" clearable style="width: 180px" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="searchData.userInputKeyword" placeholder="按问题关键词筛选" clearable style="width: 220px" />
          </el-form-item>
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
          <el-form-item>
            <el-button class="workspace-btn workspace-btn--ghost" @click="resetFilters">重置</el-button>
          </el-form-item>
          <el-form-item>
            <el-button class="workspace-btn workspace-btn--ghost" @click="exportCurrentRows">导出当前结果</el-button>
          </el-form-item>
          <el-form-item>
            <el-button class="workspace-btn workspace-btn--ghost" @click="exportAcceptanceDraft">导出验收草稿</el-button>
          </el-form-item>
          <el-form-item>
            <el-button class="workspace-btn workspace-btn--ghost" @click="openSaveAcceptanceBatchDialog">保存验收批次</el-button>
          </el-form-item>
          <el-form-item>
            <el-button class="workspace-btn workspace-btn--ghost" @click="openTemplateAcceptanceBatchDialog">从默认问题集建批次</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <section class="glass-panel review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">MVP 验收提示</div>
          <div class="review-desc">建议优先检查命中问题、可信、易懂、失败体面四项，再结合调用耗时和失败原因定位问题。</div>
        </div>
        <div class="review-badges">
          <span class="review-badge">当前记录数：{{ filteredRows.length }}</span>
          <span class="review-badge">失败记录：{{ failRowCount }}</span>
          <span class="review-badge">慢请求：{{ slowRowCount }}</span>
          <span class="review-badge">长回答：{{ longAnswerRowCount }}</span>
          <span class="review-badge">待跟进：{{ followUpCount }}</span>
        </div>
      </div>
      <div class="quick-filter-row">
        <span class="quick-filter-label">快速聚焦</span>
        <el-button
          class="workspace-btn"
          :class="quickView === 'all' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'all'"
        >
          全部记录
        </el-button>
        <el-button
          class="workspace-btn"
          :class="quickView === 'fail' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'fail'"
        >
          只看失败
        </el-button>
        <el-button
          class="workspace-btn"
          :class="quickView === 'slow' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'slow'"
        >
          只看慢请求
        </el-button>
        <el-button
          class="workspace-btn"
          :class="quickView === 'long' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'long'"
        >
          只看长回答
        </el-button>
        <el-button
          class="workspace-btn"
          :class="quickView === 'followUp' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'followUp'"
        >
          只看待跟进
        </el-button>
        <el-button
          class="workspace-btn"
          :class="quickView === 'reviewed' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="quickView = 'reviewed'"
        >
          只看已复盘
        </el-button>
        <span class="quick-filter-hint">慢请求默认按耗时 ≥ 5000ms，长回答默认按回答长度 ≥ 200 字符判断。</span>
      </div>
      <div class="review-header-note">
        <span>建议顺序：先看失败，再看慢请求，最后抽样看长回答是否真正答题且有依据。</span>
      </div>
    </section>

    <section class="glass-panel review-panel compact-panel">
      <div class="review-header">
        <div>
          <div class="review-title">当前聚焦说明</div>
          <div class="review-desc">{{ currentQuickViewDesc }}</div>
        </div>
      </div>
    </section>

    <section class="glass-panel review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">优先复盘清单</div>
          <div class="review-desc">这里按失败、慢请求、长回答等规则自动挑出值得优先检查的记录，只做排查建议，不替代人工验收结论。</div>
        </div>
        <div class="review-badges">
          <span class="review-badge">待优先复盘：{{ priorityReviewList.length }}</span>
        </div>
      </div>
      <div v-if="priorityReviewList.length" class="priority-grid">
        <article v-for="item in priorityReviewList" :key="item.key" class="priority-card">
          <div class="priority-card-head">
            <div>
              <div class="priority-card-title">
                {{ item.row.appName || '未命名应用' }}
                <span class="priority-card-id">#{{ item.row.id }}</span>
              </div>
              <div class="priority-card-meta">
                {{ item.row.libName || '未绑定知识库' }} · {{ item.detail.modelName || '未知模型' }} · {{ item.detail.costTime ?? item.row.costTime ?? '-' }}ms
              </div>
            </div>
            <div class="priority-tag-group">
              <span
                v-for="tag in item.riskTags"
                :key="`${item.key}-${tag.key}`"
                class="priority-tag"
                :class="`priority-tag--${tag.tone}`"
              >
                {{ tag.label }}
              </span>
              <span v-if="getReviewStatus(item.key) !== 'pending'" class="priority-tag priority-tag--success">
                {{ reviewStatusText[getReviewStatus(item.key)] }}
              </span>
            </div>
          </div>
          <div class="priority-block">
            <div class="priority-label">用户问题</div>
            <div class="priority-text">{{ item.detail.userInput || '-' }}</div>
          </div>
          <div class="priority-block">
            <div class="priority-label">回答摘要</div>
            <div class="priority-text">{{ summarizeDisplayText(item.detail.assistantMessage) }}</div>
          </div>
          <div class="priority-block" v-if="item.detail.failReason || item.row.failReason">
            <div class="priority-label">失败原因</div>
            <div class="priority-text priority-text--danger">{{ item.detail.failReason || item.row.failReason }}</div>
          </div>
          <div class="priority-actions">
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="copyText(item.detail.userInput, '查询词')">复制问题</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="openDetailDialog('响应结果', item.detail.assistantMessage)">查看回答</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="copyAcceptanceEntry(item.row, item.detail)">复制验收条目</el-button>
            <el-button
              text
              class="workspace-btn workspace-btn--text record-text-btn"
              @click="cycleReviewStatus(item.key)"
            >
              {{ nextReviewActionText(item.key) }}
            </el-button>
          </div>
        </article>
      </div>
      <div v-else class="empty-review-hint">
        当前筛选结果里没有自动判定为重点复盘的记录，可以继续抽样检查表格明细。
      </div>
    </section>

    <section class="glass-panel review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">待跟进问题分布</div>
          <div class="review-desc">把待跟进记录归到补知识、补切分、补提示词、补展示、补观测等动作类型，帮助后续直接进入任务池。</div>
        </div>
        <div class="review-badges">
          <span class="review-badge">已归类：{{ categorizedFollowUpCount }}</span>
        </div>
      </div>
      <div class="category-export-row">
        <el-button class="workspace-btn workspace-btn--ghost" @click="exportFollowUpTaskDraft">导出后续任务草稿</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" @click="openTaskSuggestionDialog">查看任务建议清单</el-button>
        <span class="quick-filter-hint">按当前待跟进分类结果生成 Markdown，方便直接整理进任务池或复盘文档。</span>
      </div>
      <div class="quick-filter-row">
        <span class="quick-filter-label">分类聚焦</span>
        <el-button
          class="workspace-btn"
          :class="followUpCategoryView === '' ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="followUpCategoryView = ''"
        >
          全部分类
        </el-button>
        <el-button
          v-for="option in followUpCategoryOptions"
          :key="option.value"
          class="workspace-btn"
          :class="followUpCategoryView === option.value ? 'workspace-btn--primary' : 'workspace-btn--ghost'"
          @click="followUpCategoryView = option.value"
        >
          {{ option.label }}
        </el-button>
      </div>
      <div v-if="followUpCategoryStats.length" class="category-grid">
        <article v-for="item in followUpCategoryStats" :key="item.key" class="category-card">
          <div class="category-card-count">{{ item.count }}</div>
          <div class="category-card-title">{{ item.label }}</div>
          <div class="category-card-desc">{{ item.description }}</div>
        </article>
      </div>
      <div v-else class="empty-review-hint">
        当前还没有待跟进分类，先把某些明细标记为待跟进，再选择对应原因分类。
      </div>
    </section>

    <section class="glass-panel review-panel">
      <div class="review-header">
        <div>
          <div class="review-title">已保存验收批次</div>
          <div class="review-desc">这里保存每次真实跑测后的验收批次，便于回看结论、继续补充问题单和再次导出。</div>
        </div>
        <div class="review-badges">
          <span class="review-badge">批次数：{{ savedAcceptanceBatches.length }}</span>
        </div>
      </div>
      <div class="category-export-row">
        <el-button class="workspace-btn workspace-btn--ghost" :disabled="selectedAcceptanceBatchIds.length !== 2" @click="openAcceptanceCompareDialog">
          对比两轮验收
        </el-button>
        <el-button class="workspace-btn workspace-btn--ghost" :disabled="selectedAcceptanceBatchIds.length !== 2" @click="exportAcceptanceCompareDraft">
          导出对比报告
        </el-button>
        <span class="quick-filter-hint">建议选择同一知识库、不同实验版本的两轮正式验收批次，直接比较切分版本或提示词版本效果。</span>
      </div>
      <div v-if="savedAcceptanceBatches.length" class="saved-batch-grid">
        <article v-for="batch in savedAcceptanceBatches" :key="batch.id" class="saved-batch-card">
          <div class="saved-batch-head">
            <div>
              <div class="saved-batch-title">{{ batch.batchName }}</div>
              <div class="saved-batch-meta">
                {{ batch.appName || '未填写应用' }} · {{ batch.sceneType || '未填写场景' }} · {{ formatDateTime(batch.testDate || batch.createdTime) }}
              </div>
              <div class="saved-batch-meta" v-if="batch.releaseVersion || batch.experimentVersion">
                发布版本：{{ batch.releaseVersion || '-' }} · 实验版本：{{ batch.experimentVersion || '-' }}
              </div>
            </div>
            <div class="saved-batch-head-side">
              <label class="saved-batch-check">
                <input
                  type="checkbox"
                  :checked="selectedAcceptanceBatchIds.includes(batch.id)"
                  @change="toggleAcceptanceBatchSelection(batch.id)"
                >
                对比
              </label>
              <div class="saved-batch-count">{{ batch.itemCount || 0 }} 条</div>
            </div>
          </div>
          <div class="saved-batch-summary">
            <span>通过：{{ batch.passCount || 0 }}</span>
            <span>待跟进：{{ batch.followUpCount || 0 }}</span>
          </div>
          <div class="saved-batch-text">{{ batch.summaryConclusion || '当前还没有填写汇总结论。' }}</div>
          <div class="priority-actions">
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="openSavedAcceptanceBatch(batch.id)">查看并编辑</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="exportSavedAcceptanceBatch(batch)">导出 Markdown</el-button>
            <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="exportAcceptanceRepairDraft(batch.id)">导出修复建议</el-button>
          </div>
        </article>
      </div>
      <div v-else class="empty-review-hint">
        还没有保存过验收批次。建议先从当前筛选结果生成一轮真实验收批次，再逐条补齐结论。
      </div>
    </section>

    <!-- 表格   -->
    <section class="glass-panel table-panel">
      <el-table :data="filteredRows" stripe :border="true" style="width: 100%">
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
                  <div class="cell-stack">
                    <el-tooltip placement="top">
                      <template v-slot:content>
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
                      @click="openDetailDialog('失败原因', scope.row.failReason)"
                    >
                      查看全文
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="startTime" label="开始时间">
              </el-table-column>
              <el-table-column prop="endTime" label="结束时间">
              </el-table-column>
              <el-table-column prop="userInput" label="查询词">
                <template v-slot:default="scope">
                  <div class="cell-stack">
                    <el-tooltip placement="top">
                      <template v-slot:content>
                        <div class="new-line">
                          {{ scope.row.userInput }}
                        </div>
                      </template>
                      <p class="ellipsis">{{ scope.row.userInput }}</p>
                    </el-tooltip>
                    <div class="record-actions">
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="copyText(scope.row.userInput, '查询词')">复制</el-button>
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="openDetailDialog('查询词', scope.row.userInput)">查看全文</el-button>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="assistantMessage" label="响应结果">
                <template v-slot:default="scope">
                  <div class="cell-stack">
                    <el-tooltip placement="top">
                      <template v-slot:content>
                        <div class="new-line">
                          {{ scope.row.assistantMessage }}
                        </div>
                      </template>
                      <p class="ellipsis multi-line-ellipsis">{{ scope.row.assistantMessage }}</p>
                    </el-tooltip>
                    <div class="record-actions">
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="copyText(scope.row.assistantMessage, '响应结果')">复制</el-button>
                      <el-button text class="workspace-btn workspace-btn--text record-text-btn" @click="openDetailDialog('响应结果', scope.row.assistantMessage)">查看全文</el-button>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="验收整理" width="180">
                <template v-slot:default="scope">
                  <div class="cell-stack">
                    <el-button
                      text
                      class="workspace-btn workspace-btn--text record-text-btn"
                      @click="copyAcceptanceEntry(props.row, scope.row)"
                    >
                      复制验收条目
                    </el-button>
                    <el-button
                      text
                      class="workspace-btn workspace-btn--text record-text-btn"
                      @click="openAcceptanceDialog(props.row, scope.row)"
                    >
                      查看验收草稿
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="复盘状态" width="150">
                <template v-slot:default="scope">
                  <div class="cell-stack">
                    <el-tag
                      :type="reviewStatusTagType[getReviewStatus(buildReviewKey(props.row, scope.row))]"
                      effect="light"
                    >
                      {{ reviewStatusText[getReviewStatus(buildReviewKey(props.row, scope.row))] }}
                    </el-tag>
                    <el-button
                      text
                      class="workspace-btn workspace-btn--text record-text-btn"
                      @click="cycleReviewStatus(buildReviewKey(props.row, scope.row))"
                    >
                      {{ nextReviewActionText(buildReviewKey(props.row, scope.row)) }}
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="跟进分类" width="180">
                <template v-slot:default="scope">
                  <div class="cell-stack">
                    <el-select
                      v-if="getReviewStatus(buildReviewKey(props.row, scope.row)) === 'followUp'"
                      :model-value="getFollowUpCategory(buildReviewKey(props.row, scope.row))"
                      placeholder="选择分类"
                      size="small"
                      style="width: 150px"
                      @change="updateFollowUpCategory(buildReviewKey(props.row, scope.row), $event)"
                    >
                      <el-option
                        v-for="option in followUpCategoryOptions"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                      />
                    </el-select>
                    <span v-else class="follow-up-category-placeholder">仅待跟进可分类</span>
                    <span
                      v-if="getFollowUpCategory(buildReviewKey(props.row, scope.row))"
                      class="follow-up-category-text"
                    >
                      {{ getFollowUpCategoryLabel(buildReviewKey(props.row, scope.row)) }}
                    </span>
                  </div>
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

    <el-dialog v-model="detailDialogVisible" class="record-detail-dialog" :title="detailDialogTitle" width="780px">
      <div class="detail-dialog-copy">
        <div class="detail-dialog-tip">这里展示完整文本，方便直接做人工验收和问题复盘。</div>
        <el-button class="workspace-btn workspace-btn--ghost" @click="copyText(detailDialogContent, detailDialogTitle)">复制内容</el-button>
      </div>
      <pre class="detail-dialog-content">{{ detailDialogContent }}</pre>
    </el-dialog>

    <el-dialog v-model="acceptanceDialogVisible" class="record-detail-dialog" title="验收条目草稿" width="860px">
      <div class="detail-dialog-copy">
        <div class="detail-dialog-tip">这是一条可直接带去人工验收文档的草稿，结论项保持待人工填写，避免系统替你做判断。</div>
        <el-button class="workspace-btn workspace-btn--ghost" @click="copyText(acceptanceDialogContent, '验收条目草稿')">复制内容</el-button>
      </div>
      <pre class="detail-dialog-content">{{ acceptanceDialogContent }}</pre>
    </el-dialog>

    <el-dialog v-model="taskSuggestionDialogVisible" class="record-detail-dialog" title="后续任务建议清单" width="920px">
      <div class="detail-dialog-copy">
        <div class="detail-dialog-tip">这里按当前待跟进分类结果生成可直接复制的任务建议清单，适合快速贴到禅道或复盘消息里。</div>
        <el-button class="workspace-btn workspace-btn--ghost" @click="copyText(taskSuggestionContent, '后续任务建议清单')">复制内容</el-button>
      </div>
      <pre class="detail-dialog-content">{{ taskSuggestionContent }}</pre>
    </el-dialog>

    <el-dialog v-model="acceptanceBatchDialogVisible" class="record-detail-dialog acceptance-batch-dialog" :title="acceptanceBatchDialogTitle" width="1120px">
      <div class="acceptance-batch-layout">
        <section class="acceptance-batch-section">
          <div class="acceptance-batch-section-title">批次信息</div>
          <el-form :model="acceptanceBatchForm" label-width="100px" class="acceptance-batch-form">
            <div class="acceptance-batch-grid">
              <el-form-item label="批次名称">
                <el-input v-model="acceptanceBatchForm.batchName" placeholder="例如：2026-06-29 内部知识问答首轮验收" />
              </el-form-item>
              <el-form-item label="应用名称">
                <el-input v-model="acceptanceBatchForm.appName" placeholder="应用名称" />
              </el-form-item>
              <el-form-item label="场景类型">
                <el-input v-model="acceptanceBatchForm.sceneType" placeholder="内部知识问答 / 任务指导" />
              </el-form-item>
              <el-form-item label="验收日期">
                <el-date-picker
                  v-model="acceptanceBatchForm.testDate"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  format="YYYY-MM-DD HH:mm:ss"
                  placeholder="选择验收日期"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="知识库范围">
                <el-input v-model="acceptanceBatchForm.knowledgeScope" placeholder="本轮验收覆盖的知识库范围" />
              </el-form-item>
              <el-form-item label="发布版本">
                <el-input v-model="acceptanceBatchForm.releaseVersion" placeholder="例如：v0.1.3" />
              </el-form-item>
              <el-form-item label="实验版本">
                <el-input v-model="acceptanceBatchForm.experimentVersion" placeholder="例如：semantic-20260629" />
              </el-form-item>
              <el-form-item label="验收人">
                <el-input v-model="acceptanceBatchForm.testerName" placeholder="验收人" />
              </el-form-item>
            </div>
            <el-form-item label="版本说明">
              <el-input v-model="acceptanceBatchForm.versionRemark" type="textarea" :rows="2" placeholder="补充本轮版本说明" />
            </el-form-item>
            <el-form-item label="汇总结论">
              <el-input v-model="acceptanceBatchForm.summaryConclusion" type="textarea" :rows="3" placeholder="填写本轮验收的整体判断" />
            </el-form-item>
            <el-form-item label="后续动作">
              <el-input v-model="acceptanceBatchForm.nextAction" type="textarea" :rows="3" placeholder="例如：补知识、补提示词、补失败反馈" />
            </el-form-item>
          </el-form>
        </section>

        <section class="acceptance-batch-section">
          <div class="acceptance-batch-section-head">
            <div class="acceptance-batch-section-title">验收条目</div>
            <div class="acceptance-batch-section-actions">
              <div class="detail-dialog-tip">共 {{ acceptanceBatchForm.items.length }} 条，可逐条填写四项结论和后续动作。</div>
              <el-button class="workspace-btn workspace-btn--ghost" @click="appendCurrentRecordItemsToBatch">追加当前筛选记录</el-button>
            </div>
          </div>
          <div class="acceptance-item-list">
            <article v-for="item in acceptanceBatchForm.items" :key="item.localKey" class="acceptance-item-card">
              <div class="acceptance-item-head">
                <div>
                  <div class="acceptance-item-title">{{ item.testCaseNo }}</div>
                  <div class="saved-batch-meta">
                    {{ item.appName || '-' }} · {{ item.libName || '-' }} · {{ item.modelName || '未知模型' }} · {{ item.costTime ?? '-' }}ms
                  </div>
                </div>
                <div class="priority-tag-group">
                  <span class="priority-tag priority-tag--info">{{ item.invokeStatus || '-' }}</span>
                  <span v-if="item.followUpCategory" class="priority-tag priority-tag--warning">{{ getFollowUpLabel(item.followUpCategory) }}</span>
                </div>
              </div>
              <div class="acceptance-item-block">
                <div class="priority-label">用户问题</div>
                <div class="priority-text">{{ item.userQuestion || '-' }}</div>
              </div>
              <div class="acceptance-item-block">
                <div class="priority-label">回答摘要</div>
                <div class="priority-text">{{ item.actualAnswerSummary || '-' }}</div>
              </div>
              <div class="acceptance-item-block" v-if="item.failReason">
                <div class="priority-label">失败原因</div>
                <div class="priority-text priority-text--danger">{{ item.failReason }}</div>
              </div>
              <el-form :model="item" label-width="112px" class="acceptance-item-form">
                <el-form-item label="期望知识点">
                  <el-input v-model="item.expectedKnowledge" type="textarea" :rows="2" placeholder="人工补充期望知识点或期望行为" />
                </el-form-item>
                <div class="acceptance-item-grid">
                  <el-form-item label="命中问题">
                    <el-select v-model="item.hitConclusion" placeholder="选择结论">
                      <el-option v-for="option in conclusionOptions" :key="option" :label="option" :value="option" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="可信">
                    <el-select v-model="item.groundedConclusion" placeholder="选择结论">
                      <el-option v-for="option in conclusionOptions" :key="option" :label="option" :value="option" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="易懂">
                    <el-select v-model="item.readableConclusion" placeholder="选择结论">
                      <el-option v-for="option in conclusionOptions" :key="option" :label="option" :value="option" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="失败体面">
                    <el-select v-model="item.gracefulFailureConclusion" placeholder="选择结论">
                      <el-option v-for="option in conclusionOptions" :key="option" :label="option" :value="option" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="HitRate@5">
                    <el-select v-model="item.hitRateConclusion" placeholder="选择结论">
                      <el-option v-for="option in conclusionOptions" :key="option" :label="option" :value="option" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="Completeness">
                    <el-select v-model="item.completenessConclusion" placeholder="选择结论">
                      <el-option v-for="option in conclusionOptions" :key="option" :label="option" :value="option" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="跟进分类">
                    <el-select v-model="item.followUpCategory" clearable placeholder="选择分类">
                      <el-option v-for="option in followUpCategoryOptions" :key="option.value" :label="option.label" :value="option.value" />
                    </el-select>
                  </el-form-item>
                </div>
                <el-form-item label="跟进动作">
                  <el-input v-model="item.followUpAction" type="textarea" :rows="2" placeholder="例如：补充制度文档、优化切分参数、补失败反馈" />
                </el-form-item>
                <el-form-item label="备注">
                  <el-input v-model="item.remark" type="textarea" :rows="2" placeholder="补充人工判断、风险说明或复现信息" />
                </el-form-item>
              </el-form>
            </article>
          </div>
        </section>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="exportAcceptanceBatchDialog">导出 Markdown</el-button>
          <el-button class="workspace-btn workspace-btn--ghost" @click="acceptanceBatchDialogVisible = false">关闭</el-button>
          <el-button class="workspace-btn workspace-btn--primary" @click="saveAcceptanceBatch">保存批次</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="acceptanceCompareDialogVisible" class="record-detail-dialog acceptance-batch-dialog" title="正式验收对比" width="1100px">
      <div v-if="acceptanceCompareState.left && acceptanceCompareState.right" class="acceptance-compare-layout">
        <section class="acceptance-batch-section">
          <div class="acceptance-batch-section-title">对比概览</div>
          <div class="acceptance-compare-grid">
            <article class="acceptance-compare-card">
              <div class="saved-batch-title">{{ acceptanceCompareState.left.batchName }}</div>
              <div class="saved-batch-meta">发布版本：{{ acceptanceCompareState.left.releaseVersion || '-' }} · 实验版本：{{ acceptanceCompareState.left.experimentVersion || '-' }}</div>
              <div class="saved-batch-summary">
                <span>总条目：{{ acceptanceCompareState.left.itemCount }}</span>
                <span>全部通过：{{ acceptanceCompareState.left.passCount }}</span>
                <span>待跟进：{{ acceptanceCompareState.left.followUpCount }}</span>
              </div>
            </article>
            <article class="acceptance-compare-card">
              <div class="saved-batch-title">{{ acceptanceCompareState.right.batchName }}</div>
              <div class="saved-batch-meta">发布版本：{{ acceptanceCompareState.right.releaseVersion || '-' }} · 实验版本：{{ acceptanceCompareState.right.experimentVersion || '-' }}</div>
              <div class="saved-batch-summary">
                <span>总条目：{{ acceptanceCompareState.right.itemCount }}</span>
                <span>全部通过：{{ acceptanceCompareState.right.passCount }}</span>
                <span>待跟进：{{ acceptanceCompareState.right.followUpCount }}</span>
              </div>
            </article>
          </div>
        </section>

        <section class="acceptance-batch-section">
          <div class="acceptance-batch-section-title">四项体验与修复分类对比</div>
          <div class="acceptance-compare-table">
            <table class="compare-table">
              <thead>
                <tr>
                  <th>维度</th>
                  <th>{{ acceptanceCompareState.left.batchName }}</th>
                  <th>{{ acceptanceCompareState.right.batchName }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in acceptanceCompareRows" :key="row.label">
                  <td>{{ row.label }}</td>
                  <td>{{ row.left }}</td>
                  <td>{{ row.right }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="acceptance-batch-section">
          <div class="acceptance-batch-section-title">对比结论草稿</div>
          <pre class="detail-dialog-content">{{ acceptanceCompareDraftContent }}</pre>
        </section>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="copyText(acceptanceCompareDraftContent, '正式验收对比草稿')">复制内容</el-button>
          <el-button class="workspace-btn workspace-btn--ghost" @click="exportAcceptanceCompareDraft">导出 Markdown</el-button>
          <el-button class="workspace-btn workspace-btn--ghost" @click="acceptanceCompareDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="defaultBatchRunnerVisible" class="record-detail-dialog" title="执行默认问题集跑测" width="760px">
      <el-form :model="defaultBatchRunnerForm" label-width="110px" class="acceptance-batch-form">
        <el-form-item label="选择应用">
          <el-select v-model="defaultBatchRunnerForm.appId" placeholder="选择要跑测的应用" style="width: 100%">
            <el-option v-for="app in availableApps" :key="app.id" :label="app.appName" :value="app.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次名称">
          <el-input v-model="defaultBatchRunnerForm.batchName" placeholder="例如：2026-06-30 默认问题集首轮正式验收" />
        </el-form-item>
        <el-form-item label="场景类型">
          <el-input v-model="defaultBatchRunnerForm.sceneType" placeholder="内部知识问答 / 任务指导" />
        </el-form-item>
        <el-form-item label="验收人">
          <el-input v-model="defaultBatchRunnerForm.testerName" placeholder="填写本轮验收人" />
        </el-form-item>
        <el-form-item label="汇总结论">
          <el-input v-model="defaultBatchRunnerForm.summaryConclusion" type="textarea" :rows="3" placeholder="可以先留空，跑完后再补齐结论。" />
        </el-form-item>
        <el-form-item label="后续动作">
          <el-input v-model="defaultBatchRunnerForm.nextAction" type="textarea" :rows="3" placeholder="例如：跑完后重点检查失败体面与补切分问题。" />
        </el-form-item>
      </el-form>
      <div class="detail-dialog-tip">
        这一步会直接对当前应用执行默认问题集，生成真实调用记录、真实回答，并自动落成正式验收批次。
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="defaultBatchRunnerVisible = false">取消</el-button>
          <el-button class="workspace-btn workspace-btn--primary" :loading="defaultBatchRunning" @click="runDefaultAcceptanceBatch">
            开始真实跑测
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name='InvokeRecordManage' lang='ts'>
import { computed, reactive, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useTable } from '@/hooks/useTable';
import {
  listRagAcceptanceBatchApi,
  pageInvokeRecordApi,
  queryInvokeRecordOverviewApi,
  queryRagAcceptanceBatchDetailApi,
  runDefaultRagAcceptanceBatchApi,
  saveRagAcceptanceBatchApi
} from '@/api/workspace/invokeRecordApi';
import { pageAppApi } from '@/api/workspace/appApi';
import { getItem, saveItem } from '@/util/storageUtil';
let searchFormData = reactive({
  appName: "",
  userInputKeyword: "",
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

const quickView = ref<'all' | 'fail' | 'slow' | 'long' | 'followUp' | 'reviewed'>('all')
const detailDialogVisible = ref(false)
const detailDialogTitle = ref('')
const detailDialogContent = ref('')
const acceptanceDialogVisible = ref(false)
const acceptanceDialogContent = ref('')
const taskSuggestionDialogVisible = ref(false)
const taskSuggestionContent = ref('')
const acceptanceBatchDialogVisible = ref(false)
const acceptanceBatchDialogTitle = ref('保存验收批次')
const acceptanceCompareDialogVisible = ref(false)
const defaultBatchRunnerVisible = ref(false)
const defaultBatchRunning = ref(false)
const followUpCategoryView = ref<FollowUpCategory | ''>('')
const savedAcceptanceBatches = ref<any[]>([])
const selectedAcceptanceBatchIds = ref<Array<string | number>>([])
const availableApps = ref<any[]>([])
const acceptanceCompareState = reactive<{ left: any | null; right: any | null }>({
  left: null,
  right: null
})
const defaultBatchRunnerForm = reactive({
  appId: '',
  batchName: '',
  sceneType: '内部知识问答',
  testerName: '',
  summaryConclusion: '',
  nextAction: ''
})
const acceptanceBatchForm = reactive<any>({
  id: null,
  batchName: '',
  appName: '',
  sceneType: '内部知识问答',
  knowledgeScope: '',
  releaseVersion: '',
  experimentVersion: '',
  versionRemark: '',
  quickView: '',
  quickViewDesc: '',
  testerName: '',
  testDate: '',
  summaryConclusion: '',
  nextAction: '',
  items: []
})
const conclusionOptions = ['通过', '不通过', '待确认']
const defaultAcceptanceQuestionPool = [
  { testCaseNo: 'TC-01', questionType: '标准知识问答', userQuestion: '这个知识库当前主要覆盖哪些主题？', expectedKnowledge: '能说明当前知识库覆盖范围、主题边界或主要文档类型。' },
  { testCaseNo: 'TC-02', questionType: '标准知识问答', userQuestion: '某个制度或流程的核心结论是什么？', expectedKnowledge: '能直接给结论，并指出依据或关键规则。' },
  { testCaseNo: 'TC-03', questionType: '任务指导', userQuestion: '我应该从哪里开始完成这个任务？', expectedKnowledge: '给出开始入口、前置准备和建议顺序。' },
  { testCaseNo: 'TC-04', questionType: '任务指导', userQuestion: '如果我要把这个流程走通，先后步骤应该是什么？', expectedKnowledge: '按顺序说明步骤，并提示关键注意事项。' },
  { testCaseNo: 'TC-05', questionType: '模糊提问', userQuestion: '这个事情我该怎么弄？', expectedKnowledge: '先识别问题模糊，再引导补充关键信息或给出安全的初始路径。' },
  { testCaseNo: 'TC-06', questionType: '模糊提问', userQuestion: '我现在不知道从哪开始，你建议我先看什么？', expectedKnowledge: '给出最小起步建议和优先阅读内容。' },
  { testCaseNo: 'TC-07', questionType: '未命中知识', userQuestion: '询问一个明确不在知识库范围内的问题', expectedKnowledge: '明确说明当前缺少依据，不要强答，并给出下一步建议。' },
  { testCaseNo: 'TC-08', questionType: '未命中知识', userQuestion: '故意使用模糊、歧义、缺少关键词的问题', expectedKnowledge: '提醒问题信息不足，建议补充关键词或制度名称。' },
  { testCaseNo: 'TC-09', questionType: '失败场景', userQuestion: '检索结果为空时，系统会怎么反馈？', expectedKnowledge: '反馈应说明未找到足够依据，并给出可理解的下一步。' },
  { testCaseNo: 'TC-10', questionType: '失败场景', userQuestion: '链路异常时，系统会怎么提示我？', expectedKnowledge: '反馈应避免报错堆栈，提示稍后重试或换更明确问法。' }
] as const
const reviewStatusStoreKey = 'invoke-record-review-status'
const followUpCategoryStoreKey = 'invoke-record-follow-up-category'
const reviewStatusMap = ref<Record<string, ReviewStatus>>(loadReviewStatusMap())
const followUpCategoryMap = ref<Record<string, FollowUpCategory>>(loadFollowUpCategoryMap())
const reviewStatusText: Record<ReviewStatus, string> = {
  pending: '待复盘',
  reviewed: '已复盘',
  followUp: '待跟进'
}
const reviewStatusTagType: Record<ReviewStatus, 'info' | 'success' | 'warning'> = {
  pending: 'info',
  reviewed: 'success',
  followUp: 'warning'
}
const followUpCategoryOptions = [
  { value: 'knowledge', label: '补知识' },
  { value: 'chunking', label: '补切分' },
  { value: 'prompt', label: '补提示词' },
  { value: 'ui', label: '补展示' },
  { value: 'observe', label: '补观测' },
  { value: 'other', label: '其他' }
] as const
const followUpCategoryLabelMap: Record<FollowUpCategory, string> = {
  knowledge: '补知识',
  chunking: '补切分',
  prompt: '补提示词',
  ui: '补展示',
  observe: '补观测',
  other: '其他'
}
const followUpCategoryDescMap: Record<FollowUpCategory, string> = {
  knowledge: '知识源缺失、知识范围不完整、资料不够支撑回答',
  chunking: '切分粒度不合适、召回片段断裂、上下文拼接不理想',
  prompt: '提示词约束不够、回答风格不稳、失败反馈不够体面',
  ui: '前端展示不清楚、来源说明不够、验收动作不顺手',
  observe: '日志字段不够、缺少关键观测、排障信息不完整',
  other: '暂时无法归入以上分类的问题'
}

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

const filteredRows = computed(() => {
  const rows = tableData.rows || []
  if (quickView.value === 'fail') {
    return rows.filter((row: any) => Number(row.status) === 0)
  }
  if (quickView.value === 'slow') {
    return rows.filter((row: any) => Number(row.costTime || 0) >= 5000)
  }
  if (quickView.value === 'long') {
    return rows.filter((row: any) =>
      (row.detailList || []).some((detail: any) => String(detail.assistantMessage || '').length >= 200)
    )
  }
  if (quickView.value === 'followUp') {
    return rows.filter((row: any) =>
      (row.detailList || []).some((detail: any) => matchFollowUpFilter(row, detail))
    )
  }
  if (quickView.value === 'reviewed') {
    return rows.filter((row: any) =>
      (row.detailList || []).some((detail: any) => getReviewStatus(buildReviewKey(row, detail)) === 'reviewed')
    )
  }
  return rows
})

const failRowCount = computed(() => (tableData.rows || []).filter((row: any) => Number(row.status) === 0).length)
const slowRowCount = computed(() => (tableData.rows || []).filter((row: any) => Number(row.costTime || 0) >= 5000).length)
const longAnswerRowCount = computed(() =>
  (tableData.rows || []).filter((row: any) =>
    (row.detailList || []).some((detail: any) => String(detail.assistantMessage || '').length >= 200)
  ).length
)

const priorityReviewList = computed(() => {
  return filteredRows.value
    .flatMap((row: any) => {
      const detailList = row.detailList || []
      return detailList.map((detail: any, index: number) => {
        const riskTags = buildRiskTags(row, detail)
        return {
          key: `${row.id}-${index + 1}`,
          row,
          detail,
          riskTags,
          riskScore: riskTags.reduce((sum: number, tag: any) => sum + tag.score, 0)
        }
      })
    })
    .filter((item: any) => item.riskTags.length > 0)
    .sort((a: any, b: any) => {
      if (b.riskScore !== a.riskScore) {
        return b.riskScore - a.riskScore
      }
      return Number(b.detail.costTime || b.row.costTime || 0) - Number(a.detail.costTime || a.row.costTime || 0)
    })
    .slice(0, 8)
})

const followUpCount = computed(() => {
  return Object.values(reviewStatusMap.value).filter((status) => status === 'followUp').length
})

const categorizedFollowUpCount = computed(() => {
  return Object.entries(reviewStatusMap.value).filter(([key, status]) =>
    status === 'followUp' && Boolean(followUpCategoryMap.value[key])
  ).length
})

const followUpCategoryStats = computed(() => {
  return followUpCategoryOptions
    .map((option) => {
      const count = Object.entries(reviewStatusMap.value).filter(([key, status]) =>
        status === 'followUp' &&
        followUpCategoryMap.value[key] === option.value &&
        matchFollowUpCategoryView(option.value)
      ).length
      return {
        key: option.value,
        label: option.label,
        description: followUpCategoryDescMap[option.value],
        count
      }
    })
    .filter((item) => item.count > 0)
    .sort((a, b) => b.count - a.count)
})

const currentQuickViewDesc = computed(() => {
  if (quickView.value === 'fail') {
    return '当前仅显示失败记录，适合优先排查失败体面、错误暴露和明显不可用问题。'
  }
  if (quickView.value === 'slow') {
    return '当前仅显示慢请求，适合排查耗时过长、上下文过重或链路抖动问题。'
  }
  if (quickView.value === 'long') {
    return '当前仅显示长回答，适合重点检查是否真正答题、是否有依据，以及是否存在过度铺陈。'
  }
  if (quickView.value === 'followUp') {
    return '当前仅显示已标记为待跟进的记录，适合集中处理需要继续补知识、补提示词或补链路观测的问题。'
  }
  if (quickView.value === 'reviewed') {
    return '当前仅显示已复盘记录，适合回看已经检查过的样本，确认结论是否需要再整理进验收文档。'
  }
  return '当前显示全部记录，适合做完整抽样和总体复盘。'
})

const acceptanceCompareRows = computed(() => {
  if (!acceptanceCompareState.left || !acceptanceCompareState.right) {
    return []
  }
  const leftStats = buildAcceptanceBatchStats(acceptanceCompareState.left)
  const rightStats = buildAcceptanceBatchStats(acceptanceCompareState.right)
  return [
    { label: '命中问题通过', left: leftStats.hitPass, right: rightStats.hitPass },
    { label: '可信通过', left: leftStats.groundedPass, right: rightStats.groundedPass },
    { label: '易懂通过', left: leftStats.readablePass, right: rightStats.readablePass },
    { label: '失败体面通过', left: leftStats.gracefulPass, right: rightStats.gracefulPass },
    { label: '补知识', left: leftStats.followUp.knowledge, right: rightStats.followUp.knowledge },
    { label: '补切分', left: leftStats.followUp.chunking, right: rightStats.followUp.chunking },
    { label: '补提示词', left: leftStats.followUp.prompt, right: rightStats.followUp.prompt },
    { label: '补展示', left: leftStats.followUp.ui, right: rightStats.followUp.ui },
    { label: '补观测', left: leftStats.followUp.observe, right: rightStats.followUp.observe },
    { label: '其他待跟进', left: leftStats.followUp.other, right: rightStats.followUp.other }
  ]
})

const acceptanceCompareDraftContent = computed(() => {
  if (!acceptanceCompareState.left || !acceptanceCompareState.right) {
    return ''
  }
  return buildAcceptanceCompareMarkdown(acceptanceCompareState.left, acceptanceCompareState.right)
})

function loadOverview() {
  queryInvokeRecordOverviewApi().then(result => {
    Object.assign(overview, result.data || {})
  })
}

function loadSavedAcceptanceBatches() {
  listRagAcceptanceBatchApi().then(result => {
    savedAcceptanceBatches.value = result.data || []
  })
}

function loadAvailableApps() {
  pageAppApi({
    pageNow: 1,
    pageSize: 100,
    appName: ''
  }).then(result => {
    availableApps.value = result.data || []
  })
}

async function copyText(text: string, label: string) {
  try {
    await navigator.clipboard.writeText(String(text || ''))
    ElMessage.success(`已复制${label}`)
  } catch {
    ElMessage.error(`复制${label}失败`)
  }
}

function exportCurrentRows() {
  if (!filteredRows.value.length) {
    ElMessage.warning('当前没有可导出的调用记录')
    return
  }
  const lines = [
    '# 调用记录导出',
    `> 导出时间：${new Date().toLocaleString()}`,
    `> 当前记录数：${filteredRows.value.length}`,
    `> 当前聚焦：${currentQuickViewDesc.value}`,
    '',
    '---',
    ''
  ]
  filteredRows.value.forEach((row: any) => {
    lines.push(`## 记录 ${row.id}`)
    lines.push(`- 应用：${row.appName || '-'}`)
    lines.push(`- 知识库：${row.libName || '-'}`)
    lines.push(`- 状态：${row.statusDesc || '-'}`)
    lines.push(`- 耗时：${row.costTime ?? '-'} ms`)
    lines.push(`- 开始时间：${row.startTime || '-'}`)
    lines.push(`- 结束时间：${row.endTime || '-'}`)
    if (row.failReason) {
      lines.push(`- 失败原因：${row.failReason}`)
    }
    lines.push('')
    ;(row.detailList || []).forEach((detail: any, index: number) => {
      lines.push(`### 明细 ${index + 1}`)
      lines.push(`- 模型：${detail.modelName || '-'}`)
      lines.push(`- 状态：${detail.statusDesc || '-'}`)
      lines.push(`- Token：${detail.costToken ?? '-'}`)
      lines.push(`- 查询词：${detail.userInput || '-'}`)
      lines.push('')
      lines.push('```text')
      lines.push(String(detail.assistantMessage || ''))
      lines.push('```')
      lines.push('')
    })
    lines.push('---')
    lines.push('')
  })
  const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `invoke-records-${Date.now()}.md`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('已导出当前调用记录')
}

function buildAcceptanceEntry(row: any, detail: any, index: number) {
  return [
    `| TC-${String(row.id)}-${index + 1} | 待分类 | ${sanitizeTableCell(detail.userInput)} | 待补充 | ${buildAnswerSummary(detail.assistantMessage)} | 待人工判断 | 待人工判断 | 待人工判断 | ${Number(detail.status) === 0 ? '重点检查' : '待人工判断'} | 待人工判断 | 待人工判断 | 记录ID=${row.id}；应用=${sanitizeInline(row.appName)}；知识库=${sanitizeInline(row.libName)}；状态=${sanitizeInline(detail.statusDesc || row.statusDesc)}；耗时=${detail.costTime ?? row.costTime ?? '-'}ms |`,
    '',
    `补充信息：`,
    `- 原始失败原因：${sanitizeInline(detail.failReason || row.failReason || '无')}`,
    `- 模型：${sanitizeInline(detail.modelName || '未知')}`,
    `- Token：${detail.costToken ?? '-'}`,
    `- 开始时间：${sanitizeInline(detail.startTime || row.startTime || '-')}`,
    `- 结束时间：${sanitizeInline(detail.endTime || row.endTime || '-')}`,
    `- 原始回答全文：`,
    '```text',
    String(detail.assistantMessage || ''),
    '```'
  ].join('\n')
}

function exportAcceptanceDraft() {
  if (!filteredRows.value.length) {
    ElMessage.warning('当前没有可导出的验收草稿')
    return
  }
  const records = filteredRows.value.flatMap((row: any) => {
    const detailList = row.detailList || []
    if (!detailList.length) {
      return [
        `| TC-${String(row.id)}-1 | 待分类 | - | 待补充 | - | 待人工判断 | 待人工判断 | 待人工判断 | ${Number(row.status) === 0 ? '重点检查' : '待人工判断'} | 待人工判断 | 待人工判断 | 记录ID=${row.id}；应用=${sanitizeInline(row.appName)}；知识库=${sanitizeInline(row.libName)}；状态=${sanitizeInline(row.statusDesc)}；耗时=${row.costTime ?? '-'}ms |`
      ]
    }
    return detailList.map((detail: any, index: number) => buildAcceptanceEntry(row, detail, index))
  })

  const lines = [
    '# RAG MVP 测试问题集与效果记录',
    '',
    '## 1. 当前导出说明',
    '',
    `- 导出时间：${new Date().toLocaleString()}`,
    `- 当前聚焦：${currentQuickViewDesc.value}`,
    `- 当前记录数：${filteredRows.value.length}`,
    `- 导出目的：从调用记录页生成验收草稿，便于后续人工补齐“期望知识点 / 验收结论 / 汇总结论”`,
    '',
    '## 2. 当前版本信息',
    '',
    '- 测试日期：',
    '- 测试人：',
    '- 应用名称：',
    '- 场景类型：内部知识问答 / 任务指导',
    '- 知识库范围：',
    '- 发布版本：',
    '- 实验版本：',
    '- 版本说明：',
    '',
    '## 3. 调用记录转验收表',
    '',
    '| 编号 | 问题类型 | 用户问题 | 期望知识点 / 期望行为 | 实际回答摘要 | 命中问题 | 可信 | 易懂 | 失败体面 | HitRate@5 | Completeness | 备注 |',
    '| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |',
    ...records,
    '',
    '## 4. 汇总结论',
    '',
    `- 当前导出记录数：${filteredRows.value.length}`,
    `- 失败记录数：${filteredRows.value.filter((row: any) => Number(row.status) === 0).length}`,
    `- 慢请求数：${filteredRows.value.filter((row: any) => Number(row.costTime || 0) >= 5000).length}`,
    `- 长回答记录数：${filteredRows.value.filter((row: any) => (row.detailList || []).some((detail: any) => String(detail.assistantMessage || '').length >= 200)).length}`,
    '- 命中问题通过数：待人工填写',
    '- 可信通过数：待人工填写',
    '- 易懂通过数：待人工填写',
    '- 失败体面通过数：待人工填写',
    '',
    '## 5. 后续动作',
    '',
    '- 需要补知识的内容：',
    '- 需要优化切分的内容：',
    '- 需要优化检索/重排的内容：',
    '- 需要优化 Prompt 的内容：',
    '- 需要优化前端展示的内容：',
    '- 需要补日志或观测的内容：'
  ]

  downloadMarkdown(lines.join('\n'), `rag-mvp-acceptance-draft-${Date.now()}.md`)
  ElMessage.success('已导出验收草稿')
}

function openSaveAcceptanceBatchDialog() {
  const items = buildAcceptanceBatchItems()
  if (!items.length) {
    ElMessage.warning('当前没有可保存的验收记录')
    return
  }
  resetAcceptanceBatchForm()
  acceptanceBatchDialogTitle.value = '保存验收批次'
  acceptanceBatchForm.quickView = quickView.value
  acceptanceBatchForm.quickViewDesc = currentQuickViewDesc.value
  acceptanceBatchForm.batchName = `${new Date().toLocaleDateString()} 验收批次`
  acceptanceBatchForm.items = items
  acceptanceBatchDialogVisible.value = true
}

function openTemplateAcceptanceBatchDialog() {
  defaultBatchRunnerForm.appId = availableApps.value[0]?.id || ''
  defaultBatchRunnerForm.batchName = `${new Date().toLocaleDateString()} 默认问题集验收`
  defaultBatchRunnerForm.sceneType = '内部知识问答'
  defaultBatchRunnerForm.testerName = ''
  defaultBatchRunnerForm.summaryConclusion = ''
  defaultBatchRunnerForm.nextAction = ''
  defaultBatchRunnerVisible.value = true
}

function runDefaultAcceptanceBatch() {
  if (!defaultBatchRunnerForm.appId) {
    ElMessage.warning('请先选择应用')
    return
  }
  if (!String(defaultBatchRunnerForm.batchName || '').trim()) {
    ElMessage.warning('请先填写验收批次名称')
    return
  }
  defaultBatchRunning.value = true
  runDefaultRagAcceptanceBatchApi({
    appId: defaultBatchRunnerForm.appId,
    batchName: defaultBatchRunnerForm.batchName,
    sceneType: defaultBatchRunnerForm.sceneType,
    testerName: defaultBatchRunnerForm.testerName,
    summaryConclusion: defaultBatchRunnerForm.summaryConclusion,
    nextAction: defaultBatchRunnerForm.nextAction
  }).then((result) => {
    ElMessage.success('已完成默认问题集真实跑测，并生成正式验收批次')
    defaultBatchRunnerVisible.value = false
    loadSavedAcceptanceBatches()
    if (result.data) {
      openSavedAcceptanceBatch(result.data)
    }
  }).finally(() => {
    defaultBatchRunning.value = false
  })
}

function buildAcceptanceBatchItems() {
  return filteredRows.value.flatMap((row: any) => {
    const detailList = row.detailList || []
    return detailList.map((detail: any, index: number) => {
      const key = buildReviewKey(row, detail)
      return {
        localKey: key,
        id: null,
        invokeRecordId: row.id,
        invokeRecordDetailId: detail.id,
        testCaseNo: `TC-${row.id}-${index + 1}`,
        questionType: '',
        userQuestion: detail.userInput || '',
        expectedKnowledge: '',
        actualAnswerSummary: buildAnswerSummary(detail.assistantMessage),
        actualAnswer: detail.assistantMessage || '',
        failReason: detail.failReason || row.failReason || '',
        hitConclusion: '',
        groundedConclusion: '',
        readableConclusion: '',
        gracefulFailureConclusion: Number(detail.status) === 0 ? '待确认' : '',
        hitRateConclusion: '',
        completenessConclusion: '',
        followUpCategory: getFollowUpCategory(key) || '',
        followUpAction: '',
        remark: '',
        invokeStatus: detail.statusDesc || row.statusDesc || '',
        modelName: detail.modelName || '',
        appName: row.appName || '',
        libName: row.libName || '',
        costTime: detail.costTime ?? row.costTime ?? null,
        costToken: detail.costToken ?? null
      }
    })
  })
}

function appendCurrentRecordItemsToBatch() {
  const currentItems = buildAcceptanceBatchItems()
  if (!currentItems.length) {
    ElMessage.warning('当前筛选结果没有可追加的调用记录')
    return
  }
  const existingKeys = new Set(acceptanceBatchForm.items.map((item: any) => item.localKey || item.testCaseNo))
  const newItems = currentItems.filter((item: any) => !existingKeys.has(item.localKey))
  if (!newItems.length) {
    ElMessage.success('当前筛选记录都已经在验收批次里了')
    return
  }
  acceptanceBatchForm.items = [...acceptanceBatchForm.items, ...newItems]
  ElMessage.success(`已追加 ${newItems.length} 条调用记录`)
}

function resetAcceptanceBatchForm() {
  Object.assign(acceptanceBatchForm, {
    id: null,
    batchName: '',
    appName: '',
    sceneType: '内部知识问答',
    knowledgeScope: '',
    releaseVersion: '',
    experimentVersion: '',
    versionRemark: '',
    quickView: '',
    quickViewDesc: '',
    testerName: '',
    testDate: '',
    summaryConclusion: '',
    nextAction: '',
    items: []
  })
}

function saveAcceptanceBatch() {
  if (!String(acceptanceBatchForm.batchName || '').trim()) {
    ElMessage.warning('请先填写验收批次名称')
    return
  }
  if (!acceptanceBatchForm.items.length) {
    ElMessage.warning('当前没有可保存的验收条目')
    return
  }
  const payload = {
    ...acceptanceBatchForm,
    items: acceptanceBatchForm.items.map((item: any) => ({
      id: item.id || null,
      invokeRecordId: item.invokeRecordId,
      invokeRecordDetailId: item.invokeRecordDetailId,
      testCaseNo: item.testCaseNo,
      questionType: item.questionType,
      userQuestion: item.userQuestion,
      expectedKnowledge: item.expectedKnowledge,
      actualAnswerSummary: item.actualAnswerSummary,
      actualAnswer: item.actualAnswer,
      failReason: item.failReason,
      hitConclusion: item.hitConclusion,
      groundedConclusion: item.groundedConclusion,
      readableConclusion: item.readableConclusion,
      gracefulFailureConclusion: item.gracefulFailureConclusion,
      hitRateConclusion: item.hitRateConclusion,
      completenessConclusion: item.completenessConclusion,
      followUpCategory: item.followUpCategory,
      followUpAction: item.followUpAction,
      remark: item.remark,
      invokeStatus: item.invokeStatus,
      modelName: item.modelName,
      appName: item.appName,
      libName: item.libName,
      costTime: item.costTime,
      costToken: item.costToken
    }))
  }
  saveRagAcceptanceBatchApi(payload).then(() => {
    ElMessage.success('已保存验收批次')
    acceptanceBatchDialogVisible.value = false
    loadSavedAcceptanceBatches()
  })
}

function openSavedAcceptanceBatch(id: string | number) {
  queryRagAcceptanceBatchDetailApi(id).then(result => {
    const data = result.data || {}
    resetAcceptanceBatchForm()
    acceptanceBatchDialogTitle.value = '查看并编辑验收批次'
    Object.assign(acceptanceBatchForm, {
      id: data.id || null,
      batchName: data.batchName || '',
      appName: data.appName || '',
      sceneType: data.sceneType || '',
      knowledgeScope: data.knowledgeScope || '',
      releaseVersion: data.releaseVersion || '',
      experimentVersion: data.experimentVersion || '',
      activeExperimentId: data.activeExperimentId || null,
      activeExperimentName: data.activeExperimentName || '',
      activeSplitStrategy: data.activeSplitStrategy || '',
      versionRemark: data.versionRemark || '',
      quickView: data.quickView || '',
      quickViewDesc: data.quickViewDesc || '',
      testerName: data.testerName || '',
      testDate: data.testDate ? formatDateForPicker(data.testDate) : '',
      summaryConclusion: data.summaryConclusion || '',
      nextAction: data.nextAction || '',
      items: (data.items || []).map((item: any, index: number) => ({
        ...item,
        localKey: item.id || `${data.id}-${index + 1}`
      }))
    })
    acceptanceBatchDialogVisible.value = true
  })
}

function toggleAcceptanceBatchSelection(id: string | number) {
  if (selectedAcceptanceBatchIds.value.includes(id)) {
    selectedAcceptanceBatchIds.value = selectedAcceptanceBatchIds.value.filter((item) => item !== id)
    return
  }
  if (selectedAcceptanceBatchIds.value.length >= 2) {
    selectedAcceptanceBatchIds.value = [...selectedAcceptanceBatchIds.value.slice(1), id]
    return
  }
  selectedAcceptanceBatchIds.value = [...selectedAcceptanceBatchIds.value, id]
}

async function openAcceptanceCompareDialog() {
  if (selectedAcceptanceBatchIds.value.length !== 2) {
    ElMessage.warning('请先选择两轮正式验收批次')
    return
  }
  const [leftId, rightId] = selectedAcceptanceBatchIds.value
  const [leftResult, rightResult] = await Promise.all([
    queryRagAcceptanceBatchDetailApi(leftId),
    queryRagAcceptanceBatchDetailApi(rightId)
  ])
  acceptanceCompareState.left = leftResult.data || null
  acceptanceCompareState.right = rightResult.data || null
  acceptanceCompareDialogVisible.value = true
}

function exportAcceptanceCompareDraft() {
  if (!acceptanceCompareState.left || !acceptanceCompareState.right) {
    if (selectedAcceptanceBatchIds.value.length !== 2) {
      ElMessage.warning('请先选择两轮正式验收批次')
      return
    }
    openAcceptanceCompareDialog().then(() => {
      if (acceptanceCompareDraftContent.value) {
        downloadMarkdown(acceptanceCompareDraftContent.value, `rag-acceptance-compare-${Date.now()}.md`)
        ElMessage.success('已导出对比报告')
      }
    })
    return
  }
  downloadMarkdown(acceptanceCompareDraftContent.value, `rag-acceptance-compare-${Date.now()}.md`)
  ElMessage.success('已导出对比报告')
}

function exportAcceptanceRepairDraft(batchId: string | number) {
  queryRagAcceptanceBatchDetailApi(batchId).then((result) => {
    const batch = result.data || {}
    const content = buildAcceptanceRepairDraft(batch)
    downloadMarkdown(content, `rag-acceptance-repair-${batchId}.md`)
    ElMessage.success('已导出修复建议')
  })
}

function exportSavedAcceptanceBatch(batch: any) {
  queryRagAcceptanceBatchDetailApi(batch.id).then(result => {
    downloadMarkdown(buildAcceptanceBatchMarkdown(result.data || {}), `rag-acceptance-batch-${batch.id}.md`)
    ElMessage.success('已导出验收批次')
  })
}

function exportAcceptanceBatchDialog() {
  if (!acceptanceBatchForm.items.length) {
    ElMessage.warning('当前没有可导出的验收条目')
    return
  }
  downloadMarkdown(buildAcceptanceBatchMarkdown(acceptanceBatchForm), `rag-acceptance-batch-${Date.now()}.md`)
  ElMessage.success('已导出验收批次')
}

function buildAcceptanceBatchMarkdown(batch: any) {
  const lines = [
    '# RAG MVP 测试问题集与效果记录',
    '',
    '## 1. 当前版本信息',
    '',
    `- 验收批次：${batch.batchName || '-'}`,
    `- 测试日期：${batch.testDate ? formatDateTime(batch.testDate) : '-'}`,
    `- 测试人：${batch.testerName || '-'}`,
    `- 应用名称：${batch.appName || '-'}`,
    `- 场景类型：${batch.sceneType || '-'}`,
    `- 知识库范围：${batch.knowledgeScope || '-'}`,
    `- 发布版本：${batch.releaseVersion || '-'}`,
    `- 实验版本：${batch.experimentVersion || '-'}`,
    `- 生效实验版本ID：${batch.activeExperimentId || '-'}`,
    `- 生效实验版本名称：${batch.activeExperimentName || '-'}`,
    `- 生效切分策略：${batch.activeSplitStrategy || '-'}`,
    `- 版本说明：${batch.versionRemark || '-'}`,
    '',
    '## 2. 调用记录转验收表',
    '',
    '| 编号 | 问题类型 | 用户问题 | 期望知识点 / 期望行为 | 实际回答摘要 | 命中问题 | 可信 | 易懂 | 失败体面 | HitRate@5 | Completeness | 备注 |',
    '| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |'
  ]
  ;(batch.items || []).forEach((item: any) => {
    lines.push(`| ${sanitizeTableCell(item.testCaseNo)} | ${sanitizeTableCell(item.questionType || '待分类')} | ${sanitizeTableCell(item.userQuestion)} | ${sanitizeTableCell(item.expectedKnowledge || '待补充')} | ${sanitizeTableCell(item.actualAnswerSummary || '-')} | ${sanitizeTableCell(item.hitConclusion || '待确认')} | ${sanitizeTableCell(item.groundedConclusion || '待确认')} | ${sanitizeTableCell(item.readableConclusion || '待确认')} | ${sanitizeTableCell(item.gracefulFailureConclusion || '待确认')} | ${sanitizeTableCell(item.hitRateConclusion || '待确认')} | ${sanitizeTableCell(item.completenessConclusion || '待确认')} | ${sanitizeTableCell(item.remark || '-')} |`)
    lines.push('')
    lines.push('补充信息：')
    lines.push(`- 原始失败原因：${sanitizeInline(item.failReason || '无')}`)
    lines.push(`- 模型：${sanitizeInline(item.modelName || '未知模型')}`)
    lines.push(`- Token：${item.costToken ?? '-'}`)
    lines.push(`- 跟进分类：${sanitizeInline(getFollowUpLabel(item.followUpCategory))}`)
    lines.push(`- 跟进动作：${sanitizeInline(item.followUpAction || '-')}`)
    lines.push('```text')
    lines.push(String(item.actualAnswer || ''))
    lines.push('```')
    lines.push('')
  })
  lines.push('## 3. 汇总结论')
  lines.push('')
  lines.push(`- 汇总结论：${batch.summaryConclusion || '-'}`)
  lines.push(`- 后续动作：${batch.nextAction || '-'}`)
  return lines.join('\n')
}

function exportFollowUpTaskDraft() {
  const taskSuggestion = buildFollowUpTaskSuggestion()
  if (!taskSuggestion) {
    ElMessage.warning('当前没有可导出的待跟进任务草稿')
    return
  }
  downloadMarkdown(taskSuggestion, `rag-follow-up-task-draft-${Date.now()}.md`)
  ElMessage.success('已导出后续任务草稿')
}

function openTaskSuggestionDialog() {
  const taskSuggestion = buildFollowUpTaskSuggestion()
  if (!taskSuggestion) {
    ElMessage.warning('当前没有可展示的后续任务建议清单')
    return
  }
  taskSuggestionContent.value = taskSuggestion
  taskSuggestionDialogVisible.value = true
}

function buildFollowUpTaskSuggestion() {
  const followUpEntries = getFollowUpEntries()
  if (!followUpEntries.length) {
    return ''
  }

  const grouped = followUpCategoryOptions
    .map((option) => ({
      ...option,
      entries: followUpEntries.filter((item) => item.category === option.value)
    }))
    .filter((group) => group.entries.length > 0)

  const lines = [
    '# RAG MVP 后续任务草稿',
    '',
    `- 导出时间：${new Date().toLocaleString()}`,
    `- 当前聚焦：${currentQuickViewDesc.value}`,
    `- 待跟进总数：${followUpEntries.length}`,
    '',
    '## 问题分类汇总',
    ''
  ]

  grouped.forEach((group) => {
    lines.push(`### ${group.label}（${group.entries.length}）`)
    lines.push(followUpCategoryDescMap[group.value])
    lines.push('')
    group.entries.forEach((item, index) => {
      lines.push(`#### ${group.label}-${index + 1}`)
      lines.push(`- 记录编号：${item.row.id}`)
      lines.push(`- 应用：${sanitizeInline(item.row.appName)}`)
      lines.push(`- 知识库：${sanitizeInline(item.row.libName)}`)
      lines.push(`- 模型：${sanitizeInline(item.detail.modelName || '未知模型')}`)
      lines.push(`- 耗时：${item.detail.costTime ?? item.row.costTime ?? '-'}ms`)
      lines.push(`- 用户问题：${sanitizeInline(item.detail.userInput)}`)
      lines.push(`- 回答摘要：${summarizeDisplayText(item.detail.assistantMessage)}`)
      lines.push(`- 失败原因：${sanitizeInline(item.detail.failReason || item.row.failReason || '无')}`)
      lines.push(`- 建议动作：围绕“${group.label}”方向进入任务池进一步处理`)
      lines.push('')
    })
  })

  lines.push('## 建议后续动作')
  lines.push('')
  lines.push('- 按分类评估是否需要新建任务，优先处理数量最多且影响体验最直接的问题。')
  lines.push('- 针对同类问题抽样核对原始调用记录，避免把单点异常误判成系统性问题。')
  lines.push('- 若同类问题连续出现，建议在任务中补充可复现样例问题与对应记录编号。')

  return lines.join('\n')
}

function getFollowUpEntries() {
  return filteredRows.value.flatMap((row: any) => {
    const detailList = row.detailList || []
    return detailList
      .map((detail: any) => {
        const key = buildReviewKey(row, detail)
        const category = getFollowUpCategory(key)
        if (getReviewStatus(key) !== 'followUp' || !category) {
          return null
        }
        return {
          key,
          row,
          detail,
          category
        }
      })
      .filter(Boolean)
  }) as Array<{ key: string; row: any; detail: any; category: FollowUpCategory }>
}

function matchFollowUpCategoryView(category: FollowUpCategory) {
  if (!followUpCategoryView.value) {
    return true
  }
  return followUpCategoryView.value === category
}

function resetFilters() {
  searchData.appName = ""
  searchData.userInputKeyword = ""
  searchData.status = ""
  searchData.startTime = null
  searchData.endTime = null
  searchData.pageNow = 1
  quickView.value = 'all'
  loadTable()
}

function openDetailDialog(title: string, content: string) {
  detailDialogTitle.value = title
  detailDialogContent.value = String(content || '')
  detailDialogVisible.value = true
}

function openAcceptanceDialog(row: any, detail: any) {
  const detailList = row.detailList || []
  const index = Math.max(detailList.findIndex((item: any) => item === detail), 0)
  acceptanceDialogContent.value = buildAcceptanceEntry(row, detail, index)
  acceptanceDialogVisible.value = true
}

async function copyAcceptanceEntry(row: any, detail: any) {
  const detailList = row.detailList || []
  const index = Math.max(detailList.findIndex((item: any) => item === detail), 0)
  await copyText(buildAcceptanceEntry(row, detail, index), '验收条目')
}

function buildAnswerSummary(message: string) {
  const normalized = String(message || '').replace(/\s+/g, ' ').trim()
  if (!normalized) {
    return '-'
  }
  return sanitizeTableCell(normalized.slice(0, 120))
}

function summarizeDisplayText(message: string) {
  const normalized = String(message || '').replace(/\s+/g, ' ').trim()
  if (!normalized) {
    return '当前没有可用回答内容'
  }
  if (normalized.length <= 120) {
    return normalized
  }
  return `${normalized.slice(0, 120)}...`
}

function buildRiskTags(row: any, detail: any) {
  const tags = []
  const detailStatus = Number(detail.status ?? row.status)
  const costTime = Number(detail.costTime ?? row.costTime ?? 0)
  const answerText = String(detail.assistantMessage || '').trim()
  const failReason = String(detail.failReason || row.failReason || '').trim()

  if (detailStatus === 0) {
    tags.push({ key: 'fail', label: '失败记录', tone: 'danger', score: 100 })
  }
  if (failReason) {
    tags.push({ key: 'reason', label: '有失败原因', tone: 'warning', score: 50 })
  }
  if (costTime >= 5000) {
    tags.push({ key: 'slow', label: '慢请求', tone: 'warning', score: 35 })
  }
  if (answerText.length >= 200) {
    tags.push({ key: 'long', label: '长回答', tone: 'info', score: 20 })
  }
  if (!answerText) {
    tags.push({ key: 'empty', label: '空回答', tone: 'danger', score: 80 })
  }

  return tags
}

function buildReviewKey(row: any, detail: any) {
  const detailList = row.detailList || []
  const index = Math.max(detailList.findIndex((item: any) => item === detail), 0)
  return `${row.id}-${index + 1}`
}

function getReviewStatus(key: string): ReviewStatus {
  return reviewStatusMap.value[key] || 'pending'
}

function cycleReviewStatus(key: string) {
  const current = getReviewStatus(key)
  const next: ReviewStatus =
    current === 'pending'
      ? 'reviewed'
      : current === 'reviewed'
        ? 'followUp'
        : 'pending'
  reviewStatusMap.value = {
    ...reviewStatusMap.value,
    [key]: next
  }
  if (next !== 'followUp' && followUpCategoryMap.value[key]) {
    const nextCategoryMap = { ...followUpCategoryMap.value }
    delete nextCategoryMap[key]
    followUpCategoryMap.value = nextCategoryMap
    saveItem(followUpCategoryStoreKey, followUpCategoryMap.value)
  }
  saveItem(reviewStatusStoreKey, reviewStatusMap.value)
  ElMessage.success(`已更新为${reviewStatusText[next]}`)
}

function nextReviewActionText(key: string) {
  const current = getReviewStatus(key)
  if (current === 'pending') {
    return '标记已复盘'
  }
  if (current === 'reviewed') {
    return '标记待跟进'
  }
  return '清除状态'
}

function loadReviewStatusMap(): Record<string, ReviewStatus> {
  const raw = getItem(reviewStatusStoreKey)
  if (!raw) {
    return {}
  }
  try {
    return JSON.parse(raw)
  } catch {
    return {}
  }
}

function getFollowUpCategory(key: string): FollowUpCategory | '' {
  return followUpCategoryMap.value[key] || ''
}

function matchFollowUpFilter(row: any, detail: any) {
  const key = buildReviewKey(row, detail)
  if (getReviewStatus(key) !== 'followUp') {
    return false
  }
  if (!followUpCategoryView.value) {
    return true
  }
  return getFollowUpCategory(key) === followUpCategoryView.value
}

function getFollowUpCategoryLabel(key: string) {
  const category = getFollowUpCategory(key)
  if (!category) {
    return ''
  }
  return followUpCategoryLabelMap[category]
}

function getFollowUpLabel(category: FollowUpCategory | '' | undefined) {
  if (!category) {
    return ''
  }
  return followUpCategoryLabelMap[category]
}

function buildAcceptanceBatchStats(batch: any) {
  const items = batch.items || []
  const initFollowUp = {
    knowledge: 0,
    chunking: 0,
    prompt: 0,
    ui: 0,
    observe: 0,
    other: 0
  }
  return items.reduce((acc: any, item: any) => {
    if (item.hitConclusion === '通过') {
      acc.hitPass += 1
    }
    if (item.groundedConclusion === '通过') {
      acc.groundedPass += 1
    }
    if (item.readableConclusion === '通过') {
      acc.readablePass += 1
    }
    if (item.gracefulFailureConclusion === '通过') {
      acc.gracefulPass += 1
    }
    const category = String(item.followUpCategory || '') as FollowUpCategory | ''
    if (category && Object.prototype.hasOwnProperty.call(acc.followUp, category)) {
      acc.followUp[category] += 1
    } else if (item.followUpAction || item.remark) {
      acc.followUp.other += 1
    }
    return acc
  }, {
    hitPass: 0,
    groundedPass: 0,
    readablePass: 0,
    gracefulPass: 0,
    followUp: initFollowUp
  })
}

function buildAcceptanceCompareMarkdown(left: any, right: any) {
  const leftStats = buildAcceptanceBatchStats(left)
  const rightStats = buildAcceptanceBatchStats(right)
  const rows = acceptanceCompareRows.value
  return [
    '# RAG 正式验收对比报告',
    '',
    `- 生成时间：${new Date().toLocaleString()}`,
    '',
    '## 对比对象',
    '',
    `- A 批次：${left.batchName}`,
    `  发布版本：${left.releaseVersion || '-'}`,
    `  实验版本：${left.experimentVersion || '-'}`,
    `  生效实验版本：${left.activeExperimentName || '-'} (${left.activeSplitStrategy || '-'})`,
    `  汇总结论：${left.summaryConclusion || '-'}`,
    `- B 批次：${right.batchName}`,
    `  发布版本：${right.releaseVersion || '-'}`,
    `  实验版本：${right.experimentVersion || '-'}`,
    `  生效实验版本：${right.activeExperimentName || '-'} (${right.activeSplitStrategy || '-'})`,
    `  汇总结论：${right.summaryConclusion || '-'}`,
    '',
    '## 结果对比',
    '',
    '| 维度 | A | B |',
    '| --- | --- | --- |',
    ...rows.map((row) => `| ${row.label} | ${row.left} | ${row.right} |`),
    '',
    '## 对比判断',
    '',
    `- A 全部通过条目：${left.passCount || 0} / ${left.itemCount || 0}`,
    `- B 全部通过条目：${right.passCount || 0} / ${right.itemCount || 0}`,
    `- A 待跟进条目：${left.followUpCount || 0}`,
    `- B 待跟进条目：${right.followUpCount || 0}`,
    '',
    '## 结论草稿',
    '',
    '- 哪一版在命中问题、可信、易懂、失败体面上更稳定：',
    `  当前可先参考：A 命中/可信/易懂/失败体面分别为 ${leftStats.hitPass}/${leftStats.groundedPass}/${leftStats.readablePass}/${leftStats.gracefulPass}，B 分别为 ${rightStats.hitPass}/${rightStats.groundedPass}/${rightStats.readablePass}/${rightStats.gracefulPass}。`,
    '- 哪一版更像“补知识”问题，哪一版更像“补切分 / 补提示词”问题：',
    `  当前可先参考：A 跟进分类 ${JSON.stringify(leftStats.followUp)}，B 跟进分类 ${JSON.stringify(rightStats.followUp)}。`,
    '- 哪一版更适合作为当前默认生效版本：',
    '- 下一轮要继续验证什么：'
  ].join('\n')
}

function buildAcceptanceRepairDraft(batch: any) {
  const items = batch.items || []
  const grouped: Record<string, any[]> = {
    knowledge: [],
    chunking: [],
    prompt: [],
    ui: [],
    observe: [],
    other: []
  }
  items.forEach((item: any) => {
    const category = String(item.followUpCategory || 'other')
    const key = Object.prototype.hasOwnProperty.call(grouped, category) ? category : 'other'
    if (item.followUpCategory || item.followUpAction || item.remark || item.hitConclusion === '不通过' || item.groundedConclusion === '不通过' || item.readableConclusion === '不通过' || item.gracefulFailureConclusion === '不通过') {
      grouped[key].push(item)
    }
  })

  const lines = [
    '# RAG 验收后修复建议草稿',
    '',
    `- 验收批次：${batch.batchName || '-'}`,
    `- 发布版本：${batch.releaseVersion || '-'}`,
    `- 实验版本：${batch.experimentVersion || '-'}`,
    `- 生效实验版本：${batch.activeExperimentName || '-'} (${batch.activeSplitStrategy || '-'})`,
    `- 生成时间：${new Date().toLocaleString()}`,
    '',
    '## 修复方向汇总',
    ''
  ]

  Object.entries(grouped).forEach(([category, categoryItems]) => {
    if (!categoryItems.length) {
      return
    }
    lines.push(`### ${getFollowUpLabel(category as FollowUpCategory) || '其他'}（${categoryItems.length}）`)
    lines.push('')
    categoryItems.forEach((item) => {
      lines.push(`- ${item.testCaseNo} ${sanitizeInline(item.userQuestion)}`)
      lines.push(`  当前结论：命中=${sanitizeInline(item.hitConclusion || '待确认')} / 可信=${sanitizeInline(item.groundedConclusion || '待确认')} / 易懂=${sanitizeInline(item.readableConclusion || '待确认')} / 失败体面=${sanitizeInline(item.gracefulFailureConclusion || '待确认')}`)
      lines.push(`  建议动作：${sanitizeInline(item.followUpAction || item.remark || '待补充')}`)
    })
    lines.push('')
  })

  lines.push('## 建议优先级')
  lines.push('')
  lines.push('- 先处理影响“可信”和“失败体面”的问题，这两项最直接影响 MVP 可用性。')
  lines.push('- 若“补切分”集中出现，优先拿当前验收批次去对比不同实验版本。')
  lines.push('- 若“补提示词”集中出现，优先检查答案结构、依据表达和失败反馈文案。')
  return lines.join('\n')
}

function updateFollowUpCategory(key: string, category: FollowUpCategory) {
  if (getReviewStatus(key) !== 'followUp') {
    ElMessage.warning('只有待跟进记录才能设置分类')
    return
  }
  followUpCategoryMap.value = {
    ...followUpCategoryMap.value,
    [key]: category
  }
  saveItem(followUpCategoryStoreKey, followUpCategoryMap.value)
  ElMessage.success(`已归类为${followUpCategoryLabelMap[category]}`)
}

function loadFollowUpCategoryMap(): Record<string, FollowUpCategory> {
  const raw = getItem(followUpCategoryStoreKey)
  if (!raw) {
    return {}
  }
  try {
    return JSON.parse(raw)
  } catch {
    return {}
  }
}

function sanitizeInline(value: any) {
  return String(value ?? '-').replace(/\n/g, ' ').trim() || '-'
}

function sanitizeTableCell(value: any) {
  return sanitizeInline(value).replace(/\|/g, '\\|')
}

function downloadMarkdown(content: string, fileName: string) {
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function formatDateTime(value: string | Date) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function formatDateForPicker(value: string | Date) {
  return formatDateTime(value)
}

type ReviewStatus = 'pending' | 'reviewed' | 'followUp'
type FollowUpCategory = 'knowledge' | 'chunking' | 'prompt' | 'ui' | 'observe' | 'other'

onMounted(() => {
  loadTable()
  loadOverview()
  loadSavedAcceptanceBatches()
  loadAvailableApps()
})
</script>
<style scoped>
.review-panel,
.table-panel,
.pagination-panel {
  padding: 18px;
}

.review-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.compact-panel {
  padding-top: 14px;
  padding-bottom: 14px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.review-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--space-text);
}

.review-desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--space-text-soft);
}

.review-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.review-badge {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(237, 245, 255, 0.9);
  color: var(--space-primary-strong);
  font-size: 12px;
  font-weight: 600;
}

.quick-filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.quick-filter-label {
  color: var(--space-text);
  font-size: 13px;
  font-weight: 700;
}

.quick-filter-hint,
.review-header-note {
  color: var(--space-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.cell-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.record-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.record-text-btn {
  min-height: 24px !important;
  padding: 0 !important;
  font-size: 12px;
}

.priority-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 14px;
}

.priority-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 249, 255, 0.95));
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.priority-card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.priority-card-title {
  color: var(--space-text);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.5;
}

.priority-card-id,
.priority-card-meta,
.priority-label,
.empty-review-hint {
  color: var(--space-text-soft);
}

.priority-card-id {
  margin-left: 6px;
  font-size: 12px;
  font-weight: 600;
}

.priority-card-meta {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
}

.priority-tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.priority-tag {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.priority-tag--danger {
  background: rgba(254, 226, 226, 0.95);
  color: #b42318;
}

.priority-tag--warning {
  background: rgba(255, 244, 229, 0.98);
  color: #b54708;
}

.priority-tag--info {
  background: rgba(237, 245, 255, 0.95);
  color: var(--space-primary-strong);
}

.priority-block {
  margin-top: 12px;
}

.priority-label {
  font-size: 12px;
  font-weight: 700;
}

.priority-text {
  margin-top: 4px;
  color: var(--space-text);
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}

.priority-text--danger {
  color: #b42318;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.category-export-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.category-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  background: rgba(255, 255, 255, 0.96);
}

.category-card-count {
  color: var(--space-primary-strong);
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
}

.category-card-title {
  margin-top: 10px;
  color: var(--space-text);
  font-size: 14px;
  font-weight: 700;
}

.category-card-desc,
.follow-up-category-placeholder,
.follow-up-category-text {
  color: var(--space-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.category-card-desc {
  margin-top: 6px;
}

.follow-up-category-text {
  color: var(--space-primary-strong);
  font-weight: 600;
}

.saved-batch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
}

.saved-batch-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  background: rgba(255, 255, 255, 0.96);
}

.saved-batch-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.saved-batch-head-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.saved-batch-check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--space-text-soft);
  font-size: 12px;
}

.saved-batch-title,
.acceptance-batch-section-title,
.acceptance-item-title {
  color: var(--space-text);
  font-weight: 700;
}

.saved-batch-title,
.acceptance-item-title {
  font-size: 15px;
}

.saved-batch-meta {
  margin-top: 4px;
  color: var(--space-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.saved-batch-count {
  color: var(--space-primary-strong);
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}

.saved-batch-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
  color: var(--space-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

.saved-batch-text {
  margin-top: 10px;
  color: var(--space-text);
  font-size: 13px;
  line-height: 1.7;
}

.acceptance-batch-layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.acceptance-batch-section {
  padding: 18px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid rgba(64, 158, 255, 0.12);
}

.acceptance-batch-section-title {
  font-size: 16px;
}

.acceptance-batch-section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.acceptance-batch-section-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.acceptance-batch-form,
.acceptance-item-form {
  margin-top: 14px;
}

.acceptance-batch-grid,
.acceptance-item-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.acceptance-item-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 14px;
  max-height: 55vh;
  overflow: auto;
  padding-right: 4px;
}

.acceptance-item-card {
  padding: 16px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(64, 158, 255, 0.1);
}

.acceptance-compare-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.acceptance-compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 14px;
}

.acceptance-compare-card {
  padding: 16px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(64, 158, 255, 0.1);
}

.acceptance-compare-table {
  margin-top: 14px;
  overflow: auto;
}

.compare-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

.compare-table th,
.compare-table td {
  padding: 12px 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  text-align: left;
  font-size: 13px;
  color: var(--space-text);
}

.compare-table th {
  background: #f8fbff;
  font-weight: 700;
}

.acceptance-item-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.acceptance-item-block {
  margin-top: 10px;
}

.priority-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.empty-review-hint {
  font-size: 13px;
  line-height: 1.7;
}

.multi-line-ellipsis {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: normal;
}

.detail-dialog-copy {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.detail-dialog-tip {
  color: var(--space-text-soft);
  font-size: 13px;
}

.detail-dialog-content {
  max-height: 55vh;
  overflow: auto;
  margin: 0;
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  color: var(--space-text);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 900px) {
  .review-header {
    flex-direction: column;
  }

  .review-badges {
    justify-content: flex-start;
  }

  .detail-dialog-copy {
    flex-direction: column;
    align-items: flex-start;
  }

  .priority-card-head {
    flex-direction: column;
  }

  .priority-tag-group {
    justify-content: flex-start;
  }

  .saved-batch-head,
  .acceptance-item-head,
  .acceptance-batch-section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .saved-batch-head-side {
    align-items: flex-start;
  }

  .acceptance-batch-grid,
  .acceptance-item-grid,
  .acceptance-compare-grid {
    grid-template-columns: 1fr;
  }
}
</style>
