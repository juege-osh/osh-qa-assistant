<template>
  <el-dialog v-model="model.detailDialogVisible" class="record-detail-dialog" :title="model.detailDialogTitle" width="780px">
    <div class="detail-dialog-copy">
      <div class="detail-dialog-tip">完整文本会显示在这里，方便直接复核。</div>
      <el-button class="workspace-btn workspace-btn--ghost" @click="model.copyText(model.detailDialogContent, model.detailDialogTitle)">复制内容</el-button>
    </div>
    <pre class="detail-dialog-content">{{ model.detailDialogContent }}</pre>
  </el-dialog>

  <el-dialog v-model="model.acceptanceDialogVisible" class="record-detail-dialog" title="验收条目草稿" width="860px">
    <div class="detail-dialog-copy">
      <div class="detail-dialog-tip">这是一条可直接带去人工验收文档的草稿，结论项保持待人工填写，避免系统替你做判断。</div>
      <el-button class="workspace-btn workspace-btn--ghost" @click="model.copyText(model.acceptanceDialogContent, '验收条目草稿')">复制内容</el-button>
    </div>
    <pre class="detail-dialog-content">{{ model.acceptanceDialogContent }}</pre>
  </el-dialog>

  <el-dialog v-model="model.taskSuggestionDialogVisible" class="record-detail-dialog" title="后续任务建议清单" width="920px">
    <div class="detail-dialog-copy">
      <div class="detail-dialog-tip">可直接复制到禅道或复盘文档。</div>
      <el-button class="workspace-btn workspace-btn--ghost" @click="model.copyText(model.taskSuggestionContent, '后续任务建议清单')">复制内容</el-button>
    </div>
    <pre class="detail-dialog-content">{{ model.taskSuggestionContent }}</pre>
  </el-dialog>

  <el-dialog v-model="model.repairTaskPoolDialogVisible" class="record-detail-dialog" :title="model.repairTaskPoolDialogTitle" width="960px">
    <div class="detail-dialog-copy">
      <div class="detail-dialog-tip">可直接复制到任务池或排期里。</div>
      <div class="dialog-footer">
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.copyText(model.repairTaskPoolContent, '任务池草稿')">复制内容</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportRepairTaskPoolContent">导出 Markdown</el-button>
      </div>
    </div>
    <pre class="detail-dialog-content">{{ model.repairTaskPoolContent }}</pre>
  </el-dialog>

  <el-dialog v-model="model.acceptanceBatchDialogVisible" class="record-detail-dialog acceptance-batch-dialog" :title="model.acceptanceBatchDialogTitle" width="1120px">
    <div class="acceptance-batch-layout">
      <section class="acceptance-batch-section">
        <div class="acceptance-batch-section-title">批次信息</div>
        <el-form :model="model.acceptanceBatchForm" label-width="100px" class="acceptance-batch-form">
          <div class="acceptance-batch-grid">
            <el-form-item label="批次名称">
              <el-input v-model="model.acceptanceBatchForm.batchName" placeholder="例如：2026-06-29 内部知识问答首轮验收" />
            </el-form-item>
            <el-form-item label="应用名称">
              <el-input v-model="model.acceptanceBatchForm.appName" placeholder="应用名称" />
            </el-form-item>
            <el-form-item label="场景类型">
              <el-input v-model="model.acceptanceBatchForm.sceneType" placeholder="内部知识问答 / 任务指导" />
            </el-form-item>
            <el-form-item label="验收日期">
              <el-date-picker
                v-model="model.acceptanceBatchForm.testDate"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择验收日期"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="知识库范围">
              <el-input v-model="model.acceptanceBatchForm.knowledgeScope" placeholder="本轮验收覆盖的知识库范围" />
            </el-form-item>
            <el-form-item label="发布版本">
              <el-input v-model="model.acceptanceBatchForm.releaseVersion" placeholder="例如：v0.1.3" />
            </el-form-item>
            <el-form-item label="实验版本">
              <el-input v-model="model.acceptanceBatchForm.experimentVersion" placeholder="例如：semantic-20260629" />
            </el-form-item>
            <el-form-item label="验收人">
              <el-input v-model="model.acceptanceBatchForm.testerName" placeholder="验收人" />
            </el-form-item>
          </div>
          <el-form-item label="版本说明">
            <el-input v-model="model.acceptanceBatchForm.versionRemark" type="textarea" :rows="2" placeholder="补充本轮版本说明" />
          </el-form-item>
          <el-form-item label="汇总结论">
            <el-input v-model="model.acceptanceBatchForm.summaryConclusion" type="textarea" :rows="3" placeholder="填写本轮验收的整体判断" />
          </el-form-item>
          <el-form-item label="后续动作">
            <el-input v-model="model.acceptanceBatchForm.nextAction" type="textarea" :rows="3" placeholder="例如：补知识、补提示词、补失败反馈" />
          </el-form-item>
        </el-form>
      </section>

      <section class="acceptance-batch-section">
        <div class="acceptance-batch-section-head">
          <div class="acceptance-batch-section-title">验收条目</div>
          <div class="acceptance-batch-section-actions">
            <div class="detail-dialog-tip">共 {{ model.acceptanceBatchForm.items.length }} 条，可逐条填写四项结论和后续动作。</div>
            <el-button class="workspace-btn workspace-btn--ghost" @click="model.appendCurrentRecordItemsToBatch">追加当前筛选记录</el-button>
          </div>
        </div>
        <div class="acceptance-item-list">
          <article v-for="item in model.acceptanceBatchForm.items" :key="item.localKey" class="acceptance-item-card">
            <div class="acceptance-item-head">
              <div>
                <div class="acceptance-item-title">{{ item.testCaseNo }}</div>
                <div class="saved-batch-meta">
                  {{ item.appName || '-' }} · {{ item.libName || '-' }} · {{ item.modelName || '未知模型' }} · {{ item.costTime ?? '-' }}ms
                </div>
              </div>
              <div class="priority-tag-group">
                <span class="priority-tag priority-tag--info">{{ item.invokeStatus || '-' }}</span>
                <span v-if="item.followUpCategory" class="priority-tag priority-tag--warning">{{ model.getFollowUpLabel(item.followUpCategory) }}</span>
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
            <div v-if="item.failReason" class="acceptance-item-block">
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
                    <el-option v-for="option in model.conclusionOptions" :key="option" :label="option" :value="option" />
                  </el-select>
                </el-form-item>
                <el-form-item label="可信">
                  <el-select v-model="item.groundedConclusion" placeholder="选择结论">
                    <el-option v-for="option in model.conclusionOptions" :key="option" :label="option" :value="option" />
                  </el-select>
                </el-form-item>
                <el-form-item label="易懂">
                  <el-select v-model="item.readableConclusion" placeholder="选择结论">
                    <el-option v-for="option in model.conclusionOptions" :key="option" :label="option" :value="option" />
                  </el-select>
                </el-form-item>
                <el-form-item label="失败体面">
                  <el-select v-model="item.gracefulFailureConclusion" placeholder="选择结论">
                    <el-option v-for="option in model.conclusionOptions" :key="option" :label="option" :value="option" />
                  </el-select>
                </el-form-item>
                <el-form-item label="HitRate@5">
                  <el-select v-model="item.hitRateConclusion" placeholder="选择结论">
                    <el-option v-for="option in model.conclusionOptions" :key="option" :label="option" :value="option" />
                  </el-select>
                </el-form-item>
                <el-form-item label="Completeness">
                  <el-select v-model="item.completenessConclusion" placeholder="选择结论">
                    <el-option v-for="option in model.conclusionOptions" :key="option" :label="option" :value="option" />
                  </el-select>
                </el-form-item>
                <el-form-item label="跟进分类">
                  <el-select v-model="item.followUpCategory" clearable placeholder="选择分类">
                    <el-option v-for="option in model.followUpCategoryOptions" :key="option.value" :label="option.label" :value="option.value" />
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
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportAcceptanceBatchDialog">导出 Markdown</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.acceptanceBatchDialogVisible = false">关闭</el-button>
        <el-button class="workspace-btn workspace-btn--primary" @click="model.saveAcceptanceBatch">保存批次</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="model.acceptanceCompareDialogVisible" class="record-detail-dialog acceptance-batch-dialog" title="正式验收对比" width="1100px">
    <div v-if="model.acceptanceCompareState.left && model.acceptanceCompareState.right" class="acceptance-compare-layout">
      <section class="acceptance-batch-section">
        <div class="acceptance-batch-section-title">对比概览</div>
        <div class="acceptance-compare-grid">
          <article class="acceptance-compare-card">
            <div class="saved-batch-title">{{ model.acceptanceCompareState.left.batchName }}</div>
            <div class="saved-batch-meta">发布版本：{{ model.acceptanceCompareState.left.releaseVersion || '-' }} · 实验版本：{{ model.acceptanceCompareState.left.experimentVersion || '-' }}</div>
            <div class="saved-batch-summary">
              <span>总条目：{{ model.acceptanceCompareState.left.itemCount }}</span>
              <span>全部通过：{{ model.acceptanceCompareState.left.passCount }}</span>
              <span>待跟进：{{ model.acceptanceCompareState.left.followUpCount }}</span>
            </div>
          </article>
          <article class="acceptance-compare-card">
            <div class="saved-batch-title">{{ model.acceptanceCompareState.right.batchName }}</div>
            <div class="saved-batch-meta">发布版本：{{ model.acceptanceCompareState.right.releaseVersion || '-' }} · 实验版本：{{ model.acceptanceCompareState.right.experimentVersion || '-' }}</div>
            <div class="saved-batch-summary">
              <span>总条目：{{ model.acceptanceCompareState.right.itemCount }}</span>
              <span>全部通过：{{ model.acceptanceCompareState.right.passCount }}</span>
              <span>待跟进：{{ model.acceptanceCompareState.right.followUpCount }}</span>
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
                <th>{{ model.acceptanceCompareState.left.batchName }}</th>
                <th>{{ model.acceptanceCompareState.right.batchName }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in model.acceptanceCompareRows" :key="row.label">
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
        <pre class="detail-dialog-content">{{ model.acceptanceCompareDraftContent }}</pre>
      </section>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.copyText(model.acceptanceCompareDraftContent, '正式验收对比草稿')">复制内容</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportAcceptanceCompareDraft">导出 Markdown</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.acceptanceCompareDialogVisible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="model.defaultBatchRunnerVisible" class="record-detail-dialog" title="执行默认问题集跑测" width="760px">
    <el-form :model="model.defaultBatchRunnerForm" label-width="110px" class="acceptance-batch-form">
      <el-form-item label="选择应用">
        <el-select v-model="model.defaultBatchRunnerForm.appId" placeholder="选择要跑测的应用" style="width: 100%">
          <el-option v-for="app in model.availableApps" :key="app.id" :label="app.appName" :value="app.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="批次名称">
        <el-input v-model="model.defaultBatchRunnerForm.batchName" placeholder="例如：2026-06-30 默认问题集首轮正式验收" />
      </el-form-item>
      <el-form-item label="场景类型">
        <el-input v-model="model.defaultBatchRunnerForm.sceneType" placeholder="内部知识问答 / 任务指导" />
      </el-form-item>
      <el-form-item label="验收人">
        <el-input v-model="model.defaultBatchRunnerForm.testerName" placeholder="填写本轮验收人" />
      </el-form-item>
      <el-form-item label="汇总结论">
        <el-input v-model="model.defaultBatchRunnerForm.summaryConclusion" type="textarea" :rows="3" placeholder="可以先留空，跑完后再补齐结论。" />
      </el-form-item>
      <el-form-item label="后续动作">
        <el-input v-model="model.defaultBatchRunnerForm.nextAction" type="textarea" :rows="3" placeholder="例如：跑完后重点检查失败体面与补切分问题。" />
      </el-form-item>
    </el-form>
    <div class="detail-dialog-tip">
      这一步会直接对当前应用执行默认问题集，生成真实调用记录、真实回答，并自动落成正式验收批次。
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.defaultBatchRunnerVisible = false">取消</el-button>
        <el-button class="workspace-btn workspace-btn--primary" :loading="model.defaultBatchRunning" @click="model.runDefaultAcceptanceBatch">
          开始真实跑测
        </el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="model.realBatchRunnerVisible" class="record-detail-dialog acceptance-batch-dialog" title="执行真实问题集跑测" width="1080px">
    <div class="acceptance-batch-layout">
      <section class="acceptance-batch-section">
        <div class="acceptance-batch-section-title">批次信息</div>
        <el-form :model="model.realBatchRunnerForm" label-width="110px" class="acceptance-batch-form">
          <div class="acceptance-batch-grid">
            <el-form-item label="选择应用">
              <el-select v-model="model.realBatchRunnerForm.appId" placeholder="选择要跑测的应用" style="width: 100%">
                <el-option v-for="app in model.availableApps" :key="app.id" :label="app.appName" :value="app.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="批次名称">
              <el-input v-model="model.realBatchRunnerForm.batchName" placeholder="例如：2026-06-30 真实问题集首轮正式验收" />
            </el-form-item>
            <el-form-item label="场景类型">
              <el-input v-model="model.realBatchRunnerForm.sceneType" placeholder="内部知识问答 / 任务指导 / 真实业务问题" />
            </el-form-item>
            <el-form-item label="验收人">
              <el-input v-model="model.realBatchRunnerForm.testerName" placeholder="填写本轮验收人" />
            </el-form-item>
          </div>
          <el-form-item label="汇总结论">
            <el-input v-model="model.realBatchRunnerForm.summaryConclusion" type="textarea" :rows="3" placeholder="可以先留空，跑完后自动汇总。" />
          </el-form-item>
          <el-form-item label="后续动作">
            <el-input v-model="model.realBatchRunnerForm.nextAction" type="textarea" :rows="3" placeholder="例如：跑完后重点检查补知识、补提示词或补切分问题。" />
          </el-form-item>
        </el-form>
      </section>

      <section class="acceptance-batch-section">
        <div class="acceptance-batch-section-head">
          <div class="acceptance-batch-section-title">真实问题集</div>
          <div class="acceptance-batch-section-actions">
            <div class="detail-dialog-tip">把真实业务问题逐条补进去，系统会直接批量执行并生成正式验收批次。</div>
            <el-button class="workspace-btn workspace-btn--ghost" @click="model.appendRealQuestionRow">新增一条</el-button>
            <el-button class="workspace-btn workspace-btn--ghost" @click="model.loadRealQuestionsFromCurrentRows">从当前调用记录带入</el-button>
          </div>
        </div>
        <div class="acceptance-item-list">
          <article v-for="(item, index) in model.realBatchRunnerForm.questions" :key="item.localKey" class="acceptance-item-card">
            <div class="acceptance-item-head">
              <div>
                <div class="acceptance-item-title">{{ item.testCaseNo || `RQ-${String(index + 1).padStart(2, '0')}` }}</div>
                <div class="saved-batch-meta">{{ item.questionType || '真实业务问题' }}</div>
              </div>
              <div class="priority-tag-group">
                <el-button class="workspace-btn workspace-btn--ghost" @click="model.removeRealQuestionRow(index)">删除</el-button>
              </div>
            </div>
            <el-form :model="item" label-width="96px" class="acceptance-item-form">
              <div class="acceptance-item-grid">
                <el-form-item label="测试编号">
                  <el-input v-model="item.testCaseNo" placeholder="例如：RQ-01" />
                </el-form-item>
                <el-form-item label="问题类型">
                  <el-input v-model="item.questionType" placeholder="例如：标准知识问答 / 任务指导 / 失败场景" />
                </el-form-item>
              </div>
              <el-form-item label="用户问题">
                <el-input v-model="item.userQuestion" type="textarea" :rows="3" placeholder="填写真实用户会问的问题" />
              </el-form-item>
              <el-form-item label="期望知识点">
                <el-input v-model="item.expectedKnowledge" type="textarea" :rows="2" placeholder="填写期望系统覆盖的知识点、回答结构或行为" />
              </el-form-item>
            </el-form>
          </article>
        </div>
      </section>
    </div>
    <div class="detail-dialog-tip">
      这一步会直接执行你填入的真实问题集，生成真实回答、真实调用记录和正式验收批次，适合做一轮真正可交付的 MVP 验收。
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.realBatchRunnerVisible = false">取消</el-button>
        <el-button class="workspace-btn workspace-btn--primary" :loading="model.realBatchRunning" @click="model.runRealAcceptanceBatch">
          开始真实跑测
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { useInvokeRecordFeatureModel } from '../composables/useInvokeRecordFeature'

const model = useInvokeRecordFeatureModel()
</script>

<style scoped>
.detail-dialog-copy {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.detail-dialog-tip,
.saved-batch-meta {
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

.acceptance-batch-layout,
.acceptance-compare-layout,
.acceptance-item-list {
  display: flex;
  flex-direction: column;
}

.acceptance-batch-layout,
.acceptance-compare-layout {
  gap: 16px;
}

.acceptance-item-list {
  gap: 14px;
  margin-top: 14px;
  max-height: 55vh;
  overflow: auto;
  padding-right: 4px;
}

.acceptance-batch-section,
.acceptance-compare-card,
.acceptance-item-card {
  border-radius: 18px;
}

.acceptance-batch-section {
  padding: 18px;
  background: #f8fbff;
  border: 1px solid rgba(64, 158, 255, 0.12);
}

.acceptance-compare-card,
.acceptance-item-card {
  padding: 16px;
  background: #fff;
  border: 1px solid rgba(64, 158, 255, 0.1);
}

.acceptance-batch-section-title,
.acceptance-item-title,
.saved-batch-title {
  color: var(--space-text);
  font-weight: 700;
}

.acceptance-batch-section-title {
  font-size: 16px;
}

.acceptance-item-title,
.saved-batch-title {
  font-size: 15px;
}

.acceptance-batch-section-head,
.acceptance-item-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.acceptance-batch-section-actions,
.acceptance-batch-grid,
.acceptance-item-grid,
.acceptance-compare-grid,
.priority-tag-group,
.saved-batch-summary,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.acceptance-batch-section-actions {
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.acceptance-batch-form,
.acceptance-item-form,
.acceptance-compare-table {
  margin-top: 14px;
}

.acceptance-batch-grid,
.acceptance-item-grid,
.acceptance-compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
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

.priority-tag {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.priority-tag--warning {
  background: rgba(255, 244, 229, 0.98);
  color: #b54708;
}

.priority-tag--info {
  background: rgba(237, 245, 255, 0.95);
  color: var(--space-primary-strong);
}

.priority-label,
.priority-text {
  color: var(--space-text);
}

.priority-label {
  font-size: 12px;
  font-weight: 700;
}

.priority-text {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}

.priority-text--danger {
  color: #b42318;
}

.saved-batch-summary {
  color: var(--space-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .detail-dialog-copy,
  .acceptance-batch-section-head,
  .acceptance-item-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .acceptance-batch-grid,
  .acceptance-item-grid,
  .acceptance-compare-grid {
    grid-template-columns: 1fr;
  }
}
</style>
