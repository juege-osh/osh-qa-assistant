export type ChatFeedback = '' | 'up' | 'down'

export interface WorkspaceChatSession {
  id: string
  chatName: string
  createTime?: string
}

export interface WorkspaceChatMessage {
  typeDesc: 'user' | 'assistant'
  message: string
  html?: string
  feedback?: ChatFeedback
}
