# notification-service

In-app notifications, with an optional best-effort email side channel. Owns
one entity, no relationships to anything else — every other service reaches
it only through its internal API after something happens (a booking
confirmed, a review posted, etc).

## Port

`8086`

## Database

`travel_notification_db` — a single `notification` table.

## Two APIs, two audiences

| API | Path | Who calls it | How it's reached |
|---|---|---|---|
| End-user inbox | `/notifications/**` | The logged-in user, via the frontend | Through the API Gateway, JWT-authenticated |
| Internal creation | `/internal/notifications` | Other services (hotel-service, etc) | **Not** gateway-routed — direct service-to-service call via Eureka, guarded by `X-Internal-Api-Key` |

The gateway's route config only maps `/notifications/**`. `/internal/**` is
deliberately absent from it, so it's unreachable from outside the Docker
network no matter what a client sends — the only thing standing between an
attacker and that endpoint if they *are* on the network is the shared
`X-Internal-Api-Key` header (see `InternalApiKeyFilter`). That's a
reasonable tradeoff for a single-Docker-network deployment; it is **not**
a substitute for mTLS or a service mesh in a real multi-tenant environment.

## Domain

```
Notification
  id, userId, type, title, message, referenceId,
  read, readAt,
  emailStatus (NOT_REQUESTED / PENDING / SENT / FAILED),
  createdAt
```

`referenceId` is an optional, untyped UUID pointer back to whatever this
notification is about (a booking ID, a review ID...) so the frontend can
deep-link "View booking" without parsing `message`. No FK — the referenced
row usually lives in a different service's database entirely.

`emailStatus` tracks the email channel independently of the notification
itself. A `Notification` always exists in-app the instant it's created;
email is optional and asynchronous, and can fail without touching the
in-app record.

## APIs

**End-user** (`/notifications/**`, JWT via gateway)

| Method | Path | Description |
|---|---|---|
| GET | `/notifications/me` | My notifications, newest first. |
| GET | `/notifications/me/unread-count` | For a badge/counter in the UI. |
| PATCH | `/notifications/{id}/read` | Mark one as read. 403 if you're not the recipient. |
| PATCH | `/notifications/read-all` | Mark everything unread as read. |

**Internal** (`/internal/notifications`, `X-Internal-Api-Key`)

| Method | Path | Description |
|---|---|---|
| POST | `/internal/notifications` | Create a notification for a user. |

```json
POST /internal/notifications
X-Internal-Api-Key: local-dev-internal-key
{
  "userId": "….",
  "type": "BOOKING_CONFIRMED",
  "title": "Hotel booking confirmed",
  "message": "Your booking at Taj Exotica Resort is confirmed.",
  "referenceId": "….",          // the HotelBooking's id, e.g.
  "sendEmail": true,
  "recipientEmail": "traveler@example.com"
}
```

`recipientEmail` is supplied by the caller, not looked up here —
notification-service has no `User` entity and never calls auth-service.
Callers already have the email on hand from the JWT claims forwarded by the
gateway (`X-Authenticated-Email`), so there's no need for a second lookup.
Omit it (or leave `sendEmail: false`) for an in-app-only notification.

## Email delivery

- `spring-boot-starter-mail` + `JavaMailSender`, dispatched `@Async` from
  `EmailDispatchServiceImpl` — runs off the request thread that created the
  notification, so a slow or unreachable SMTP server never blocks the
  caller (e.g. a hotel booking).
- Any failure (bad address, SMTP down, whatever) is caught, logged, and
  recorded as `emailStatus=FAILED`. It never propagates back to the caller
  and never affects the in-app notification, which already exists.
- Locally, points at **Mailhog** (`docker-compose.yml`) — a fake SMTP
  server with a web UI at `http://localhost:8025` where you can see every
  email that would have been sent, without a real provider or credentials.
- `notification.email.enabled=false` disables the channel entirely
  (used in tests) without touching code.

## Running locally

```bash
mvn spring-boot:run
```

Needs `travel_notification_db` to exist and Mailhog (or any SMTP server) reachable
at `spring.mail.host`/`spring.mail.port` if you want to see email actually attempt
to send — otherwise it'll just fail and log, harmlessly.

## Swagger UI

http://localhost:8086/swagger-ui/index.html

## Known simplifications

- **No notification preferences.** Every event that calls the internal API
  creates a notification — there's no per-user "mute booking confirmations"
  setting yet. Add a `NotificationPreference` table keyed by `(userId, type)`
  if that's ever needed; the internal API's contract doesn't need to change,
  only whether `createNotification` short-circuits.
- **No retry queue for failed emails.** A `FAILED` email just sits there.
  A real system would put failed sends on a retry queue (or fall back to a
  message broker instead of a direct REST call) rather than dropping them.
- **The internal API has no rate limiting or payload size caps.** Fine for
  a handful of trusted internal callers; would need hardening if this ever
  became a public-facing ingestion endpoint.
