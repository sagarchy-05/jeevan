import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { ApiError, apiRequest } from '../api/client'
import Calendar from '../components/Calendar'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { toDateParam } from '../utils/datetime'

export default function BookingPage() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { authedRequest } = useAuth()
  const { showToast } = useToast()

  const [availability, setAvailability] = useState(null)
  const [selectedDate, setSelectedDate] = useState(null)
  const [slots, setSlots] = useState([])
  const [loadingSlots, setLoadingSlots] = useState(false)
  const [booking, setBooking] = useState(false)

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

  const onBook = async (slot) => {
    setBooking(true)
    try {
      await authedRequest('/appointments', {
        method: 'POST',
        body: { doctorId: Number(id), startTime: slot.start },
      })
      showToast('Appointment confirmed ✓', 'success')
      navigate('/appointments')
    } catch (err) {
      if (err instanceof ApiError && err.code === 'SLOT_ALREADY_BOOKED') {
        showToast('That slot was just taken — pick another.', 'error')
        if (selectedDate) loadSlots(selectedDate)
      } else if (err instanceof ApiError && err.code === 'EMAIL_NOT_VERIFIED') {
        showToast('Please verify your email before booking.', 'error')
      } else if (err instanceof ApiError && err.status === 401) {
        navigate('/login')
      } else {
        showToast(err.message || 'Could not book. Try again.', 'error')
      }
    } finally {
      setBooking(false)
    }
  }

  return (
    <div>
      <button onClick={() => navigate('/doctors')} className="mb-4 text-sm text-slate-600 hover:text-slate-900">
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
            <div className="grid grid-cols-3 gap-2">
              {slots.map((slot) => (
                <button
                  key={slot.start}
                  disabled={booking}
                  onClick={() => onBook(slot)}
                  className="rounded-md border border-slate-300 px-2 py-2 text-sm hover:border-slate-900 hover:bg-slate-900 hover:text-white disabled:opacity-50"
                >
                  {slot.label}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
