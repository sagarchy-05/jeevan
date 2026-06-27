import { useState } from 'react'
import { apiRequest } from '../api/client'
import { useToast } from '../context/ToastContext'

/** Unauthenticated resend-by-email (used on login + verify screens). */
export default function ResendVerificationForm({ defaultEmail = '' }) {
  const [email, setEmail] = useState(defaultEmail)
  const [sending, setSending] = useState(false)
  const { showToast } = useToast()

  const onResend = async () => {
    if (!email) return
    setSending(true)
    try {
      const res = await apiRequest('/auth/resend-verification', { method: 'POST', body: { email } })
      showToast(res?.message || 'If an account exists for that email, a link was sent.', 'success')
    } catch {
      showToast('Could not resend. Try again.', 'error')
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="mt-4 flex gap-2">
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="you@example.com"
        className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-200"
      />
      <button
        disabled={sending || !email}
        onClick={onResend}
        className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
      >
        {sending ? 'Sending…' : 'Resend'}
      </button>
    </div>
  )
}
