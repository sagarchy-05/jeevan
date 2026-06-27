# jeevan-web

Vite + React + Tailwind frontend — a clean demo harness for the Jeevan workflows.
Minimal styling, no heavy component library; the JWT is stored in `localStorage`.

## Run

```bash
npm install
npm run dev        # http://localhost:5173 (needs the core API running)
npm run build      # production build (what the Docker image serves via nginx)
```

The root `docker compose up` builds this into an **nginx** image serving the static
build with SPA routing.

## Pages

- **Login / Register** — inline field-error handling; register auto-logs-in on the
  log-only path, or routes to "verify your email" when verification is enabled.
- **Doctors** (public) — paginated grid, specialty filter, name/specialty search
  (debounced, server-side).
- **Booking** — month calendar enabling only the doctor's working days within 30 days;
  pick a slot, confirm, and watch a paced toast sequence of the booking + notification
  round-trip.
- **My Appointments** — live notification status (polled while pending) and cancel
  (with a confirmation modal + the same toast sequence).
- **Verify** (`/verify`) — consumes the email token; offers resend-by-email when invalid.

## Configuration (build-time `VITE_*`)

Read from the repo-root `.env` (Vite `envDir` points there). Only `VITE_*` vars reach
the bundle.

| Variable | Default | Effect |
|----------|---------|--------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Core API base URL |
| `VITE_DEMO_PACING` | `true` | Insert small gaps between toast steps so the flow is visible; `false` = instant |

In Docker these are baked at build time via build args; for local `npm run dev` they're
read from `.env` at startup (restart the dev server after changing them).

## Error handling

A shared `ApiError(code)` drives the UI: `401`/`TOKEN_EXPIRED` → clear token + redirect
to login; `SLOT_ALREADY_BOOKED` → toast + auto-refresh slots; `VALIDATION_FAILED` →
inline field errors; `EMAIL_NOT_VERIFIED` (at login) → verify prompt + resend form;
network/5xx → toast.

## Layout

```
src/
├── api/client.js          # fetch wrapper + ApiError
├── context/               # AuthContext, ToastContext (stacked animated toasts)
├── components/            # Layout, Calendar, ConfirmModal, Field, Pagination, …
├── pages/                 # Login, Register, Doctors, Booking, Appointments, Verify
└── utils/                 # datetime, pacing, notifications (poll)
```
