import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'

const ToastContext = createContext(null)

const COLORS = {
  error: 'bg-red-600',
  success: 'bg-emerald-600',
  info: 'bg-slate-800',
}

const STAY_MS = 3000
const EXIT_MS = 300

/** One toast: slides + fades in on mount, out after STAY_MS, then removes itself. */
function ToastItem({ toast, onDone }) {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const enter = requestAnimationFrame(() => setVisible(true))
    const stay = window.setTimeout(() => setVisible(false), STAY_MS)
    const remove = window.setTimeout(() => onDone(toast.id), STAY_MS + EXIT_MS)
    return () => {
      cancelAnimationFrame(enter)
      window.clearTimeout(stay)
      window.clearTimeout(remove)
    }
  }, [toast.id, onDone])

  return (
    <div
      role="status"
      className={`transform rounded-md px-4 py-3 text-sm text-white shadow-lg transition-all duration-300 ease-out ${
        visible ? 'translate-x-0 opacity-100' : 'translate-x-8 opacity-0'
      } ${COLORS[toast.type] || COLORS.info}`}
    >
      {toast.message}
    </div>
  )
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const idRef = useRef(0)

  const showToast = useCallback((message, type = 'info') => {
    const id = (idRef.current += 1)
    setToasts((prev) => [...prev, { id, message, type }])
    return id
  }, [])

  const remove = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="pointer-events-none fixed right-4 top-4 z-50 flex w-72 flex-col gap-2">
        {toasts.map((toast) => (
          <ToastItem key={toast.id} toast={toast} onDone={remove} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}
