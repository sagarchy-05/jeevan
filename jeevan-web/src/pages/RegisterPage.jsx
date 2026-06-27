import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, apiRequest } from '../api/client'
import Field from '../components/Field'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

export default function RegisterPage() {
  const [form, setForm] = useState({ fullName: '', email: '', password: '', phone: '' })
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  const { login } = useAuth()
  const { showToast } = useToast()
  const navigate = useNavigate()

  const update = (key) => (value) => setForm((f) => ({ ...f, [key]: value }))

  const onSubmit = async (e) => {
    e.preventDefault()
    setFieldErrors({})
    setSubmitting(true)
    try {
      await apiRequest('/auth/register', { method: 'POST', body: form })
      // Smooth demo flow: auto-login with the same credentials.
      const res = await apiRequest('/auth/login', {
        method: 'POST',
        body: { email: form.email, password: form.password },
      })
      login(res.token, res.user)
      showToast('Welcome to Jeevan!', 'success')
      navigate('/doctors', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.code === 'VALIDATION_FAILED' && err.fieldErrors) {
        setFieldErrors(err.fieldErrors)
      } else if (err instanceof ApiError && err.code === 'EMAIL_ALREADY_EXISTS') {
        setFieldErrors({ email: 'An account with this email already exists.' })
      } else {
        showToast(err.message || 'Something went wrong, try again.', 'error')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-6 text-2xl font-semibold text-slate-900">Create your account</h1>
      <form onSubmit={onSubmit} className="space-y-4">
        <Field label="Full name" value={form.fullName} onChange={update('fullName')} error={fieldErrors.fullName} />
        <Field label="Email" type="email" value={form.email} onChange={update('email')} error={fieldErrors.email} />
        <Field
          label="Password"
          type="password"
          value={form.password}
          onChange={update('password')}
          error={fieldErrors.password}
        />
        <Field label="Phone" value={form.phone} onChange={update('phone')} error={fieldErrors.phone} />
        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
        >
          {submitting ? 'Creating…' : 'Register'}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-slate-900 underline">
          Log in
        </Link>
      </p>
    </div>
  )
}
