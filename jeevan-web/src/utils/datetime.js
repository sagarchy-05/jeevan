// Matches the backend clinic timezone so displayed times line up with the slots.
const CLINIC_TZ = 'Asia/Kolkata'

/** Format a UTC ISO instant for display in the clinic timezone. */
export function formatDateTime(iso) {
  return new Intl.DateTimeFormat('en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: CLINIC_TZ,
  }).format(new Date(iso))
}

/** A JS Date -> 'YYYY-MM-DD' using local calendar components (for the slots query). */
export function toDateParam(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
