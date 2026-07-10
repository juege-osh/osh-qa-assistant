export type InvokeRecordTabKey = 'records' | 'review' | 'acceptance'

export type ReviewStatus = 'pending' | 'reviewed' | 'followUp'

export type FollowUpCategory = 'knowledge' | 'chunking' | 'prompt' | 'ui' | 'observe' | 'other'

export interface InvokeRecordSearchForm {
  appName: string
  userInputKeyword: string
  status: string
  startTime: string | null
  endTime: string | null
}

export interface InvokeRecordOverview {
  totalCount: number
  successCount: number
  failCount: number
  totalCostToken: number
  avgCostTime: number
}

export interface RealQuestionRow {
  localKey: string
  testCaseNo: string
  questionType: string
  userQuestion: string
  expectedKnowledge: string
}
