import { createContext, useCallback, useContext, useRef, useState } from 'react'

const ToastContext = createContext(null)

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null)
  const timerRef = useRef(null)

  const showToast = useCallback((message, type = 'info') => {
    setToast({ message, type })
    if (timerRef.current) window.clearTimeout(timerRef.current)
    timerRef.current = window.setTimeout(() => setToast(null), 4000)
  }, [])

  const colors = {
    error: 'bg-red-600',
    success: 'bg-emerald-600',
    info: 'bg-slate-800',
  }

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {toast && (
        <div
          className={`fixed bottom-4 right-4 z-50 max-w-sm rounded-md px-4 py-3 text-sm text-white shadow-lg ${colors[toast.type] || colors.info}`}
          role="status"
        >
          {toast.message}
        </div>
      )}
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}
