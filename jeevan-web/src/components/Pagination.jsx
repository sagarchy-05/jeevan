export default function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null
  return (
    <div className="mt-6 flex items-center justify-center gap-3">
      <button
        disabled={page <= 0}
        onClick={() => onChange(page - 1)}
        className="rounded-md border px-3 py-1.5 text-sm disabled:opacity-40"
      >
        Previous
      </button>
      <span className="text-sm text-slate-600">
        Page {page + 1} of {totalPages}
      </span>
      <button
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
        className="rounded-md border px-3 py-1.5 text-sm disabled:opacity-40"
      >
        Next
      </button>
    </div>
  )
}
