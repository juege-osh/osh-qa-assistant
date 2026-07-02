export interface PublicMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string
}

export interface PublicChatSession {
  id: string
  title: string
  createdAt: string
  updatedAt: string
  messages: PublicMessage[]
}

export interface PublicAppDetail {
  slug: string
  appName: string
  appDesc: string
  iconPath: string
  accessType: string
  passwordRequired: boolean
}

export interface PublicPasswordForm {
  accessPassword: string
}
