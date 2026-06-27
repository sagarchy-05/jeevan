/**
 * Polls a single appointment until its notification status resolves (SENT/FAILED)
 * or the timeout elapses. Returns the resolved status, or 'PENDING' on timeout.
 */
export async function pollNotificationStatus(
  authedRequest,
  appointmentId,
  { timeoutMs = 15000, intervalMs = 1000 } = {},
) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
    try {
      const appt = await authedRequest(`/appointments/${appointmentId}`)
      if (appt.notificationStatus !== 'PENDING') return appt.notificationStatus
    } catch {
      /* keep polling */
    }
  }
  return 'PENDING'
}
