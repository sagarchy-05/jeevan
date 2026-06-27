import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError, apiRequest } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

export default function VerifyPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [status, setStatus] = useState('verifying') // verifying | success | invalid
  const { isAuthenticated, authedRequest, refreshUser } = useAuth()
  const { showToast } = useToast()
  const ran = useRef(false)

  useEffect(() => {
    if (ran.current) return // guard against StrictMode double-invoke consuming the token twice
    ran.current = true

    if (!token) {
      setStatus('invalid')
      return
    }
    apiRequest(`/auth/verify?token=${encodeURIComponent(token)}`)
      .then(async () => {
        setStatus('success')
        if (isAuthenticated) {
          try {
            await refreshUser()
          } catch {
            /* best effort */
          }
        }
      })
      .catch(() => setStatus('invalid'))
  }, [token, isAuthenticated, refreshUser])

  const onResend = async () => {
    try {
      const res = await authedRequest('/auth/resend-verification', { method: 'POST' })
      showToast(res?.message || 'Verification email sent.', 'success')
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        showToast('Please log in to resend a verification email.', 'error')
      } else {
        showToast('Could not resend. Try again.', 'error')
      }
    }
  }

  return (
    <div className="mx-auto max-w-md text-center">
      {status === 'verifying' && <p className="text-slate-600">Verifying your email…</p>}

      {status === 'success' && (
        <div>
          <h1 className="text-2xl font-semibold text-emerald-700">Email verified ✓</h1>
          <p className="mt-2 text-slate-600">You can now book appointments.</p>
          <Link
            to="/doctors"
            className="mt-6 inline-block rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
          >
            Find a doctor
          </Link>
        </div>
      )}

      {status === 'invalid' && (
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">This link is no longer valid</h1>
          <p className="mt-2 text-slate-600">It may have expired or already been used.</p>
          {isAuthenticated ? (
            <button
              onClick={onResend}
              className="mt-6 inline-block rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
            >
              Resend verification email
            </button>
          ) : (
            <Link
              to="/login"
              className="mt-6 inline-block rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
            >
              Log in to resend
            </Link>
          )}
        </div>
      )}
    </div>
  )
}
