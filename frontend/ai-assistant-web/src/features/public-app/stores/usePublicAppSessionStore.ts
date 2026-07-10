import { defineStore } from 'pinia'
import type { PublicChatSession } from '../types'

export const usePublicAppSessionStore = defineStore('public-app-session', {
  state: () => ({
    sessions: [] as PublicChatSession[],
    activeSessionId: ''
  }),
  actions: {
    setSessions(sessions: PublicChatSession[]) {
      this.sessions = sessions
    },
    setActiveSessionId(sessionId: string) {
      this.activeSessionId = sessionId
    },
    reset() {
      this.sessions = []
      this.activeSessionId = ''
    }
  }
})
