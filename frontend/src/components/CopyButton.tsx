import { useEffect, useRef, useState } from 'react'

export function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false)
  const resetTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    return () => {
      if (resetTimer.current !== null) clearTimeout(resetTimer.current)
    }
  }, [])

  async function copy() {
    try {
      await navigator.clipboard.writeText(text)
      setCopied(true)
      if (resetTimer.current !== null) clearTimeout(resetTimer.current)
      resetTimer.current = setTimeout(() => setCopied(false), 1400)
    } catch {
      setCopied(false)
    }
  }

  return (
    <button
      className="copy-btn"
      onClick={copy}
      aria-label={`Copy ${text}`}
      title={copied ? 'Copied!' : `Copy ${text}`}
      type="button"
    >
      {copied ? (
        <svg width="13" height="13" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path
            d="M3.5 8.5L6.5 11.5L12.5 4.5"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      ) : (
        <svg width="13" height="13" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <rect
            x="5.5"
            y="5.5"
            width="8"
            height="8"
            rx="1.5"
            stroke="currentColor"
            strokeWidth="1.4"
          />
          <path
            d="M10.5 5.5V3.5C10.5 2.67 9.83 2 9 2H3.5C2.67 2 2 2.67 2 3.5V9C2 9.83 2.67 10.5 3.5 10.5H5.5"
            stroke="currentColor"
            strokeWidth="1.4"
          />
        </svg>
      )}
    </button>
  )
}
