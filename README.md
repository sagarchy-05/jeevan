# Jeevan — Healthcare Appointment Platform

Event-driven appointment booking built as independently deployable services:

- **jeevan-core** — Spring Boot. Source of truth; owns users, doctors, availability, and appointments. Books **synchronously and transactionally**, then publishes events.
- **jeevan-notifier** — Python FastAPI worker. Async notifications only; reacts to already-committed bookings.
- **jeevan-web** — Vite + React + Tailwind demo frontend.
- **Postgres 16** — database. **RabbitMQ 3** — message broker. **MailHog** — local SMTP catcher.

The architectural seam: the core commits a booking and returns `201`/`409` synchronously, *then* publishes `appointment.booked`; the worker notifies and publishes `notification.sent` back; the core flips the appointment's notification status. Booking correctness never leaves the core.

## Repository layout

```
jeevan/
├── jeevan-core/          # Spring Boot service
├── jeevan-notifier/      # Python FastAPI worker
├── jeevan-web/           # Vite + React + Tailwind frontend
├── docker-compose.yml    # infra now; full stack later
└── .env.example          # copy to .env
```

## Quick start (infrastructure)

> Application services are wired into compose in a later build step. For now this boots the infra dependencies.

```bash
cp .env.example .env
docker-compose up -d postgres rabbitmq mailhog
```

| Service   | URL / Port                        |
|-----------|-----------------------------------|
| Postgres  | `localhost:5432`                  |
| RabbitMQ  | AMQP `localhost:5672`             |
| RabbitMQ management UI | http://localhost:15672 |
| MailHog UI | http://localhost:8025            |

Credentials and topology come from `.env` (see `.env.example`).

_This README is expanded with full setup, API, and schema docs in the final build step._
