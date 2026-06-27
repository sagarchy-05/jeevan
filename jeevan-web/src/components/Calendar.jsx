import { useMemo, useState } from 'react'

const WEEKDAY_NAMES = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
]
const WEEKDAY_LABELS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa']

const startOfMonth = (d) => new Date(d.getFullYear(), d.getMonth(), 1)
const stripTime = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate())
const addMonths = (d, n) => new Date(d.getFullYear(), d.getMonth() + n, 1)
const sameDay = (a, b) =>
  a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()

function buildMonthCells(view) {
  const first = startOfMonth(view)
  const leadingBlanks = first.getDay()
  const daysInMonth = new Date(view.getFullYear(), view.getMonth() + 1, 0).getDate()
  const cells = []
  for (let i = 0; i < leadingBlanks; i += 1) cells.push(null)
  for (let day = 1; day <= daysInMonth; day += 1) {
    cells.push(new Date(view.getFullYear(), view.getMonth(), day))
  }
  return cells
}

/**
 * Month calendar that only enables days the doctor works, within [minDate, maxDate]
 * (today .. booking window). Past/out-of-range/non-working days are disabled.
 */
export default function Calendar({ workingDays, minDate, maxDate, selectedDate, onSelect }) {
  const [view, setView] = useState(() => startOfMonth(minDate))
  const cells = useMemo(() => buildMonthCells(view), [view])

  const min = stripTime(minDate)
  const max = stripTime(maxDate)
  const canPrev = startOfMonth(view) > startOfMonth(minDate)
  const canNext = startOfMonth(view) < startOfMonth(maxDate)

  const isEnabled = (d) =>
    d && d >= min && d <= max && workingDays.has(WEEKDAY_NAMES[d.getDay()])

  return (
    <div className="rounded-lg border bg-white p-4">
      <div className="mb-3 flex items-center justify-between">
        <button
          disabled={!canPrev}
          onClick={() => setView(addMonths(view, -1))}
          className="rounded px-2 py-1 text-slate-600 disabled:opacity-30"
        >
          ‹
        </button>
        <span className="text-sm font-medium text-slate-800">
          {view.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}
        </span>
        <button
          disabled={!canNext}
          onClick={() => setView(addMonths(view, 1))}
          className="rounded px-2 py-1 text-slate-600 disabled:opacity-30"
        >
          ›
        </button>
      </div>

      <div className="grid grid-cols-7 gap-1 text-center text-xs text-slate-400">
        {WEEKDAY_LABELS.map((label) => (
          <div key={label}>{label}</div>
        ))}
      </div>

      <div className="mt-1 grid grid-cols-7 gap-1">
        {cells.map((d, i) => {
          if (!d) return <div key={`blank-${i}`} />
          const enabled = isEnabled(d)
          const selected = selectedDate && sameDay(d, selectedDate)
          return (
            <button
              key={d.toISOString()}
              disabled={!enabled}
              onClick={() => onSelect(d)}
              className={`aspect-square rounded-md text-sm ${
                selected
                  ? 'bg-slate-900 text-white'
                  : enabled
                    ? 'text-slate-800 hover:bg-slate-100'
                    : 'cursor-not-allowed text-slate-300'
              }`}
            >
              {d.getDate()}
            </button>
          )
        })}
      </div>
    </div>
  )
}
