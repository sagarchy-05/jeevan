import { ApiError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

/**
 * Shown only to a logged-in but unverified user (so it never appears on the
 * default log-only path, where everyone is verified). Offers a resend.
 */
export default function VerifyBanner() {
  const { isAuthenticated, user, authedRequest } = useAuth()
  const { showToast } = useToast()

  if (!isAuthenticated || !user || user.emailVerified) return null

  const onResend = async () => {
    try {
      const res = await authedRequest('/auth/resend-verification', { method: 'POST' })
      showToast(res?.message || 'Verification email sent.', 'success')
    } catch (err) {
      if (!(err instanceof ApiError && err.status === 401)) {
        showToast('Could not resend. Try again.', 'error')
      }
    }
  }

  return (
    <div className="border-b border-amber-200 bg-amber-50 text-amber-800">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-2 text-sm">
        <span>Please verify your email to book appointments.</span>
        <button onClick={onResend} className="font-medium underline hover:no-underline">
          Resend email
        </button>
      </div>
    </div>
  )
}
