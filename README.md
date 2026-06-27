# Jeevan — Healthcare Appointment Platform

An event-driven appointment-booking platform built as independently deployable
services. A patient registers, browses doctors, books a 30-minute slot against a
doctor's weekly availability, and receives a confirmation notification.

**The architectural heart:** the core service commits a booking **synchronously and
transactionally** (returning `201`/`409` immediately), *then* publishes an event; a
separate worker reacts to that already-committed fact and notifies, publishing a
result the core uses to update the appointment's notification status. Booking
correctness never leaves the core — the worker only handles async side-effects.

```
                 REST (sync, transactional)
   Patient ───────────────────────────────────►  jeevan-core ──► PostgreSQL
   (jeevan-web)                                        │  ▲
                                                       │  │
                              appointment.booked /     │  │ notification.sent
                              appointment.cancelled /  ▼  │ (status SENT/FAILED)
                              verification.requested      │
                                          RabbitMQ ◄──────┘
                                             │
                                             ▼
                                       jeevan-notifier ──► MailHog (optional email)
```

## Services

| Service | Stack | Role |
|---------|-------|------|
| **jeevan-core** | Java 21, Spring Boot 3 | Source of truth — users, doctors, availability, appointments. Synchronous transactional booking; publishes events. |
| **jeevan-notifier** | Python 3.12, FastAPI | Async side-effects only — consumes appointment/verification events, sends notifications, publishes results. Owns no appointment data. |
| **jeevan-web** | Vite + React + Tailwind | Demo frontend. |
| **postgres** | PostgreSQL 16 | Database (no H2). |
| **rabbitmq** | RabbitMQ 3-management | Message broker. |
| **mailhog** | MailHog | Local SMTP catcher for the optional real-email path. |

## Quick start (one command)

Prerequisite: Docker + Docker Compose.

```bash
cp .env.example .env
docker compose up --build      # first run pulls Maven deps; give it a few minutes
```

| Surface | URL |
|---------|-----|
| Web app | http://localhost:5173 |
| Core API (Swagger UI) | http://localhost:8080/swagger-ui.html |
| Core OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Notifier (Swagger UI) | http://localhost:8000/docs |
| Notifier processed log | http://localhost:8000/notifications |
| RabbitMQ management | http://localhost:15672 (jeevan / jeevan) |
| MailHog inbox | http://localhost:8025 |

Tear down with `docker compose down` (add `-v` to also wipe the database volume).

## The patient flow

1. **Register / log in.**
2. **Doctors** — paginated grid (9/page), specialty filter, and a name/specialty
   search. Browsing is public; booking requires login.
3. **Book** — a calendar enables only the doctor's working days within 30 days;
   pick a day → pick a 30-minute slot → confirm. A live toast sequence shows the
   booking + notification round-trip, then redirects to **My Appointments**.
4. **My Appointments** — live notification status (Sending… → Sent ✓) and cancel.

## Configuration

All config is env-driven; copy `.env.example` to `.env` and adjust. Notable flags:

| Variable | Default | Effect |
|----------|---------|--------|
| `EMAIL_VERIFICATION_ENABLED` | `false` | When `true`, new users are created **unverified** and must verify their email before they can log in (§5a). |
| `NOTIFIER_EMAIL_ENABLED` | `false` | When `true`, the worker sends real emails via MailHog; otherwise it logs notifications. |
| `VITE_DEMO_PACING` | `true` | Adds small gaps between booking/cancel toast steps so the event-driven flow is visible on localhost. |
| `CLINIC_TIMEZONE` | `Asia/Kolkata` | Timezone availability is interpreted in (and emails are rendered in). |
| `SLOT_LENGTH_MINUTES` / `BOOKING_WINDOW_DAYS` | `30` / `30` | Slot size and how far ahead booking is open. |

To demo **email verification + real email**, set both `EMAIL_VERIFICATION_ENABLED=true`
and `NOTIFIER_EMAIL_ENABLED=true`, then `docker compose up -d --build --force-recreate
jeevan-core jeevan-notifier`. Register → verify via the link in MailHog → log in → book.

## Correctness: no double-booking

The most important logic (`AppointmentService.book`) has **two layers**:

1. A **partial unique index** — `UNIQUE (doctor_id, start_time) WHERE appointment_status
   <> 'CANCELLED'` — the authoritative, race-proof guarantee (cancelled rows free the slot).
2. A **pessimistic lock** on the doctor row in the booking transaction, so concurrent
   attempts serialize and the loser cleanly sees the committed booking.

A constraint violation is translated to `409 SLOT_ALREADY_BOOKED` — never a stack
trace. A focused Testcontainers test fires two concurrent bookings at one slot and
asserts exactly one wins. See [jeevan-core/docs/schema.md](jeevan-core/docs/schema.md).

## Errors

Every failure returns a consistent envelope with a stable, machine-readable code the
frontend branches on:

```json
{ "timestamp": "...", "status": 409, "error": "SLOT_ALREADY_BOOKED",
  "message": "That slot was just taken.", "path": "/api/appointments" }
```

## Tests

```bash
cd jeevan-core && mvn test     # needs Docker (Testcontainers spins up Postgres 16)
```
Covers the concurrency/duplicate-booking guarantee and the auth round-trip.

## Project structure

```
jeevan/
├── jeevan-core/          # Spring Boot — booking, source of truth   (see its README)
├── jeevan-notifier/      # FastAPI worker — async notifications      (see its README)
├── jeevan-web/           # Vite + React + Tailwind frontend          (see its README)
├── docker-compose.yml    # all six services, with health gating
└── .env.example          # copy to .env
```

## Out of scope (by decision)

H2 (Postgres throughout), a `slots` table (slots are generated dynamically), doctor
login / admin APIs, one-off availability exceptions, dead-letter/retry topologies,
third-party email (MailHog only), caching, and rate limiting.
