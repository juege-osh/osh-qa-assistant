<template>
  <el-dialog v-model="model.detailDialogVisible" class="workspace-form-dialog record-detail-dialog" :title="model.detailDialogTitle" width="780px">
    <div class="dialog-intro">完整文本会显示在这里，方便直接复核、复制和继续带到后续文档里使用。</div>
    <div class="detail-dialog-copy workspace-dialog-toolbar">
      <div class="detail-dialog-tip workspace-dialog-note">完整文本会显示在这里，方便直接复核。</div>
      <el-button class="workspace-btn workspace-btn--ghost" @click="model.copyText(model.detailDialogContent, model.detailDialogTitle)">复制内容</el-button>
    </div>
    <section class="workspace-info-card workspace-dialog-summary-card">
      <div class="workspace-info-grid workspace-info-grid--compact">
        <div class="workspace-info-item">
          <div class="workspace-info-label">内容类型</div>
          <div class="workspace-info-value">{{ model.detailDialogTitle }}</div>
        </div>
        <div class="workspace-info-item">
          <div class="workspace-info-label">字符数</div>
          <div class="workspace-info-value">{{ model.detailDialogContent?.length || 0 }}</div>
        </div>
      </div>
    </section>
    <section class="workspace-dialog-tip-panel detail-dialog-tip-panel">
      建议先快速看字符规模和内容类型，再决定是直接复制、继续人工复核，还是转成下一步验收或任务材料。
    </section>
    <pre class="detail-dialog-content workspace-dialog-text-preview space-scrollbar">{{ model.detailDialogContent }}</pre>
  </el-dialog>

  <el-dialog v-model="model.acceptanceDialogVisible" class="workspace-form-dialog record-detail-dialog" title="验收条目草稿" width="860px">
    <div class="dialog-intro">这是一条可直接带去人工验收文档的草稿，系统只负责整理结构，不替人工下结论。</div>
    <div class="detail-dialog-copy workspace-dialog-toolbar">
      <div class="detail-dialog-tip workspace-dialog-note">这是一条可直接带去人工验收文档的草稿，结论项保持待人工填写，避免系统替你做判断。</div>
      <el-button class="workspace-btn workspace-btn--ghost" @click="model.copyText(model.acceptanceDialogContent, '验收条目草稿')">复制内容</el-button>
    </div>
    <section class="workspace-info-card workspace-dialog-summary-card">
      <div class="workspace-info-grid workspace-info-grid--compact">
        <div class="workspace-info-item">
          <div class="workspace-info-label">内容类型</div>
          <div class="workspace-info-value">验收条目草稿</div>
        </div>
        <div class="workspace-info-item">
          <div class="workspace-info-label">字符数</div>
          <div class="workspace-info-value">{{ model.acceptanceDialogContent?.length || 0 }}</div>
        </div>
      </div>
    </section>
    <section class="workspace-dialog-tip-panel detail-dialog-tip-panel">
      更适合先带着这份草稿去做人工核验，再把结论、风险和后续动作补回正式验收批次。
    </section>
    <pre class="detail-dialog-content workspace-dialog-text-preview space-scrollbar">{{ model.acceptanceDialogContent }}</pre>
  </el-dialog>

  <el-dialog v-model="model.taskSuggestionDialogVisible" class="workspace-form-dialog record-detail-dialog" title="后续任务建议清单" width="920px">
    <div class="dialog-intro">这里会整理出一份可继续流转的后续任务建议，适合直接带去复盘、排期或任务池。</div>
    <div class="detail-dialog-copy workspace-dialog-toolbar">
      <div class="detail-dialog-tip workspace-dialog-note">可直接复制到禅道或复盘文档。</div>
      <el-button class="workspace-btn workspace-btn--ghost" @click="model.copyText(model.taskSuggestionContent, '后续任务建议清单')">复制内容</el-button>
    </div>
    <section class="workspace-info-card workspace-dialog-summary-card">
      <div class="workspace-info-grid workspace-info-grid--compact">
        <div class="workspace-info-item">
          <div class="workspace-info-label">内容类型</div>
          <div class="workspace-info-value">后续任务建议清单</div>
        </div>
        <div class="workspace-info-item">
          <div class="workspace-info-label">字符数</div>
          <div class="workspace-info-value">{{ model.taskSuggestionContent?.length || 0 }}</div>
        </div>
      </div>
    </section>
    <section class="workspace-dialog-tip-panel detail-dialog-tip-panel">
      如果这份建议会直接进入执行，建议先人工确认优先级、责任归属和是否需要拆成多条更可执行的小任务。
    </section>
    <pre class="detail-dialog-content workspace-dialog-text-preview space-scrollbar">{{ model.taskSuggestionContent }}</pre>
  </el-dialog>

  <el-dialog v-model="model.repairTaskPoolDialogVisible" class="workspace-form-dialog record-detail-dialog" :title="model.repairTaskPoolDialogTitle" width="960px">
    <div class="dialog-intro">任务池草稿会把当前问题、修复方向和执行建议整理成更适合排期跟进的格式。</div>
    <div class="detail-dialog-copy workspace-dialog-toolbar">
      <div class="detail-dialog-tip workspace-dialog-note">可直接复制到任务池或排期里。</div>
      <div class="workspace-dialog-footer workspace-dialog-actions-inline detail-dialog-actions">
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.copyText(model.repairTaskPoolContent, '任务池草稿')">复制内容</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportRepairTaskPoolContent">导出 Markdown</el-button>
      </div>
    </div>
    <section class="workspace-info-card workspace-dialog-summary-card">
      <div class="workspace-info-grid workspace-info-grid--compact">
        <div class="workspace-info-item">
          <div class="workspace-info-label">内容类型</div>
          <div class="workspace-info-value">{{ model.repairTaskPoolDialogTitle }}</div>
        </div>
        <div class="workspace-info-item">
          <div class="workspace-info-label">字符数</div>
          <div class="workspace-info-value">{{ model.repairTaskPoolContent?.length || 0 }}</div>
        </div>
      </div>
    </section>
    <section class="workspace-dialog-tip-panel detail-dialog-tip-panel">
      更稳妥的做法通常是先人工筛一遍优先级，再决定哪些进入当前迭代、哪些保留在长期任务池里持续跟踪。
    </section>
    <pre class="detail-dialog-content workspace-dialog-text-preview space-scrollbar">{{ model.repairTaskPoolContent }}</pre>
  </el-dialog>

  <el-dialog v-model="model.acceptanceBatchDialogVisible" class="workspace-form-dialog record-detail-dialog acceptance-batch-dialog" :title="model.acceptanceBatchDialogTitle" width="1120px">
    <div class="dialog-intro">把一批真实记录整理成正式验收批次，补齐版本、结论和后续动作后，就能形成一份可交付的验收材料。</div>
    <div class="acceptance-batch-layout workspace-review-layout">
      <section class="acceptance-batch-section workspace-review-panel">
        <div class="acceptance-batch-section-title workspace-review-panel__title">批次信息</div>
        <section class="workspace-dialog-tip-panel workspace-review-tip-panel">
          建议先把批次名称、版本和知识范围写清楚，再逐条补结论。这样后续回看时更容易定位这轮验收到底验证了什么。
        </section>
        <section class="workspace-info-card workspace-dialog-summary-card">
          <div class="workspace-info-grid">
            <div class="workspace-info-item">
              <div class="workspace-info-label">验收条目</div>
              <div class="workspace-info-value">{{ model.acceptanceBatchForm.items.length }}</div>
            </div>
            <div class="workspace-info-item">
              <div class="workspace-info-label">应用名称</div>
              <div class="workspace-info-value">{{ model.acceptanceBatchForm.appName || '--' }}</div>
            </div>
            <div class="workspace-info-item">
              <div class="workspace-info-label">场景类型</div>
              <div class="workspace-info-value">{{ model.acceptanceBatchForm.sceneType || '--' }}</div>
            </div>
            <div class="workspace-info-item">
              <div class="workspace-info-label">验收人</div>
              <div class="workspace-info-value">{{ model.acceptanceBatchForm.testerName || '--' }}</div>
            </div>
          </div>
        </section>
        <el-form :model="model.acceptanceBatchForm" label-position="top" class="acceptance-batch-form workspace-review-form">
          <div class="acceptance-batch-grid workspace-review-grid">
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

      <section class="acceptance-batch-section workspace-review-panel">
        <div class="acceptance-batch-section-head workspace-review-head">
          <div class="acceptance-batch-section-title workspace-review-panel__title">验收条目</div>
          <div class="acceptance-batch-section-actions workspace-review-actions">
            <div class="detail-dialog-tip workspace-review-note">共 {{ model.acceptanceBatchForm.items.length }} 条，可逐条填写四项结论和后续动作。</div>
            <el-button class="workspace-btn workspace-btn--ghost" @click="model.appendCurrentRecordItemsToBatch">追加当前筛选记录</el-button>
          </div>
        </div>
        <div class="acceptance-item-list workspace-review-list space-scrollbar">
          <article v-for="item in model.acceptanceBatchForm.items" :key="item.localKey" class="acceptance-item-card workspace-review-card">
            <div class="acceptance-item-head workspace-review-head">
              <div>
                <div class="acceptance-item-title workspace-review-card__title">{{ item.testCaseNo }}</div>
                <div class="saved-batch-meta workspace-review-note">
                  {{ item.appName || '-' }} · {{ item.libName || '-' }} · {{ item.modelName || '未知模型' }} · {{ item.costTime ?? '-' }}ms
                </div>
              </div>
              <div class="workspace-chip-row priority-tag-group">
                <span class="workspace-chip workspace-chip--info">{{ item.invokeStatus || '-' }}</span>
                <span v-if="item.followUpCategory" class="workspace-chip workspace-chip--warning">{{ model.getFollowUpLabel(item.followUpCategory) }}</span>
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
            <el-form :model="item" label-position="top" class="acceptance-item-form workspace-review-form">
              <el-form-item label="期望知识点">
                <el-input v-model="item.expectedKnowledge" type="textarea" :rows="2" placeholder="人工补充期望知识点或期望行为" />
              </el-form-item>
              <div class="acceptance-item-grid workspace-review-grid">
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
      <div class="workspace-dialog-footer dialog-footer-wrap">
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportAcceptanceBatchDialog">导出 Markdown</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.acceptanceBatchDialogVisible = false">关闭</el-button>
        <el-button class="workspace-btn workspace-btn--primary" @click="model.saveAcceptanceBatch">保存批次</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="model.acceptanceCompareDialogVisible" class="workspace-form-dialog record-detail-dialog acceptance-batch-dialog" title="正式验收对比" width="1100px">
    <div class="dialog-intro">把两轮正式验收并排对比，先看整体规模和通过情况，再看四项体验与修复分类的差异，最后输出对比结论。</div>
    <div v-if="model.acceptanceCompareState.left && model.acceptanceCompareState.right" class="acceptance-compare-layout workspace-review-layout">
      <section class="acceptance-batch-section workspace-review-panel">
        <div class="acceptance-batch-section-title workspace-review-panel__title">对比概览</div>
        <div class="acceptance-compare-grid workspace-review-grid">
          <article class="acceptance-compare-card workspace-review-card">
            <div class="saved-batch-title workspace-review-card__title">{{ model.acceptanceCompareState.left.batchName }}</div>
            <div class="saved-batch-meta workspace-review-note">发布版本：{{ model.acceptanceCompareState.left.releaseVersion || '-' }} · 实验版本：{{ model.acceptanceCompareState.left.experimentVersion || '-' }}</div>
            <div class="saved-batch-summary workspace-review-summary">
              <span>总条目：{{ model.acceptanceCompareState.left.itemCount }}</span>
              <span>全部通过：{{ model.acceptanceCompareState.left.passCount }}</span>
              <span>待跟进：{{ model.acceptanceCompareState.left.followUpCount }}</span>
            </div>
          </article>
          <article class="acceptance-compare-card workspace-review-card">
            <div class="saved-batch-title workspace-review-card__title">{{ model.acceptanceCompareState.right.batchName }}</div>
            <div class="saved-batch-meta workspace-review-note">发布版本：{{ model.acceptanceCompareState.right.releaseVersion || '-' }} · 实验版本：{{ model.acceptanceCompareState.right.experimentVersion || '-' }}</div>
            <div class="saved-batch-summary workspace-review-summary">
              <span>总条目：{{ model.acceptanceCompareState.right.itemCount }}</span>
              <span>全部通过：{{ model.acceptanceCompareState.right.passCount }}</span>
              <span>待跟进：{{ model.acceptanceCompareState.right.followUpCount }}</span>
            </div>
          </article>
        </div>
      </section>

      <section class="acceptance-batch-section workspace-review-panel">
        <div class="acceptance-batch-section-title workspace-review-panel__title">四项体验与修复分类对比</div>
        <div class="acceptance-compare-table workspace-review-table workspace-compare-table">
          <table>
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

      <section class="acceptance-batch-section workspace-review-panel">
        <div class="acceptance-batch-section-title workspace-review-panel__title">对比结论草稿</div>
        <pre class="detail-dialog-content workspace-dialog-text-preview space-scrollbar">{{ model.acceptanceCompareDraftContent }}</pre>
      </section>
    </div>
    <template #footer>
      <div class="workspace-dialog-footer dialog-footer-wrap">
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.copyText(model.acceptanceCompareDraftContent, '正式验收对比草稿')">复制内容</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.exportAcceptanceCompareDraft">导出 Markdown</el-button>
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.acceptanceCompareDialogVisible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="model.defaultBatchRunnerVisible" class="workspace-form-dialog record-detail-dialog" title="执行默认问题集跑测" width="760px">
    <div class="dialog-intro">默认问题集适合快速跑一轮标准验收，先验证命中、体面失败和回答稳定性，再决定是否进入更重的真实问题集跑测。</div>
    <section class="acceptance-batch-section acceptance-batch-section--single workspace-review-panel workspace-review-panel--single">
      <div class="acceptance-batch-section-head workspace-review-head">
        <div>
          <div class="acceptance-batch-section-title workspace-review-panel__title">批次设置</div>
          <div class="detail-dialog-tip workspace-review-note">默认问题集适合快速做一轮标准验收，先验证命中、体面失败和回答稳定性。</div>
        </div>
      </div>
      <section class="workspace-dialog-tip-panel workspace-review-tip-panel">
        如果当前应用还处在早期验证阶段，建议先用默认问题集把链路跑通，再决定要不要投入更真实、更复杂的问题样本。
      </section>
      <section class="workspace-info-card workspace-dialog-summary-card">
        <div class="workspace-info-grid workspace-info-grid--compact">
          <div class="workspace-info-item">
            <div class="workspace-info-label">问题来源</div>
            <div class="workspace-info-value">默认问题集</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">可选应用</div>
            <div class="workspace-info-value">{{ model.availableApps.length }}</div>
          </div>
        </div>
      </section>
      <el-form :model="model.defaultBatchRunnerForm" label-position="top" class="acceptance-batch-form workspace-review-form">
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
      <div class="dialog-tip-panel workspace-dialog-tip-panel">
        这一步会直接对当前应用执行默认问题集，生成真实调用记录、真实回答，并自动落成正式验收批次。
      </div>
    </section>
    <template #footer>
      <div class="workspace-dialog-footer dialog-footer-wrap">
        <el-button class="workspace-btn workspace-btn--ghost" @click="model.defaultBatchRunnerVisible = false">取消</el-button>
        <el-button class="workspace-btn workspace-btn--primary" :loading="model.defaultBatchRunning" @click="model.runDefaultAcceptanceBatch">
          开始真实跑测
        </el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="model.realBatchRunnerVisible" class="workspace-form-dialog record-detail-dialog acceptance-batch-dialog" title="执行真实问题集跑测" width="1080px">
    <div class="dialog-intro">真实问题集会直接生成真实调用记录和正式验收批次，更适合在准备交付、复盘或灰度前做一轮贴近业务的问题验证。</div>
    <div class="acceptance-batch-layout workspace-review-layout">
      <section class="acceptance-batch-section workspace-review-panel">
        <div class="acceptance-batch-section-title workspace-review-panel__title">批次信息</div>
        <section class="workspace-dialog-tip-panel workspace-review-tip-panel">
          这一轮更适合填入真实业务问题和明确期望知识点。问题越贴近实际使用场景，后续验收结论就越有参考价值。
        </section>
        <section class="workspace-info-card workspace-dialog-summary-card">
          <div class="workspace-info-grid">
            <div class="workspace-info-item">
              <div class="workspace-info-label">问题来源</div>
              <div class="workspace-info-value">真实问题集</div>
            </div>
            <div class="workspace-info-item">
              <div class="workspace-info-label">可选应用</div>
              <div class="workspace-info-value">{{ model.availableApps.length }}</div>
            </div>
            <div class="workspace-info-item">
              <div class="workspace-info-label">当前问题数</div>
              <div class="workspace-info-value">{{ model.realBatchRunnerForm.questions.length }}</div>
            </div>
            <div class="workspace-info-item">
              <div class="workspace-info-label">执行目标</div>
              <div class="workspace-info-value">生成正式验收批次</div>
            </div>
          </div>
        </section>
        <el-form :model="model.realBatchRunnerForm" label-position="top" class="acceptance-batch-form workspace-review-form">
          <div class="acceptance-batch-grid workspace-review-grid">
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

      <section class="acceptance-batch-section workspace-review-panel">
        <div class="acceptance-batch-section-head workspace-review-head">
          <div class="acceptance-batch-section-title workspace-review-panel__title">真实问题集</div>
          <div class="acceptance-batch-section-actions workspace-review-actions">
            <div class="detail-dialog-tip workspace-review-note">把真实业务问题逐条补进去，系统会直接批量执行并生成正式验收批次。</div>
            <el-button class="workspace-btn workspace-btn--ghost" @click="model.appendRealQuestionRow">新增一条</el-button>
            <el-button class="workspace-btn workspace-btn--ghost" @click="model.loadRealQuestionsFromCurrentRows">从当前调用记录带入</el-button>
          </div>
        </div>
        <div class="acceptance-item-list workspace-review-list space-scrollbar">
          <article v-for="(item, index) in model.realBatchRunnerForm.questions" :key="item.localKey" class="acceptance-item-card workspace-review-card">
            <div class="acceptance-item-head workspace-review-head">
              <div>
                <div class="acceptance-item-title workspace-review-card__title">{{ item.testCaseNo || `RQ-${String(index + 1).padStart(2, '0')}` }}</div>
                <div class="saved-batch-meta workspace-review-note">{{ item.questionType || '真实业务问题' }}</div>
              </div>
              <div class="priority-tag-group">
                <el-button class="workspace-btn workspace-btn--ghost" @click="model.removeRealQuestionRow(index)">删除</el-button>
              </div>
            </div>
            <el-form :model="item" label-position="top" class="acceptance-item-form workspace-review-form">
              <div class="acceptance-item-grid workspace-review-grid">
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
    <div class="workspace-dialog-tip-panel workspace-review-tip-panel workspace-review-tip-panel--footer">
      这一步会直接执行你填入的真实问题集，生成真实回答、真实调用记录和正式验收批次，适合做一轮真正可交付的 MVP 验收。
    </div>
    <template #footer>
      <div class="workspace-dialog-footer dialog-footer-wrap">
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
.priority-tag-group,
.dialog-footer-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.detail-dialog-copy {
  align-items: flex-start;
}

.detail-dialog-tip-panel,
.workspace-review-tip-panel {
  margin-bottom: 14px;
}

.workspace-review-tip-panel--footer {
  margin-top: 16px;
  margin-bottom: 0;
}

.dialog-footer-wrap {
  width: 100%;
}

.acceptance-batch-grid,
.acceptance-item-grid,
.acceptance-compare-grid {
  display: grid;
}

.acceptance-batch-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
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

@media (max-width: 900px) {
  .detail-dialog-copy {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
