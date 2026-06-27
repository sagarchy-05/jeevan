import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiRequest } from '../api/client'
import Pagination from '../components/Pagination'
import { useToast } from '../context/ToastContext'

const PAGE_SIZE = 9

export default function DoctorsPage() {
  const [specialties, setSpecialties] = useState([])
  const [specialty, setSpecialty] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [loading, setLoading] = useState(true)

  const { showToast } = useToast()
  const navigate = useNavigate()

  useEffect(() => {
    apiRequest('/doctors/specialties')
      .then(setSpecialties)
      .catch(() => {
        /* non-critical */
      })
  }, [])

  // Debounce the search box so we don't fire a request per keystroke.
  useEffect(() => {
    const timer = setTimeout(() => {
      setSearch(searchInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [searchInput])

  useEffect(() => {
    setLoading(true)
    const query = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
    if (specialty) query.set('specialty', specialty)
    if (search) query.set('search', search)
    apiRequest(`/doctors?${query.toString()}`)
      .then(setData)
      .catch(() => showToast('Could not load doctors.', 'error'))
      .finally(() => setLoading(false))
  }, [page, specialty, search, showToast])

  const onSpecialtyChange = (value) => {
    setSpecialty(value)
    setPage(0)
  }

  return (
    <div>
      <h1 className="mb-4 text-2xl font-semibold text-slate-900">Find a doctor</h1>
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center">
        <input
          type="search"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          placeholder="Search by doctor name or specialty…"
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-200 sm:flex-1"
        />
        <select
          value={specialty}
          onChange={(e) => onSpecialtyChange(e.target.value)}
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        >
          <option value="">All specialties</option>
          {specialties.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <p className="text-sm text-slate-500">Loading…</p>
      ) : data.content.length === 0 ? (
        <p className="text-sm text-slate-500">No doctors found.</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.content.map((doctor) => (
            <div key={doctor.id} className="flex flex-col rounded-lg border bg-white p-4">
              <h2 className="font-semibold text-slate-900">{doctor.name}</h2>
              <p className="text-sm font-medium text-slate-500">{doctor.specialty}</p>
              <p className="mt-2 flex-1 text-sm text-slate-600">{doctor.bio}</p>
              <button
                onClick={() => navigate(`/doctors/${doctor.id}/book`, { state: { doctor } })}
                className="mt-4 rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
              >
                Book appointment
              </button>
            </div>
          ))}
        </div>
      )}

      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  )
}
