// Demo pacing. When VITE_DEMO_PACING=true, deliberate gaps are inserted between
// toast steps so the (otherwise instant on localhost) event-driven flow is visible.
export const DEMO_PACING = import.meta.env.VITE_DEMO_PACING === 'true'

/** Resolve after `ms` when pacing is on; resolve immediately when off. */
export function pace(ms) {
  return DEMO_PACING ? new Promise((resolve) => setTimeout(resolve, ms)) : Promise.resolve()
}
