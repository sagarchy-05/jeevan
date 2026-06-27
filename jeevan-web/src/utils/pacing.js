// Demo pacing. Deliberate gaps between toast steps so the (otherwise instant on
// localhost) event-driven flow is visible. Defaults ON — this is a demo and the
// paced flow is the point; set VITE_DEMO_PACING=false to disable.
export const DEMO_PACING = import.meta.env.VITE_DEMO_PACING !== 'false'

// Uniform gap between consecutive toast steps.
export const STEP_GAP_MS = 800

/** Resolve after `ms` when pacing is on; resolve immediately when off. */
export function pace(ms) {
  return DEMO_PACING ? new Promise((resolve) => setTimeout(resolve, ms)) : Promise.resolve()
}
