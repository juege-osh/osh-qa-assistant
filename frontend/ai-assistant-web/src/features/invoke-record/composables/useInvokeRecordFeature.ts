import { computed, inject, provide, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useTable } from '@/hooks/useTable'
import { pageAppApi } from '@/api/workspace/appApi'
import { getItem, saveItem } from '@/util/storageUtil'
import { writeClipboardText } from '@/util/clipboard'
import {
  listRagAcceptanceBatchApi,
  pageInvokeRecordApi,
  queryInvokeRecordOverviewApi,
  queryRagAcceptanceBatchDetailApi,
  runDefaultRagAcceptanceBatchApi,
  runRagAcceptanceBatchApi,
  saveRagAcceptanceBatchApi
} from '../api/invokeRecordApi'
import { useInvokeRecordTabStore } from '../stores/useInvokeRecordTabStore'
import type {
  FollowUpCategory,
  InvokeRecordOverview,
  InvokeRecordSearchForm,
  InvokeRecordTabKey,
  RealQuestionRow,
  ReviewStatus
} from '../types'

type AnyRecord = Record<string, any>

const invokeRecordFeatureKey = Symbol('invoke-record-feature')

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

export function createInvokeRecordFeatureModel() {
  const tabStore = useInvokeRecordTabStore()
  const searchFormData = reactive<InvokeRecordSearchForm>({
    appName: '',
    userInputKeyword: '',
    status: '',
    startTime: null,
    endTime: null
  })

  const {
    searchData,
    tableData,
    loadTable,
    handlePageSizeChange,
    handlePageNowChange
  } = useTable({ searchFormData, loadTableApi: pageInvokeRecordApi })

  const activeTab = computed<InvokeRecordTabKey>({
    get: () => tabStore.activeTab,
    set: (value) => tabStore.setActiveTab(value)
  })
  const quickView = ref<'all' | 'fail' | 'slow' | 'long' | 'followUp' | 'reviewed'>('all')
  const detailDialogVisible = ref(false)
  const detailDialogTitle = ref('')
  const detailDialogContent = ref('')
  const acceptanceDialogVisible = ref(false)
  const acceptanceDialogContent = ref('')
  const taskSuggestionDialogVisible = ref(false)
  const taskSuggestionContent = ref('')
  const repairTaskPoolDialogVisible = ref(false)
  const repairTaskPoolDialogTitle = ref('验收任务池草稿')
  const repairTaskPoolContent = ref('')
  const repairTaskPoolFilename = ref('')
  const acceptanceBatchDialogVisible = ref(false)
  const acceptanceBatchDialogTitle = ref('保存验收批次')
  const acceptanceCompareDialogVisible = ref(false)
  const defaultBatchRunnerVisible = ref(false)
  const defaultBatchRunning = ref(false)
  const realBatchRunnerVisible = ref(false)
  const realBatchRunning = ref(false)
  const followUpCategoryView = ref<FollowUpCategory | ''>('')
  const savedAcceptanceBatches = ref<AnyRecord[]>([])
  const selectedAcceptanceBatchIds = ref<Array<string | number>>([])
  const availableApps = ref<AnyRecord[]>([])
  const acceptanceCompareState = reactive<{ left: AnyRecord | null; right: AnyRecord | null }>({
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
  const realBatchRunnerForm = reactive({
    appId: '',
    batchName: '',
    sceneType: '真实业务问题',
    testerName: '',
    summaryConclusion: '',
    nextAction: '',
    questions: buildInitialRealQuestionRows()
  })
  const acceptanceBatchForm = reactive<AnyRecord>({
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
  const tabOptions: Array<{ key: InvokeRecordTabKey; label: string; description: string }> = [
    { key: 'records', label: '记录', description: '筛选、查看和导出调用记录。' },
    { key: 'review', label: '复盘', description: '聚焦失败、慢请求和待跟进问题。' },
    { key: 'acceptance', label: '验收批次', description: '保存、对比和复跑验收批次。' }
  ]

  const overview = reactive<InvokeRecordOverview>({
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
      return rows.filter((row: AnyRecord) => Number(row.status) === 0)
    }
    if (quickView.value === 'slow') {
      return rows.filter((row: AnyRecord) => Number(row.costTime || 0) >= 5000)
    }
    if (quickView.value === 'long') {
      return rows.filter((row: AnyRecord) =>
        (row.detailList || []).some((detail: AnyRecord) => String(detail.assistantMessage || '').length >= 200)
      )
    }
    if (quickView.value === 'followUp') {
      return rows.filter((row: AnyRecord) =>
        (row.detailList || []).some((detail: AnyRecord) => matchFollowUpFilter(row, detail))
      )
    }
    if (quickView.value === 'reviewed') {
      return rows.filter((row: AnyRecord) =>
        (row.detailList || []).some((detail: AnyRecord) => getReviewStatus(buildReviewKey(row, detail)) === 'reviewed')
      )
    }
    return rows
  })

  const failRowCount = computed(() => (tableData.rows || []).filter((row: AnyRecord) => Number(row.status) === 0).length)
  const slowRowCount = computed(() => (tableData.rows || []).filter((row: AnyRecord) => Number(row.costTime || 0) >= 5000).length)
  const longAnswerRowCount = computed(() =>
    (tableData.rows || []).filter((row: AnyRecord) =>
      (row.detailList || []).some((detail: AnyRecord) => String(detail.assistantMessage || '').length >= 200)
    ).length
  )

  const priorityReviewList = computed(() => {
    return filteredRows.value
      .flatMap((row: AnyRecord) => {
        const detailList = row.detailList || []
        return detailList.map((detail: AnyRecord, index: number) => {
          const riskTags = buildRiskTags(row, detail)
          return {
            key: `${row.id}-${index + 1}`,
            row,
            detail,
            riskTags,
            riskScore: riskTags.reduce((sum: number, tag: AnyRecord) => sum + tag.score, 0)
          }
        })
      })
      .filter((item: AnyRecord) => item.riskTags.length > 0)
      .sort((a: AnyRecord, b: AnyRecord) => {
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
      return '只看失败记录，先排查报错和不可用问题。'
    }
    if (quickView.value === 'slow') {
      return '只看慢请求，先看耗时长的链路。'
    }
    if (quickView.value === 'long') {
      return '只看长回答，先检查是否答偏或过度展开。'
    }
    if (quickView.value === 'followUp') {
      return '只看待跟进记录，方便继续整理修复项。'
    }
    if (quickView.value === 'reviewed') {
      return '只看已复盘记录，方便回看已确认样本。'
    }
    return '查看全部记录。'
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
    queryInvokeRecordOverviewApi().then((result) => {
      Object.assign(overview, result.data || {})
    })
  }

  function loadSavedAcceptanceBatches() {
    listRagAcceptanceBatchApi().then((result) => {
      savedAcceptanceBatches.value = result.data || []
    })
  }

  function loadAvailableApps() {
    pageAppApi({
      pageNow: 1,
      pageSize: 100,
      appName: ''
    }).then((result) => {
      availableApps.value = result.data || []
    })
  }

  async function copyText(text: string, label: string) {
    try {
      await writeClipboardText(String(text || ''))
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
    filteredRows.value.forEach((row: AnyRecord) => {
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
      ;(row.detailList || []).forEach((detail: AnyRecord, index: number) => {
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
    downloadMarkdown(lines.join('\n'), `invoke-records-${Date.now()}.md`)
    ElMessage.success('已导出当前调用记录')
  }

  function buildAcceptanceEntry(row: AnyRecord, detail: AnyRecord, index: number) {
    return [
      `| TC-${String(row.id)}-${index + 1} | 待分类 | ${sanitizeTableCell(detail.userInput)} | 待补充 | ${buildAnswerSummary(detail.assistantMessage)} | 待人工判断 | 待人工判断 | 待人工判断 | ${Number(detail.status) === 0 ? '重点检查' : '待人工判断'} | 待人工判断 | 待人工判断 | 记录ID=${row.id}；应用=${sanitizeInline(row.appName)}；知识库=${sanitizeInline(row.libName)}；状态=${sanitizeInline(detail.statusDesc || row.statusDesc)}；耗时=${detail.costTime ?? row.costTime ?? '-'}ms |`,
      '',
      '补充信息：',
      `- 原始失败原因：${sanitizeInline(detail.failReason || row.failReason || '无')}`,
      `- 模型：${sanitizeInline(detail.modelName || '未知')}`,
      `- Token：${detail.costToken ?? '-'}`,
      `- 开始时间：${sanitizeInline(detail.startTime || row.startTime || '-')}`,
      `- 结束时间：${sanitizeInline(detail.endTime || row.endTime || '-')}`,
      '- 原始回答全文：',
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
    const records = filteredRows.value.flatMap((row: AnyRecord) => {
      const detailList = row.detailList || []
      if (!detailList.length) {
        return [
          `| TC-${String(row.id)}-1 | 待分类 | - | 待补充 | - | 待人工判断 | 待人工判断 | 待人工判断 | ${Number(row.status) === 0 ? '重点检查' : '待人工判断'} | 待人工判断 | 待人工判断 | 记录ID=${row.id}；应用=${sanitizeInline(row.appName)}；知识库=${sanitizeInline(row.libName)}；状态=${sanitizeInline(row.statusDesc)}；耗时=${row.costTime ?? '-'}ms |`
        ]
      }
      return detailList.map((detail: AnyRecord, index: number) => buildAcceptanceEntry(row, detail, index))
    })

    const lines = [
      '# RAG MVP 测试问题集与效果记录',
      '',
      '## 1. 当前导出说明',
      '',
      `- 导出时间：${new Date().toLocaleString()}`,
      `- 当前聚焦：${currentQuickViewDesc.value}`,
      `- 当前记录数：${filteredRows.value.length}`,
      '- 导出目的：从调用记录页生成验收草稿，便于后续人工补齐“期望知识点 / 验收结论 / 汇总结论”',
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
      `- 失败记录数：${filteredRows.value.filter((row: AnyRecord) => Number(row.status) === 0).length}`,
      `- 慢请求数：${filteredRows.value.filter((row: AnyRecord) => Number(row.costTime || 0) >= 5000).length}`,
      `- 长回答记录数：${filteredRows.value.filter((row: AnyRecord) => (row.detailList || []).some((detail: AnyRecord) => String(detail.assistantMessage || '').length >= 200)).length}`,
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

  function openRealAcceptanceBatchDialog() {
    realBatchRunnerForm.appId = availableApps.value[0]?.id || ''
    realBatchRunnerForm.batchName = `${new Date().toLocaleDateString()} 真实问题集验收`
    realBatchRunnerForm.sceneType = '真实业务问题'
    realBatchRunnerForm.testerName = ''
    realBatchRunnerForm.summaryConclusion = ''
    realBatchRunnerForm.nextAction = ''
    realBatchRunnerForm.questions = buildInitialRealQuestionRows()
    realBatchRunnerVisible.value = true
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

  function runRealAcceptanceBatch() {
    if (!realBatchRunnerForm.appId) {
      ElMessage.warning('请先选择应用')
      return
    }
    if (!String(realBatchRunnerForm.batchName || '').trim()) {
      ElMessage.warning('请先填写验收批次名称')
      return
    }
    const questions = realBatchRunnerForm.questions
      .map((item, index) => ({
        testCaseNo: String(item.testCaseNo || `RQ-${String(index + 1).padStart(2, '0')}`).trim(),
        questionType: String(item.questionType || '真实业务问题').trim(),
        userQuestion: String(item.userQuestion || '').trim(),
        expectedKnowledge: String(item.expectedKnowledge || '').trim()
      }))
      .filter((item) => item.userQuestion)
    if (!questions.length) {
      ElMessage.warning('请至少填写一条真实问题')
      return
    }
    realBatchRunning.value = true
    runRagAcceptanceBatchApi({
      appId: realBatchRunnerForm.appId,
      batchName: realBatchRunnerForm.batchName,
      sceneType: realBatchRunnerForm.sceneType,
      testerName: realBatchRunnerForm.testerName,
      summaryConclusion: realBatchRunnerForm.summaryConclusion,
      nextAction: realBatchRunnerForm.nextAction,
      questions
    }).then((result) => {
      ElMessage.success('已完成真实问题集跑测，并生成正式验收批次')
      realBatchRunnerVisible.value = false
      loadSavedAcceptanceBatches()
      if (result.data) {
        openSavedAcceptanceBatch(result.data)
      }
    }).finally(() => {
      realBatchRunning.value = false
    })
  }

  function appendRealQuestionRow() {
    realBatchRunnerForm.questions.push(createRealQuestionRow(realBatchRunnerForm.questions.length + 1))
  }

  function removeRealQuestionRow(index: number) {
    if (realBatchRunnerForm.questions.length <= 1) {
      ElMessage.warning('至少保留一条真实问题')
      return
    }
    realBatchRunnerForm.questions.splice(index, 1)
  }

  function loadRealQuestionsFromCurrentRows() {
    const entries = filteredRows.value.flatMap((row: AnyRecord) => {
      const detailList = row.detailList || []
      return detailList.map((detail: AnyRecord, index: number) => ({
        localKey: `${row.id}-${index + 1}`,
        testCaseNo: `RQ-${row.id}-${index + 1}`,
        questionType: '真实业务问题',
        userQuestion: String(detail.userInput || '').trim(),
        expectedKnowledge: ''
      }))
    }).filter((item: RealQuestionRow) => item.userQuestion)
    if (!entries.length) {
      ElMessage.warning('当前筛选结果没有可带入的用户问题')
      return
    }
    realBatchRunnerForm.questions = entries
    ElMessage.success(`已带入 ${entries.length} 条真实问题`)
  }

  function buildAcceptanceBatchItems() {
    return filteredRows.value.flatMap((row: AnyRecord) => {
      const detailList = row.detailList || []
      return detailList.map((detail: AnyRecord, index: number) => {
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
    const existingKeys = new Set(acceptanceBatchForm.items.map((item: AnyRecord) => item.localKey || item.testCaseNo))
    const newItems = currentItems.filter((item: AnyRecord) => !existingKeys.has(item.localKey))
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
      items: acceptanceBatchForm.items.map((item: AnyRecord) => ({
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
    queryRagAcceptanceBatchDetailApi(id).then((result) => {
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
        items: (data.items || []).map((item: AnyRecord, index: number) => ({
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
      downloadMarkdown(content, buildRepairTaskFilename(batch))
      ElMessage.success('已导出修复建议')
    })
  }

  async function rerunAcceptanceBatch(batchId: string | number) {
    const result = await queryRagAcceptanceBatchDetailApi(batchId)
    const batch = result.data || {}
    const items = Array.isArray(batch.items) ? batch.items : []
    if (!batch.appId) {
      ElMessage.warning('当前批次缺少应用信息，无法直接复跑')
      return
    }
    if (!items.length) {
      ElMessage.warning('当前批次没有可复跑的问题条目')
      return
    }
    const questions = items
      .map((item: AnyRecord, index: number) => ({
        testCaseNo: String(item.testCaseNo || `RQ-${String(index + 1).padStart(2, '0')}`).trim(),
        questionType: String(item.questionType || '真实业务问题').trim(),
        userQuestion: String(item.userQuestion || '').trim(),
        expectedKnowledge: String(item.expectedKnowledge || '').trim()
      }))
      .filter((item: AnyRecord) => item.userQuestion)
    if (!questions.length) {
      ElMessage.warning('当前批次缺少可复跑的用户问题')
      return
    }

    realBatchRunning.value = true
    try {
      const rerunResult = await runRagAcceptanceBatchApi({
        appId: batch.appId,
        batchName: buildRerunBatchName(batch),
        sceneType: batch.sceneType || '真实业务问题',
        testerName: batch.testerName || 'codex',
        summaryConclusion: '',
        nextAction: `基于批次 ${batch.batchName || batch.id} 按当前生效版本重新复跑，准备与上一轮正式验收直接对比。`,
        questions
      })
      ElMessage.success('已按当前生效版本完成复跑，并生成新的正式验收批次')
      loadSavedAcceptanceBatches()
      const newBatchId = rerunResult.data
      if (newBatchId) {
        const nextSelectedIds = [batch.id, newBatchId].filter(Boolean)
        selectedAcceptanceBatchIds.value = nextSelectedIds.slice(-2)
        await openAcceptanceCompareDialog()
      }
    } finally {
      realBatchRunning.value = false
    }
  }

  function buildRerunBatchName(batch: AnyRecord) {
    const baseName = String(batch.batchName || '正式验收批次').trim()
    const now = new Date()
    const dateText = [
      now.getFullYear(),
      String(now.getMonth() + 1).padStart(2, '0'),
      String(now.getDate()).padStart(2, '0')
    ].join('-')
    return `${dateText} ${baseName} 当前生效版本复跑`
  }

  function openAcceptanceRepairTaskDialog(batchId: string | number) {
    queryRagAcceptanceBatchDetailApi(batchId).then((result) => {
      const batch = result.data || {}
      const content = buildAcceptanceRepairDraft(batch)
      if (!content) {
        ElMessage.warning('当前批次还没有可整理进任务池的问题')
        return
      }
      repairTaskPoolDialogTitle.value = `${batch.batchName || '当前批次'} 任务池草稿`
      repairTaskPoolContent.value = content
      repairTaskPoolFilename.value = buildRepairTaskFilename(batch)
      repairTaskPoolDialogVisible.value = true
    })
  }

  function exportRepairTaskPoolContent() {
    if (!repairTaskPoolContent.value) {
      ElMessage.warning('当前没有可导出的任务池草稿')
      return
    }
    downloadMarkdown(repairTaskPoolContent.value, repairTaskPoolFilename.value || `rag-acceptance-task-pool-${Date.now()}.md`)
    ElMessage.success('已导出任务池草稿')
  }

  function exportSavedAcceptanceBatch(batch: AnyRecord) {
    queryRagAcceptanceBatchDetailApi(batch.id).then((result) => {
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

  function buildAcceptanceBatchMarkdown(batch: AnyRecord) {
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
    ;(batch.items || []).forEach((item: AnyRecord) => {
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
    return filteredRows.value.flatMap((row: AnyRecord) => {
      const detailList = row.detailList || []
      return detailList
        .map((detail: AnyRecord) => {
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
    }) as Array<{ key: string; row: AnyRecord; detail: AnyRecord; category: FollowUpCategory }>
  }

  function matchFollowUpCategoryView(category: FollowUpCategory) {
    if (!followUpCategoryView.value) {
      return true
    }
    return followUpCategoryView.value === category
  }

  function resetFilters() {
    searchData.appName = ''
    searchData.userInputKeyword = ''
    searchData.status = ''
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

  function openAcceptanceDialog(row: AnyRecord, detail: AnyRecord) {
    const detailList = row.detailList || []
    const index = Math.max(detailList.findIndex((item: AnyRecord) => item === detail), 0)
    acceptanceDialogContent.value = buildAcceptanceEntry(row, detail, index)
    acceptanceDialogVisible.value = true
  }

  async function copyAcceptanceEntry(row: AnyRecord, detail: AnyRecord) {
    const detailList = row.detailList || []
    const index = Math.max(detailList.findIndex((item: AnyRecord) => item === detail), 0)
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

  function buildRiskTags(row: AnyRecord, detail: AnyRecord) {
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

  function buildReviewKey(row: AnyRecord, detail: AnyRecord) {
    const detailList = row.detailList || []
    const index = Math.max(detailList.findIndex((item: AnyRecord) => item === detail), 0)
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

  function matchFollowUpFilter(row: AnyRecord, detail: AnyRecord) {
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

  function buildAcceptanceBatchStats(batch: AnyRecord) {
    const items = batch.items || []
    const initFollowUp = {
      knowledge: 0,
      chunking: 0,
      prompt: 0,
      ui: 0,
      observe: 0,
      other: 0
    }
    return items.reduce((acc: AnyRecord, item: AnyRecord) => {
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

  function buildAcceptanceCompareMarkdown(left: AnyRecord, right: AnyRecord) {
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

  function buildAcceptanceRepairDraft(batch: AnyRecord) {
    const items = batch.items || []
    const grouped: Record<string, AnyRecord[]> = {
      knowledge: [],
      chunking: [],
      prompt: [],
      ui: [],
      observe: [],
      other: []
    }
    items.forEach((item: AnyRecord) => {
      const category = String(item.followUpCategory || 'other')
      const key = Object.prototype.hasOwnProperty.call(grouped, category) ? category : 'other'
      if (item.followUpCategory || item.followUpAction || item.remark || item.hitConclusion === '不通过' || item.groundedConclusion === '不通过' || item.readableConclusion === '不通过' || item.gracefulFailureConclusion === '不通过') {
        grouped[key].push(item)
      }
    })

    const taskGroups = Object.entries(grouped)
      .filter(([, categoryItems]) => categoryItems.length > 0)
      .map(([category, categoryItems]) => buildRepairTaskGroup(batch, category as FollowUpCategory, categoryItems))

    if (!taskGroups.length) {
      return ''
    }

    const lines = [
      '# RAG 验收后任务池草稿',
      '',
      `- 验收批次：${batch.batchName || '-'}`,
      `- 应用：${batch.appName || '-'}`,
      `- 场景类型：${batch.sceneType || '-'}`,
      `- 知识库范围：${batch.knowledgeScope || '-'}`,
      `- 发布版本：${batch.releaseVersion || '-'}`,
      `- 实验版本：${batch.experimentVersion || '-'}`,
      `- 生效实验版本：${batch.activeExperimentName || '-'} (${batch.activeSplitStrategy || '-'})`,
      `- 生成时间：${new Date().toLocaleString()}`,
      '',
      '## 任务池总览',
      ''
    ]

    taskGroups.forEach((group, index) => {
      lines.push(`## 任务 ${index + 1}：${group.title}`)
      lines.push('')
      lines.push(`- 建议优先级：${group.priority}`)
      lines.push(`- 问题分类：${group.label}`)
      lines.push(`- 影响条数：${group.items.length}`)
      lines.push(`- 影响维度：${group.dimensions.join(' / ') || '待补充'}`)
      lines.push(`- 任务摘要：${group.summary}`)
      lines.push(`- 建议动作：${group.actions.join('；')}`)
      lines.push('')
      lines.push('典型样例：')
      group.items.slice(0, 3).forEach((item: AnyRecord) => {
        lines.push(`- ${sanitizeInline(item.testCaseNo)} ${sanitizeInline(item.userQuestion)}`)
        lines.push(`  当前结论：命中=${sanitizeInline(item.hitConclusion || '待确认')} / 可信=${sanitizeInline(item.groundedConclusion || '待确认')} / 易懂=${sanitizeInline(item.readableConclusion || '待确认')} / 失败体面=${sanitizeInline(item.gracefulFailureConclusion || '待确认')}`)
        lines.push(`  原始失败原因：${sanitizeInline(item.failReason || '无')}`)
        lines.push(`  建议动作：${sanitizeInline(item.followUpAction || item.remark || group.actions[0] || '待补充')}`)
      })
      lines.push('')
      lines.push('回归验收：')
      group.acceptanceChecks.forEach((check: string) => {
        lines.push(`- ${check}`)
      })
      lines.push('')
    })

    lines.push('## 排期建议')
    lines.push('')
    lines.push('- 先做 `P0` 的补知识、补切分、补提示词，这三类最直接影响“命中问题 / 可信 / 失败体面”。')
    lines.push('- 每完成一类修复后，至少复跑当前批次中的对应样例，再决定是否继续扩充真实问题集。')
    lines.push('- 如果同类问题跨多个批次反复出现，再拆成更细的知识任务或提示词任务。')
    return lines.join('\n')
  }

  function buildRepairTaskGroup(batch: AnyRecord, category: FollowUpCategory, items: AnyRecord[]) {
    const label = getFollowUpLabel(category) || '其他'
    const dimensions = resolveRepairTaskDimensions(items)
    const priority = resolveRepairTaskPriority(category, dimensions, items)
    const actions = collectRepairTaskActions(category, items)
    return {
      label,
      items,
      priority,
      title: buildRepairTaskTitle(batch, label, priority),
      summary: `当前批次中共有 ${items.length} 条问题需要围绕“${label}”处理，主要影响 ${dimensions.join(' / ') || '整体可用性'}。`,
      actions,
      dimensions,
      acceptanceChecks: buildRepairTaskAcceptanceChecks(category, items)
    }
  }

  function resolveRepairTaskDimensions(items: AnyRecord[]) {
    const dimensions = new Set<string>()
    items.forEach((item) => {
      if (item.hitConclusion && item.hitConclusion !== '通过') {
        dimensions.add('命中问题')
      }
      if (item.groundedConclusion && item.groundedConclusion !== '通过') {
        dimensions.add('可信')
      }
      if (item.readableConclusion && item.readableConclusion !== '通过') {
        dimensions.add('易懂')
      }
      if (item.gracefulFailureConclusion && item.gracefulFailureConclusion !== '通过') {
        dimensions.add('失败体面')
      }
    })
    return [...dimensions]
  }

  function resolveRepairTaskPriority(category: FollowUpCategory, dimensions: string[], items: AnyRecord[]) {
    const hasCriticalDimension = dimensions.includes('可信') || dimensions.includes('失败体面') || dimensions.includes('命中问题')
    const hasHardFail = items.some((item) =>
      item.hitConclusion === '不通过' ||
      item.groundedConclusion === '不通过' ||
      item.gracefulFailureConclusion === '不通过'
    )
    if (category === 'knowledge' || category === 'chunking' || category === 'prompt') {
      return hasCriticalDimension || hasHardFail ? 'P0' : 'P1'
    }
    if (category === 'observe') {
      return hasHardFail ? 'P1' : 'P2'
    }
    if (category === 'ui') {
      return hasCriticalDimension ? 'P1' : 'P2'
    }
    return hasCriticalDimension ? 'P1' : 'P2'
  }

  function collectRepairTaskActions(category: FollowUpCategory, items: AnyRecord[]) {
    const actions = items
      .map((item) => String(item.followUpAction || item.remark || '').trim())
      .filter(Boolean)
    const fallbackActions: Record<FollowUpCategory, string> = {
      knowledge: '补齐当前问题依赖的知识源，并重新验证是否能给出有依据的回答',
      chunking: '调整切分策略和召回方式，确认正确知识能稳定进入前置候选',
      prompt: '收紧提示词约束，优化回答结构、依据表达和失败反馈文案',
      ui: '优化前端展示文案、引用说明或交互入口，降低运营和用户理解成本',
      observe: '补充日志字段、失败原因和链路观测信息，提升排障效率',
      other: '结合当前样例补充更明确的修复动作'
    }
    const deduped = [...new Set(actions)]
    if (!deduped.length) {
      return [fallbackActions[category]]
    }
    return deduped.slice(0, 3)
  }

  function buildRepairTaskAcceptanceChecks(category: FollowUpCategory, items: AnyRecord[]) {
    const sampleList = items
      .slice(0, 3)
      .map((item) => `${sanitizeInline(item.testCaseNo)} ${sanitizeInline(item.userQuestion)}`)
    const checks = [
      `复跑当前分类下的 ${items.length} 条样例，至少确保本任务影响维度不再出现“明显不通过”。`,
      `重点回归：${sampleList.join('；')}`
    ]
    if (category === 'knowledge') {
      checks.push('回答中的关键结论能够被知识库内容支撑，不再依赖无依据补全。')
    }
    if (category === 'chunking') {
      checks.push('正确知识应稳定进入前置召回候选，避免答非所问或只抓住局部关键词。')
    }
    if (category === 'prompt') {
      checks.push('失败场景下需要明确说明依据不足或暂不可用，并给出下一步建议。')
    }
    if (category === 'ui') {
      checks.push('运营人能直接看懂当前问题、建议动作和回归口径，不需要再手工二次整理。')
    }
    if (category === 'observe') {
      checks.push('出现异常时能从日志或记录页直接定位失败阶段，而不是只看到模糊错误。')
    }
    return checks
  }

  function buildRepairTaskTitle(batch: AnyRecord, label: string, priority: string) {
    return `【${priority}】${batch.appName || '当前应用'} ${label}修复`
  }

  function buildRepairTaskFilename(batch: AnyRecord) {
    const batchId = batch.id || Date.now()
    return `rag-acceptance-task-pool-${batchId}.md`
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

  function buildInitialRealQuestionRows() {
    return [
      createRealQuestionRow(1),
      createRealQuestionRow(2),
      createRealQuestionRow(3)
    ]
  }

  function createRealQuestionRow(index: number): RealQuestionRow {
    return {
      localKey: `real-question-${Date.now()}-${index}-${Math.random().toString(16).slice(2, 8)}`,
      testCaseNo: `RQ-${String(index).padStart(2, '0')}`,
      questionType: '真实业务问题',
      userQuestion: '',
      expectedKnowledge: ''
    }
  }

  async function initialize() {
    loadTable()
    loadOverview()
    loadSavedAcceptanceBatches()
    loadAvailableApps()
  }

  return reactive({
    acceptanceBatchDialogTitle,
    acceptanceBatchDialogVisible,
    acceptanceBatchForm,
    acceptanceCompareDialogVisible,
    acceptanceCompareDraftContent,
    acceptanceCompareRows,
    acceptanceCompareState,
    activeTab,
    appendCurrentRecordItemsToBatch,
    appendRealQuestionRow,
    availableApps,
    conclusionOptions,
    copyAcceptanceEntry,
    copyText,
    currentQuickViewDesc,
    cycleReviewStatus,
    categorizedFollowUpCount,
    defaultAcceptanceQuestionPool,
    defaultBatchRunnerForm,
    defaultBatchRunnerVisible,
    defaultBatchRunning,
    detailDialogContent,
    detailDialogTitle,
    detailDialogVisible,
    exportAcceptanceBatchDialog,
    exportAcceptanceCompareDraft,
    exportAcceptanceDraft,
    exportAcceptanceRepairDraft,
    exportCurrentRows,
    exportFollowUpTaskDraft,
    exportRepairTaskPoolContent,
    exportSavedAcceptanceBatch,
    failRowCount,
    filteredRows,
    followUpCategoryDescMap,
    followUpCategoryLabelMap,
    followUpCategoryOptions,
    followUpCategoryStats,
    followUpCategoryView,
    followUpCount,
    formatDateTime,
    getFollowUpCategory,
    getFollowUpCategoryLabel,
    getFollowUpLabel,
    getReviewStatus,
    handlePageNowChange,
    handlePageSizeChange,
    initialize,
    loadTable,
    loadRealQuestionsFromCurrentRows,
    longAnswerRowCount,
    nextReviewActionText,
    openAcceptanceCompareDialog,
    openAcceptanceDialog,
    openAcceptanceRepairTaskDialog,
    openDetailDialog,
    openRealAcceptanceBatchDialog,
    openSaveAcceptanceBatchDialog,
    openSavedAcceptanceBatch,
    openTaskSuggestionDialog,
    openTemplateAcceptanceBatchDialog,
    overview,
    paginationTotal: computed(() => tableData.total),
    priorityReviewList,
    quickView,
    realBatchRunnerForm,
    realBatchRunnerVisible,
    realBatchRunning,
    recordRows: computed(() => tableData.rows || []),
    repairTaskPoolContent,
    repairTaskPoolDialogTitle,
    repairTaskPoolDialogVisible,
    resetFilters,
    reviewStatusTagType,
    reviewStatusText,
    runDefaultAcceptanceBatch,
    runRealAcceptanceBatch,
    savedAcceptanceBatches,
    searchData,
    selectedAcceptanceBatchIds,
    slowRowCount,
    successRate,
    summarizeDisplayText,
    tabOptions,
    tableData,
    taskSuggestionContent,
    taskSuggestionDialogVisible,
    toggleAcceptanceBatchSelection,
    updateFollowUpCategory,
    acceptanceDialogContent,
    acceptanceDialogVisible,
    removeRealQuestionRow,
    rerunAcceptanceBatch,
    saveAcceptanceBatch,
    reviewStatusMap,
    followUpCategoryMap,
    buildReviewKey
  })
}

export type InvokeRecordFeatureModel = ReturnType<typeof createInvokeRecordFeatureModel>

export function provideInvokeRecordFeatureModel() {
  const model = createInvokeRecordFeatureModel()
  provide(invokeRecordFeatureKey, model)
  return model
}

export function useInvokeRecordFeatureModel() {
  const model = inject<InvokeRecordFeatureModel>(invokeRecordFeatureKey)
  if (!model) {
    throw new Error('invoke record feature model not provided')
  }
  return model
}
