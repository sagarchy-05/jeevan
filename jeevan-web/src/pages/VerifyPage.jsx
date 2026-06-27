import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { apiRequest } from '../api/client'
import ResendVerificationForm from '../components/ResendVerificationForm'
import { useAuth } from '../context/AuthContext'

export default function VerifyPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [status, setStatus] = useState('verifying') // verifying | success | invalid
  const { isAuthenticated, refreshUser } = useAuth()
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

  return (
    <div className="mx-auto max-w-md text-center">
      {status === 'verifying' && <p className="text-slate-600">Verifying your email…</p>}

      {status === 'success' && (
        <div>
          <h1 className="text-2xl font-semibold text-emerald-700">Email verified ✓</h1>
          <p className="mt-2 text-slate-600">You can now log in and book appointments.</p>
          <Link
            to="/login"
            className="mt-6 inline-block rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
          >
            Log in
          </Link>
        </div>
      )}

      {status === 'invalid' && (
        <div className="text-left">
          <h1 className="text-center text-2xl font-semibold text-slate-900">
            This link is no longer valid
          </h1>
          <p className="mt-2 text-center text-slate-600">
            It may have expired or already been used. Enter your email to get a new link:
          </p>
          <ResendVerificationForm />
        </div>
      )}
    </div>
  )
}
