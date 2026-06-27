import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { ApiError, apiRequest } from '../api/client'
import Calendar from '../components/Calendar'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { toDateParam } from '../utils/datetime'

const NOTIFICATION_POLL_TIMEOUT_MS = 15000
const NOTIFICATION_POLL_INTERVAL_MS = 1000

export default function BookingPage() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { authedRequest } = useAuth()
  const { showToast } = useToast()

  const [availability, setAvailability] = useState(null)
  const [selectedDate, setSelectedDate] = useState(null)
  const [slots, setSlots] = useState([])
  const [selectedSlot, setSelectedSlot] = useState(null)
  const [loadingSlots, setLoadingSlots] = useState(false)
  const [processing, setProcessing] = useState(false)

  const today = useMemo(() => new Date(), [])
  const maxDate = useMemo(() => {
    const d = new Date()
    d.setDate(d.getDate() + 30)
    return d
  }, [])
  const workingDays = useMemo(
    () => new Set((availability?.windows || []).map((w) => w.dayOfWeek)),
    [availability],
  )
  const doctorName = availability?.doctorName || location.state?.doctor?.name || 'this doctor'

  useEffect(() => {
    apiRequest(`/doctors/${id}/availability`)
      .then(setAvailability)
      .catch(() => showToast('Could not load availability.', 'error'))
  }, [id, showToast])

  const loadSlots = async (date) => {
    setLoadingSlots(true)
    setSelectedSlot(null)
    try {
      const res = await apiRequest(`/doctors/${id}/slots?date=${toDateParam(date)}`)
      setSlots(res.slots || [])
    } catch {
      showToast('Could not load slots.', 'error')
      setSlots([])
    } finally {
      setLoadingSlots(false)
    }
  }

  const onSelectDate = (date) => {
    setSelectedDate(date)
    loadSlots(date)
  }

  /** Poll the appointment until its notification resolves (or we time out). */
  const pollNotification = async (appointmentId) => {
    const deadline = Date.now() + NOTIFICATION_POLL_TIMEOUT_MS
    while (Date.now() < deadline) {
      await new Promise((resolve) => setTimeout(resolve, NOTIFICATION_POLL_INTERVAL_MS))
      try {
        const appt = await authedRequest(`/appointments/${appointmentId}`)
        if (appt.notificationStatus !== 'PENDING') return appt.notificationStatus
      } catch {
        /* keep polling */
      }
    }
    return 'PENDING'
  }

  const onBook = async () => {
    if (!selectedSlot) return
    setProcessing(true)
    showToast('Initiating booking…', 'info')

    let appointment
    try {
      appointment = await authedRequest('/appointments', {
        method: 'POST',
        body: { doctorId: Number(id), startTime: selectedSlot.start },
      })
    } catch (err) {
      setProcessing(false)
      if (err instanceof ApiError && err.code === 'SLOT_ALREADY_BOOKED') {
        showToast('That slot was just taken — pick another.', 'error')
        if (selectedDate) loadSlots(selectedDate)
      } else if (err instanceof ApiError && err.code === 'EMAIL_NOT_VERIFIED') {
        showToast('Please verify your email before booking.', 'error')
      } else if (err instanceof ApiError && err.status === 401) {
        navigate('/login')
      } else {
        showToast(err.message || 'Booking failed. Try again.', 'error')
      }
      return
    }

    showToast('Booking confirmed ✓', 'success')
    showToast('Sending confirmation…', 'info')

    const finalStatus = await pollNotification(appointment.id)
    if (finalStatus === 'SENT') {
      showToast('Confirmation sent ✓', 'success')
    } else if (finalStatus === 'FAILED') {
      showToast('Confirmation could not be sent.', 'error')
    } else {
      showToast('Still sending confirmation — see My Appointments.', 'info')
    }

    setProcessing(false)
    navigate('/appointments')
  }

  return (
    <div>
      <button
        onClick={() => navigate('/doctors')}
        className="mb-4 text-sm text-slate-600 hover:text-slate-900"
      >
        ← Back to doctors
      </button>
      <h1 className="mb-6 text-2xl font-semibold text-slate-900">Book with {doctorName}</h1>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div>
          <h2 className="mb-2 text-sm font-medium text-slate-700">Pick a day</h2>
          <Calendar
            workingDays={workingDays}
            minDate={today}
            maxDate={maxDate}
            selectedDate={selectedDate}
            onSelect={onSelectDate}
          />
          <p className="mt-2 text-xs text-slate-400">Only days this doctor works are selectable.</p>
        </div>

        <div>
          <h2 className="mb-2 text-sm font-medium text-slate-700">Available times</h2>
          {!selectedDate ? (
            <p className="text-sm text-slate-500">Select a day to see open slots.</p>
          ) : loadingSlots ? (
            <p className="text-sm text-slate-500">Loading slots…</p>
          ) : slots.length === 0 ? (
            <p className="text-sm text-slate-500">No open slots on this day.</p>
          ) : (
            <>
              <div className="grid grid-cols-3 gap-2">
                {slots.map((slot) => {
                  const selected = selectedSlot?.start === slot.start
                  return (
                    <button
                      key={slot.start}
                      disabled={processing}
                      onClick={() => setSelectedSlot(slot)}
                      className={`rounded-md border px-2 py-2 text-sm disabled:opacity-50 ${
                        selected
                          ? 'border-slate-900 bg-slate-900 text-white'
                          : 'border-slate-300 hover:border-slate-900'
                      }`}
                    >
                      {slot.label}
                    </button>
                  )
                })}
              </div>

              <button
                disabled={!selectedSlot || processing}
                onClick={onBook}
                className="mt-4 w-full rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
              >
                {processing
                  ? 'Booking…'
                  : selectedSlot
                    ? `Book ${selectedSlot.label}`
                    : 'Select a time to book'}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
