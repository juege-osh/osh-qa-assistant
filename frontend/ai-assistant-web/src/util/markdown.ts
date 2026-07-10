const BOLD_LABEL_WITHOUT_SEPARATOR_RE = /\*\*([^*\n]*?[：:])\s*\*\*(?=\S)/gu

export function normalizeAssistantMarkdown(source: string) {
  return String(source || '').replace(BOLD_LABEL_WITHOUT_SEPARATOR_RE, (_, label: string) => {
    return `**${label.trimEnd()}** `
  })
}
