import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import ConfirmModal from '../components/ConfirmModal'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { formatDateTime } from '../utils/datetime'
import { pollNotificationStatus } from '../utils/notifications'
import { pace, STEP_GAP_MS } from '../utils/pacing'

function notificationLabel(appointment) {
  const cancelled = appointment.appointmentStatus === 'CANCELLED'
  switch (appointment.notificationStatus) {
    case 'PENDING':
      return cancelled ? 'Sending cancellation…' : 'Sending confirmation…'
    case 'SENT':
      return cancelled ? 'Cancellation sent ✓' : 'Confirmation sent ✓'
    default:
      return 'Notification failed'
  }
}

export default function AppointmentsPage() {
  const { authedRequest } = useAuth()
  const { showToast } = useToast()
  const [appointments, setAppointments] = useState([])
  const [loading, setLoading] = useState(true)
  const [confirmTarget, setConfirmTarget] = useState(null)
  const [cancelling, setCancelling] = useState(false)

  const load = useCallback(async () => {
    try {
      const data = await authedRequest('/appointments')
      setAppointments(data || [])
    } catch (err) {
      if (!(err instanceof ApiError && err.status === 401)) {
        showToast('Could not load appointments.', 'error')
      }
    } finally {
      setLoading(false)
    }
  }, [authedRequest, showToast])

  useEffect(() => {
    load()
  }, [load])

  // Live notification status: poll while anything is still "Sending…".
  useEffect(() => {
    const anyPending = appointments.some((a) => a.notificationStatus === 'PENDING')
    if (!anyPending) return undefined
    const timer = setInterval(load, 3000)
    return () => clearInterval(timer)
  }, [appointments, load])

  const runCancel = async () => {
    const appointmentId = confirmTarget.id
    setCancelling(true)
    showToast('Initiating cancellation…', 'info')
    await pace(STEP_GAP_MS)

    try {
      await authedRequest(`/appointments/${appointmentId}/cancel`, { method: 'POST' })
    } catch (err) {
      setCancelling(false)
      setConfirmTarget(null)
      if (err instanceof ApiError && err.status !== 401) {
        showToast(err.message || 'Could not cancel.', 'error')
      }
      return
    }

    // Poll concurrently so the four toasts keep one even rhythm.
    const notificationResult = pollNotificationStatus(authedRequest, appointmentId)

    showToast('Appointment cancelled ✓', 'success')
    await pace(STEP_GAP_MS)
    showToast('Sending cancellation…', 'info')
    await pace(STEP_GAP_MS)

    const finalStatus = await notificationResult
    if (finalStatus === 'SENT') {
      showToast('Cancellation sent ✓', 'success')
    } else if (finalStatus === 'FAILED') {
      showToast('Cancellation notice could not be sent.', 'error')
    } else {
      showToast('Still sending cancellation…', 'info')
    }

    await pace(STEP_GAP_MS)
    setCancelling(false)
    setConfirmTarget(null)
    load()
  }

  if (loading) return <p className="text-sm text-slate-500">Loading…</p>

  return (
    <div>
      <h1 className="mb-6 text-2xl font-semibold text-slate-900">My appointments</h1>
      {appointments.length === 0 ? (
        <p className="text-sm text-slate-500">You have no appointments yet.</p>
      ) : (
        <ul className="space-y-3">
          {appointments.map((appt) => {
            const cancelled = appt.appointmentStatus === 'CANCELLED'
            const upcoming = new Date(appt.startTime) > new Date()
            return (
              <li key={appt.id} className="flex items-center justify-between rounded-lg border bg-white p-4">
                <div>
                  <p className="font-semibold text-slate-900">{appt.doctorName}</p>
                  <p className="text-sm text-slate-500">{appt.specialty}</p>
                  <p className="mt-1 text-sm text-slate-700">{formatDateTime(appt.startTime)}</p>
                  <p className="mt-1 text-xs text-slate-400">{notificationLabel(appt)}</p>
                </div>
                <div className="flex flex-col items-end gap-2">
                  <span
                    className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
                      cancelled ? 'bg-slate-100 text-slate-500' : 'bg-emerald-100 text-emerald-700'
                    }`}
                  >
                    {cancelled ? 'Cancelled' : 'Confirmed'}
                  </span>
                  {!cancelled && upcoming && (
                    <button
                      onClick={() => setConfirmTarget(appt)}
                      className="rounded-md border border-red-300 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50"
                    >
                      Cancel
                    </button>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <ConfirmModal
        open={!!confirmTarget}
        title="Cancel appointment?"
        message={
          confirmTarget
            ? `Cancel your appointment with ${confirmTarget.doctorName} on ${formatDateTime(confirmTarget.startTime)}? This frees the slot for others.`
            : ''
        }
        confirmLabel="Cancel appointment"
        busy={cancelling}
        onConfirm={runCancel}
        onCancel={() => setConfirmTarget(null)}
      />
    </div>
  )
}
