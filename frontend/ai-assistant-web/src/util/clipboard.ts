function fallbackCopyText(text: string) {
  if (typeof document === 'undefined') {
    throw new Error('document is not available')
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.top = '0'
  textarea.style.left = '0'
  textarea.style.opacity = '0'
  textarea.style.pointerEvents = 'none'

  const selection = document.getSelection()
  const previousRange = selection && selection.rangeCount > 0 ? selection.getRangeAt(0) : null
  const activeElement = document.activeElement instanceof HTMLElement ? document.activeElement : null

  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  textarea.setSelectionRange(0, textarea.value.length)

  const copied = document.execCommand('copy')
  document.body.removeChild(textarea)

  if (activeElement) {
    activeElement.focus()
  }
  if (selection) {
    selection.removeAllRanges()
    if (previousRange) {
      selection.addRange(previousRange)
    }
  }

  if (!copied) {
    throw new Error('execCommand copy failed')
  }
}

export async function writeClipboardText(text: string) {
  const normalizedText = String(text ?? '')

  if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(normalizedText)
      return
    } catch {
      // Fallback is required for insecure contexts and browsers that expose the API but reject writes.
    }
  }

  fallbackCopyText(normalizedText)
}
