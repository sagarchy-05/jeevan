const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

/** Error carrying the backend's machine-readable code so the UI can branch on it. */
export class ApiError extends Error {
  constructor(status, code, message, fieldErrors) {
    super(message || 'Request failed')
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors || null
  }
}

/**
 * Thin fetch wrapper. Returns parsed JSON (or null for 204) and throws an
 * {@link ApiError} on any non-2xx or network failure, surfacing the error envelope's
 * `error` code and `fieldErrors`.
 */
export async function apiRequest(path, { method = 'GET', body, token } = {}) {
  let response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers: {
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Something went wrong, try again.')
  }

  if (response.status === 204) return null

  let data = null
  const text = await response.text()
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      /* non-JSON body */
    }
  }

  if (!response.ok) {
    throw new ApiError(
      response.status,
      data?.error || 'UNKNOWN',
      data?.message || 'Request failed',
      data?.fieldErrors,
    )
  }
  return data
}
