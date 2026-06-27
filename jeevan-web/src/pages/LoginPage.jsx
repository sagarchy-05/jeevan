import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError, apiRequest } from '../api/client'
import Field from '../components/Field'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  const { login } = useAuth()
  const { showToast } = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from?.pathname || '/doctors'

  const onSubmit = async (e) => {
    e.preventDefault()
    setFieldErrors({})
    setSubmitting(true)
    try {
      const res = await apiRequest('/auth/login', { method: 'POST', body: { email, password } })
      login(res.token, res.user)
      navigate(from, { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.code === 'VALIDATION_FAILED' && err.fieldErrors) {
        setFieldErrors(err.fieldErrors)
      } else if (err instanceof ApiError && err.code === 'INVALID_CREDENTIALS') {
        showToast('Invalid email or password.', 'error')
      } else {
        showToast(err.message || 'Something went wrong, try again.', 'error')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-6 text-2xl font-semibold text-slate-900">Log in</h1>
      <form onSubmit={onSubmit} className="space-y-4">
        <Field label="Email" type="email" value={email} onChange={setEmail} error={fieldErrors.email} />
        <Field
          label="Password"
          type="password"
          value={password}
          onChange={setPassword}
          error={fieldErrors.password}
        />
        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
        >
          {submitting ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        No account?{' '}
        <Link to="/register" className="font-medium text-slate-900 underline">
          Register
        </Link>
      </p>
    </div>
  )
}
