import { useEffect } from 'react'

interface ToastProps {
  message: string
  type: 'success' | 'error'
  onDismiss: () => void
}

export function Toast({ message, type, onDismiss }: ToastProps) {
  useEffect(() => {
    if (type !== 'success') return
    const timer = setTimeout(onDismiss, 3500)
    return () => clearTimeout(timer)
  }, [message, type, onDismiss])

  return (
    <div className={`notice ${type}`} role={type === 'error' ? 'alert' : 'status'}>
      <span>{message}</span>
      <button onClick={onDismiss} aria-label={`Dismiss ${type}`}>
        ×
      </button>
    </div>
  )
}
