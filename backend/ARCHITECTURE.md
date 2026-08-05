# Travel Platform — Microservices Architecture

## Service Map

```
                           ┌─────────────────────────────────┐
                           │         Frontend (Vite)          │
                           │         localhost:5173           │
                           └────────────────┬────────────────┘
                                            │ HTTP
                                            ▼
                           ┌─────────────────────────────────┐
                           │          API Gateway             │
                           │          port 8080               │
                           │                                  │
                           │  • JWT validation (global)       │
                           │  • Route by path prefix          │
                           │  • CORS                          │
                           │  • Adds X-Authenticated-*        │
                           │    headers downstream            │
                           └──┬──────────────┬───────────────┘
                              │              │
               /auth/**       │              │  /api/**, /admin/**
                              ▼              ▼
          ┌──────────────────────┐   ┌───────────────────────────┐
          │     auth-service     │   │   bus-booking-service     │
          │     port 8081        │   │   port 8082               │
          │                      │   │                           │
          │  • Register / Login  │   │  • Bus CRUD (admin)       │
          │  • Refresh token     │   │  • Driver CRUD (admin)    │
          │  • Logout            │   │  • Search buses           │
          │  • Issues JWT        │   │  • Book tickets           │
          │                      │   │  • Booking history        │
          │  DB: travel_auth_db  │   │  DB: travel_bus_booking_db│
          └──────────────────────┘   └───────────────────────────┘
                      │                          │
                      │              ┌───────────────────────────┐
                      │              │   ride-share-service      │
                      │              │   port 8083                │
                      │              │                            │
                      │              │  • Create/update/cancel   │
                      │              │    ride (any user)        │
                      │              │  • Search rides            │
                      │              │  • Book seats (PENDING)   │
                      │              │  • Driver approve/reject  │
                      │              │  • Cancel booking          │
                      │              │                            │
                      │              │  DB: travel_rideshare_db  │
                      │              └───────────────────────────┘
                      │                          │
                      │              ┌───────────────────────────┐
                      │              │  travel-packages-service  │
                      │              │  port 8084                │
                      │              │                            │
                      │              │  Module: travelpackage    │
                      │              │  • Admin: create/update/  │
                      │              │    delist packages         │
                      │              │  • Browse/search packages │
                      │              │  • Book (auto-confirm)    │
                      │              │  • Cancel booking          │
                      │              │                            │
                      │              │  Module: destination      │
                      │              │  • Admin: create/update/  │
                      │              │    delist destinations    │
                      │              │  • Browse/search/filter   │
                      │              │  • Popular destinations    │
                      │              │                            │
                      │              │  DB: travel_packages_db   │
                      │              └───────────────────────────┘
                      │                          │
                      │              ┌───────────────────────────┐
                      │              │      hotel-service        │
                      │              │      port 8085             │
                      │              │                            │
                      │              │  • Admin: hotels/rooms/    │
                      │              │    images/amenities        │
                      │              │  • Browse/search hotels    │
                      │              │  • Book (auto-confirm)     │
                      │              │  • Cancel booking          │
                      │              │  • Reviews (1 per user)    │
                      │              │                            │
                      │              │  References destinationId  │
                      │              │  and userId by UUID only — │
                      │              │  no shared DB, no JPA FK   │
                      │              │  across services.          │
                      │              │                            │
                      │              │  DB: travel_hotel_db      │
                      │              └───────────────────────────┘
                      │                          │
                      └──────────┬───────────────┘
                                 │ registers with
                                 ▼
                    ┌────────────────────────┐
                    │    service-registry    │
                    │    (Eureka)  port 8761 │
                    └────────────────────────┘
                                 ▲
                                 │ registers with
                                 │
          ┌──────────────────────────────────────┐
          │         notification-service           │
          │         port 8086                      │
          │                                        │
          │  /notifications/**  (gateway-routed,   │
          │    JWT)  — end-user inbox: list, mark  │
          │    read, unread count                  │
          │                                        │
          │  /internal/notifications  (NOT gateway-│
          │    routed — called directly by other   │
          │    services via Eureka, guarded by     │
          │    X-Internal-Api-Key)                 │
          │                                        │
          │  In-app always; email is @Async +      │
          │  best-effort via Mailhog locally.       │
          │                                        │
          │  DB: travel_notification_db            │
          └──────────────────────────────────────┘
                                 ▲
                                 │ direct call, bypasses gateway
                                 │ (see NotificationClient in each service)
                     hotel-service, bus-booking-service,
                     ride-share-service, travel-packages-service

── Future Services (add a folder + pom module + gateway route) ──────────────

  restaurant-service   /restaurants/**   — table reservations by city/cuisine
  payment-service       — introduced once hotel/bus/ride/package bookings
                           need real payment instead of auto-confirm
```

---

## How JWT flows across services

```
1. Client calls POST /auth/Loginin  →  API Gateway (no token needed)
2. Gateway routes to auth-service   →  auth-service issues JWT + refresh token
3. Client stores both tokens

4. Client calls GET /api/user/searchbusses/...
   Authorization: Bearer <jwt>
   → API Gateway validates JWT signature + expiry
   → Adds headers: X-Authenticated-Email, X-Authenticated-Authorities
   → Routes to bus-booking-service
   → bus-booking-service reads those headers — no second JWT parse needed

5. JWT expires (15 min) → client calls POST /auth/refresh-token
   → auth-service rotates refresh token, issues new JWT
   → client retries with new JWT
```

**Key principle:** JWT is validated **once** at the gateway. Downstream services
trust the `X-Authenticated-*` headers rather than re-validating the signature.
This means only the gateway and auth-service need the JWT secret.

**Important:** because bus-booking-service trusts these headers unconditionally,
it must never be reachable directly from outside the cluster — only the gateway
should have network access to it. In Kubernetes this is enforced with a
NetworkPolicy; locally, just don't expose port 8082 publicly. If a client could
reach bus-booking-service directly, they could forge `X-Authenticated-Email: admin@x.com`
and `X-Authenticated-Authorities: ROLE_ADMIN` themselves.

---

## Database isolation

Each service owns its own PostgreSQL database (originally MySQL — switched
to Postgres when Docker support was added; no service used MySQL-specific
SQL syntax, so the migration was a dependency + dialect + JDBC URL change
only, no entity/query rewrites):

| Service | Database |
|---------|----------|
| auth-service | `travel_auth_db` |
| bus-booking-service | `travel_bus_booking_db` |
| ride-share-service | `travel_rideshare_db` |
| travel-packages-service (packages + destinations) | `travel_packages_db` |
| hotel-service | `travel_hotel_db` |
| notification-service | `travel_notification_db` |

`travel_packages_db` holds both modules' tables — `travel_package`,
`package_itinerary`, `package_booking` for the packages side, and
`destination`, `attraction`, `destination_activity` for the destination
side — since they're one Spring Boot application, not two services
talking over a network. See "Why destination discovery lives inside
travel-packages-service" below for the reasoning.

bus-booking-service, ride-share-service, and travel-packages-service each
hold a `user_ref` table (UUID + email only) to maintain FK relationships on
booking records, kept in sync via the JWT claims on each request. None of
them stores passwords, roles, or any auth data — that lives exclusively in
`travel_auth_db`.

hotel-service deliberately skips `user_ref`: `HotelBooking.userId` and
`HotelReview.userId` are plain UUID columns with no local FK, populated
straight from the `X-Authenticated-User-Id` header the gateway forwards.
No email caching, no `findOrCreate` step. This is a valid alternative to
the `user_ref` pattern when a service never needs to *display* a cached
user email locally — if that need shows up later, add `user_ref` the same
way travel-packages-service does.

notification-service goes a step further: it has no `user_ref` and never
touches auth-service at all. `Notification.userId` is a plain UUID, and if
email delivery needs an address, the *caller* supplies it directly
(`recipientEmail` on the internal API) — usually lifted straight from that
caller's own `X-Authenticated-Email` header on the request that triggered
the notification. See "notification-service design notes" below.

---

## Startup order

Always start in this order:

```
1. service-registry        (Eureka must be up before anyone registers)
2. mailhog                 (no dependency on anything — start any time before notification-service)
3. auth-service
4. bus-booking-service
5. ride-share-service
6. travel-packages-service
7. notification-service    (hotel-service calls it directly — see below)
8. hotel-service            (depends on notification-service being reachable, though
                              booking still succeeds even if it isn't — see NotificationClientImpl)
9. api-gateway              (needs Eureka entries to exist before routing; start last)
```

`docker-compose.yml` encodes this via `depends_on: condition: service_healthy`,
not just container-start order — see "Docker setup" below.

---

## Adding a new microservice

1. Create `travel-platform/<service-name>/` with its own `pom.xml`.
2. Add `<module><service-name></module>` to the parent `pom.xml`.
3. Add a route block to `api-gateway/src/main/resources/application.yml`
   (skip this if the service should *not* be reachable from outside the
   Docker network — see notification-service's `/internal/**`).
4. Add `@EnableDiscoveryClient` to the new service's `@SpringBootApplication` class.
5. Point it at its own database in `application.properties`, and add a
   `CREATE DATABASE` line to `docker/postgres-init/01-create-databases.sql`.
6. Write a `Dockerfile` (copy an existing service's as a template), add a
   service block to `docker-compose.yml`, and wire up `depends_on`/
   `healthcheck` for anything that talks to it at startup.
7. **Add `COPY <service-name>/pom.xml <service-name>/pom.xml` to every
   *other* service's `Dockerfile`, including the new one's own copy of
   every sibling's pom.** This is easy to miss: because this is a single
   multi-module Maven reactor, `mvn -pl <module> -am` needs *all* the
   module poms declared in the parent to exist on disk to resolve the
   reactor, even though it only compiles the one module's source. Forget
   this step and every other service's Docker build breaks with a
   "child module does not exist" error the next time someone does a clean
   build — it won't show up if you're just running `mvn spring-boot:run`
   locally with an already-resolved `.m2` cache, only in Docker.

That's it — the gateway, Eureka, and JWT auth infrastructure are already in place.

---

## Future service sketches

### restaurant-service  (`/restaurants/**`)
- `GET /restaurants` — list restaurants by city/cuisine
- `POST /restaurants/{id}/reserve` — make a table reservation

(The hotel half of the original "hostel-restaurant-service" sketch is now
implemented as `hotel-service` — see its design notes below and its own
`README.md`.)

### payment-service
Once built, hotel-service, bus-booking-service, ride-share-service, and
travel-packages-service should each evolve their booking-creation flow to
start `PENDING` and only flip to `CONFIRMED` on a successful payment
webhook/callback, instead of auto-confirming immediately.

---

## ride-share-service design notes

Implemented as a standalone service (not folded into bus-booking-service)
because the domain is fundamentally different: bus-booking is operator-owned
fixed inventory, ride-share is peer-to-peer where any user can be a driver on
one ride and a passenger on another in the same session.

**No `ROLE_DRIVER`.** "Driver" is not a role — it's just "the `UserRef`
referenced by `Ride.driver`." Every authenticated `ROLE_USER` can post a ride
and separately book seats on other people's rides. This avoids the awkwardness
of a user needing two roles or a role-switch flow.

**Booking lifecycle:**
```
Passenger books seats  →  RideBooking created as PENDING
                           (seats NOT yet deducted from Ride.availableSeats)
Driver approves         →  status → APPROVED, seats deducted from availableSeats
Driver rejects           →  status → REJECTED, no seat change (none were taken)
Passenger cancels         →  status → CANCELLED
                           (if was APPROVED, seats are returned to availableSeats)
```

Seats are intentionally **not** deducted at PENDING time. This means
`availableSeats` reflects only confirmed (APPROVED) bookings, and a popular
ride can accumulate more PENDING requests than physical seats — the driver
sees all of them and decides who to approve. `approveBooking` re-checks
availability at approval time and throws `InsufficientSeatsException` if
seats were consumed by a concurrent approval first.

**Authorization is enforced in the service layer, not assumed from URL
structure.** `updateRide`, `cancelRide`, `approveBooking`, `rejectBooking`,
and `cancelBooking` all verify the caller owns the relevant ride/booking
before acting. `getBookingsByRide` also checks driver ownership — without
this check, any authenticated user could view another driver's passenger
list (names, emails) just by guessing a ride UUID.

---

## travel-packages-service design notes

Unlike ride-share-service, packages are **admin-curated inventory** — closer
in shape to bus-booking-service than to the peer-to-peer ride model. `ROLE_ADMIN`
creates/updates/delists packages; any authenticated user (the "books for"
side) browses and books them. There's no "package owner" role to check
against — ownership checks only apply to bookings (only the traveler who
made a booking can cancel it).

**Booking auto-confirms, no approval step.** Unlike ride-share's
PENDING→APPROVED flow, `PackageBooking.status` only has `CONFIRMED`/`CANCELLED`.
Slots are deducted from `TravelPackage.availableSlots` immediately on booking,
not held back until a separate approval — there's no driver-equivalent who
needs to review each request before it's final.

**Why `@ElementCollection` was deliberately avoided** for `inclusions`,
`exclusions`, `placesCovered`, `activities`, and `imageUrls`: it creates
anonymous join tables with no row ID, and Hibernate rewrites the *entire*
collection on every update (delete-all-then-reinsert), even for a one-item
change. Since these are flat, unordered string lists with no sub-structure
and no query requirement ("find packages including scuba diving" isn't a
current need), a `StringListConverter` (`AttributeConverter<List<String>, String>`
backed by Jackson) stores each as a single JSON column — one row write per
update, same `List<String>` API on the entity, and the DTO/API contract is
identical either way if a real entity is needed later.

**Itinerary is the one field promoted to a real entity.** `PackageItinerary`
has actual structure (`dayNumber` ordering + variable-length text per day)
that the flat-list fields don't, and is something you'd plausibly want to
edit "day 3" of independently later — a `@OneToMany` with
`cascade = CascadeType.ALL, orphanRemoval = true` handles full itinerary
replacement on update cleanly.

**`active` is a soft-delete flag, not a deletion.** `deletePackage()` sets
`active = false` rather than removing the row, preserving `PackageBooking`
FK integrity and booking history for travelers who already booked a package
that's since been delisted.

---

## hotel-service design notes

**Fully independent bounded context — no shared database, no cross-service
JPA relationship.** Hotel Service owns `Hotel`, `HotelImage`, `HotelAmenity`,
`Room`, `RoomImage`, `RoomAmenity`, `HotelBooking`, and `HotelReview`. It does
not have `Destination` or `User` entities; it stores `destinationId` and
`userId` as plain UUID columns, resolved by calling travel-packages-service
or auth-service only if a caller ever needs the full destination/user record
(neither call happens today — the UUIDs are enough for booking and display
via the frontend's own lookups).

**Hotels are bookable directly — no dependency on travel packages.**
`POST /hotel-bookings` never touches `travel-packages-service`. A
`TravelPackage` *may* optionally reference a `hotelId` later if the business
wants "packages that include this hotel," but that's a one-way reference
from packages → hotels, never the other way.

**Images and amenities are child entities, not JSON columns.** Unlike
`TravelPackage.inclusions`/`exclusions` (JSON via `StringListConverter`),
`HotelImage`/`HotelAmenity`/`RoomImage`/`RoomAmenity` are real `@OneToMany`
rows. Reasoning: images need a stable ID to delete individually
(`DELETE /hotel-images/{id}` was in the original design sketch) and
`displayOrder`; a JSON array makes single-item deletion and reordering
Java-side round-trips instead of one SQL statement.

**`Room.availableRooms` is a single counter, not a date calendar.** It's
decremented on booking creation and restored on cancellation — the same
shape as `TravelPackage.availableSlots`. This means two bookings for
non-overlapping date ranges still compete for the same pool of rooms of that
type, which is a real limitation for a production hotel system (a legitimate
January booking and a legitimate March booking shouldn't fight over the same
counter). It was a deliberate scope cut to keep the first version simple and
consistent with the rest of the platform's booking services, all of which
use simple counters rather than date-indexed availability. If double-booking
protection across date ranges becomes a real requirement, replace the
counter with a table keyed by `(roomId, date)` or an interval-overlap query
against existing bookings — the DTOs and controller contracts don't need to
change, only `HotelBookingServiceImpl`'s availability check.

**No payments yet, by design.** Bookings auto-confirm
(`bookingStatus=CONFIRMED`, `paymentStatus=PENDING`) immediately at booking
time, same pattern as `travel-packages-service`. `paymentStatus` exists on
the entity today specifically so a future Payment Service integration
doesn't require a schema migration — just a change to *when* `bookingStatus`
flips to `CONFIRMED`.

**Reviews: DB constraint backs the application check.**
`HotelReviewServiceImpl.createReview` checks
`existsByHotelIdAndUserId` before inserting, but `HotelReview` also has a DB
`UNIQUE(hotel_id, user_id)` constraint. The application check gives a clean
400 in the common case; the DB constraint is the actual guarantee under
concurrent requests from the same user (a race between two check-then-insert
calls could otherwise both pass the application check).

**`averageRating`/`reviewCount` are denormalized onto `Hotel`, updated
incrementally.** Each new review recomputes
`newAverage = (oldAverage * oldCount + newRating) / (oldCount + 1)` rather
than running `AVG()`/`COUNT()` over the full review table on every hotel
read — the same reasoning as `TravelPackage.availableSlots` being
maintained incrementally instead of derived from `PackageBooking` on read.

---

## notification-service design notes

**Two entry points, two trust models — not one API with mixed auth.**
`/notifications/**` is the end-user inbox: gateway-routed, JWT-authenticated,
identical header-trust pattern to every other service (`JwtValidator`).
`/internal/notifications` is how *other services* create notifications:
it is absent from the gateway's route table entirely (not just
access-controlled — genuinely unroutable from outside the Docker network),
and additionally guarded by a shared `X-Internal-Api-Key` header checked by
`InternalApiKeyFilter`. Two separate filters, two separate concerns, rather
than one endpoint trying to accept both a user JWT and a service secret.

**Notification creation never depends on a User entity or auth-service
call.** `Notification.userId` is a bare UUID, same as hotel-service's
`HotelBooking.userId`. If the caller wants an email sent, it passes
`recipientEmail` directly in the creation request — notification-service
doesn't look anything up. In practice, this address is the caller's own
`X-Authenticated-Email` header (forwarded by the gateway on the original
request that triggered the notification), just threaded one hop further.
This keeps notification-service's coupling to zero: no dependency on
auth-service being up, no user cache to keep in sync.

**Email is a side effect, never a dependency.** `NotificationServiceImpl.
createNotification` always persists the in-app `Notification` first, in the
same transaction as the request. Only *after* that succeeds does it call
`EmailDispatchServiceImpl.sendAsync`, `@Async` and in a separate DB
transaction against the already-committed row. A slow SMTP server, a bad
address, or Mailhog being down can only ever produce `emailStatus=FAILED`
on that one row — it cannot fail notification creation, and it especially
cannot fail whatever business action (a hotel booking, say) triggered the
notification in the first place.

**Calling services must apply the same discipline.** Every booking service —
hotel-service, bus-booking-service, ride-share-service, travel-packages-service
— has its own copy of `NotificationClientImpl` (identical code, different
package). Each wraps its call to `/internal/notifications` in a try-catch and
marks itself `@Async` — the same "never block, never fail the caller"
contract, one layer further out. If notification-service is completely down,
a booking still confirms; the user just doesn't get a notification about it.
This is a deliberate choice: notifications are a nice-to-have, not part of a
booking's correctness guarantee. A future Payment Service should *not* copy
this pattern for payment confirmations — that's a case where the side effect
genuinely can't be best-effort.

The four copies of `NotificationClient`/`NotificationClientImpl` are
intentionally duplicated rather than extracted into a shared library module.
At four call sites, a shared client would need its own artifact, its own
versioning, and a release step every time it changed — more process than the
~70 lines of code justify right now. If a fifth or sixth service needs it,
that's the point to reconsider a `notification-client-starter` module.

**Where each service gets the recipient's email differs by design, not by
accident.** hotel-service reads `X-Authenticated-Email` off the request that
triggered the notification (it has no local user cache). bus-booking-service
reads `Bookingdetails.email`, which was captured directly on the booking
form. ride-share-service and travel-packages-service both read
`UserRef.email`, the cached hint already used for `user_ref` FK integrity
(see "Three architectural fixes" below for why that cache is a hint, not a
source of truth). All four ultimately trace back to the same JWT claim; only
the *path* to it differs per service's existing data model.

**Local email testing uses Mailhog, not a real provider.** `docker-compose.yml`
runs `mailhog/mailhog` as a fake SMTP server; every email
notification-service sends shows up at `http://localhost:8025` instead of
requiring SendGrid/SES credentials just to see that the feature works. Swap
`spring.mail.*` for a real provider only when actually deploying.

---

## Why destination discovery lives inside travel-packages-service

A destination module was proposed as a 7th standalone microservice. It was
deliberately **not** built that way — it's a second module
(`com.travelplatform.packages.destination`) inside the existing
travel-packages-service, sharing its database, sharing its
`GlobalExceptionHandler`/`SecurityConfig`/`JwtValidator`.

**The coupling test that decided this:** if Service A needs Service B's
data on nearly every request, they're the same bounded context, not two
services connected by a network call. A destination detail page needs to
show its packages; a package detail page benefits from showing "things to
do here" (the destination's attractions/activities); browsing packages by
destination is a basic filter. None of that is occasional cross-service
chat — it's the core read pattern for both halves. Splitting them would
mean every package-browse request either makes a synchronous HTTP call to
a destination service or duplicates destination data locally just to avoid
that call — neither is better than just sharing one database.

**What this bought concretely:** `TravelPackage.destinationId` is a real
`UUID` column referencing `Destination.id` in the *same* database — no
network call, no partial-failure handling (no "what if the destination
service is down when creating a package"), no third copy of a
denormalized destination snapshot. The field sits alongside the original
free-text `destination` String for backward compatibility with packages
created before this module existed; new packages can populate both.

**Parallel structure to the package module, intentionally.** Just like
`PackageController`/`PackageAdminController`, destination splits into
`DestinationController` (`/destinations/**`, any authenticated user —
browse, search, category filter, popular list) and
`DestinationAdminController` (`/destinations/admin/**`, `ROLE_ADMIN` —
create/update/delist). Same `@PreAuthorize` + gateway-path-matcher
defense-in-depth pattern as packages. Same soft-delete (`active=false`)
convention as `TravelPackage.deletePackage()`.

**Attraction and Activity are real child entities, not JSON-converted
lists,** unlike `TravelPackage`'s inclusions/exclusions/places/activities.
The difference: those package fields are flat strings entered once by an
admin and just displayed back. `Attraction`/`Activity` plausibly want
their own identity later (e.g. a future reviews-per-attraction feature, or
linking an `Activity` here to a bookable add-on) — UUID-keyed child
entities keep that door open without a migration. `imageUrls` on
`Destination` reuses the same `StringListConverter` as packages, since
images have no sub-structure here either.

**`rating` exists as a field with no writer yet.** It's populated as
`0.0` on creation and exposed via the `popular` endpoint
(`ORDER BY rating DESC`), but nothing currently updates it — that's
intentionally deferred until a reviews feature exists, at which point
it likely becomes a computed aggregate rather than a stored value.

---

## Docker setup

The platform runs via `docker-compose.yml` at the repo root. Eight images
(one per Spring Boot module) plus one shared Postgres container plus
Mailhog (fake SMTP, for notification-service's email channel).

**Multi-stage Dockerfiles.** Each service's `Dockerfile` has two stages: a
`maven:3.9-eclipse-temurin-17` build stage that compiles just that module
(`mvn -pl <module> -am package`, using `-am` to also build the parent reactor's
dependency graph it needs), and a slim `eclipse-temurin:17-jre-alpine` runtime
stage that copies out only the built jar. This keeps final images small —
no Maven, no source code, no build cache in the shipped image — and lets
Docker cache the dependency-resolution layer separately from the
source-compile layer (the `pom.xml` files are `COPY`'d before `src/`, so
changing application code doesn't invalidate the Maven dependency cache).

**Build context is the repo root, not each service's folder.** Because
this is a multi-module Maven reactor, every service's Dockerfile needs the
parent `pom.xml` and every sibling module's `pom.xml` (Maven resolves the
reactor's dependency versions from there) even though it only compiles its
own module's source. `docker-compose.yml` sets each service's
`build.context: .` with `build.dockerfile: <service>/Dockerfile`
accordingly.

**Why Postgres needs an init script.** The official `postgres` image only
auto-creates one database — whatever `POSTGRES_DB` is set to — on first
boot. This platform needs seven (`travel_auth_db`, `travel_bus_booking_db`,
`travel_rideshare_db`, `travel_packages_db`, `travel_hotel_db`,
`travel_notification_db`, and implicitly `postgres` itself as the default
admin DB).
`docker/postgres-init/01-create-databases.sql`
is mounted into `/docker-entrypoint-initdb.d/`, which Postgres's
entrypoint script runs automatically — but **only** against a fresh, empty
data volume. If you change the init script after the volume already
exists, it won't re-run; `docker-compose down -v` first to force a clean
re-init.

**Healthcheck-gated startup, not just container-start ordering.**
`depends_on` alone only guarantees container *start* order, not that the
dependency is actually ready to accept traffic — a freshly-started
Postgres container can take a few seconds before `pg_isready` succeeds,
and a freshly-started Eureka server needs a moment before
`/actuator/health` returns 200. Every `depends_on` in `docker-compose.yml`
uses `condition: service_healthy`, which blocks the dependent service from
starting until the healthcheck actually passes. This mirrors the
"Startup order" section above, but enforced by Docker instead of by a
human running `mvn spring-boot:run` in the right sequence across five
terminals.

**Environment-variable configuration, with localhost defaults.** Every
service's `application.properties` uses Spring's `${ENV_VAR:default}`
syntax — e.g. `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/travel_auth_db}`.
This means the exact same jar works two ways with zero code changes:
running locally via `mvn spring-boot:run` (falls back to the `localhost`
defaults), or running in `docker-compose` (the compose file injects
container-network hostnames like `postgres` and `service-registry` as env
vars, overriding the defaults). One build artifact, two environments.

**`api-gateway`'s JWT secret is now env-var overridable**
(`${JWT_SECRET:...}` in `application.yml`); auth-service's
`JwtConstant.SECRET_KEY` is still a hardcoded Java `static final` constant
— it's instantiated manually (`new JwtValidator()`, not a Spring bean) in
several places, so converting it to `@Value` injection would need a wider
refactor than the Docker setup alone called for. Both copies of the secret
must match (the gateway signs nothing but validates everything; auth-service
signs every token), so if you do override `JWT_SECRET` for the gateway in
a real deployment, `JwtConstant.SECRET_KEY` needs to be updated to match
or token validation will fail platform-wide.

---

## Three architectural fixes (applied after initial build)

### Fix 1 — user_ref email is a cache hint, not a source of truth

**The problem:** `UserRef`/`UserAdmin` entities in bus-booking-service,
ride-share-service, and travel-packages-service stored `email` with
`@Column(nullable = false, unique = true)`. This made the email look
authoritative — but if a user changes their email in auth-service, the
cached copy in each service's `user_ref` table becomes stale with no
automatic sync mechanism.

**What was changed:**
- `nullable = true`, uniqueness constraint removed from `user_ref.email`
  across all three domain services. The column stays (it's a useful
  display hint) but is no longer treated as an identity source.
- `UserProfileController` added to auth-service: `GET /auth/users/{id}`
  and `GET /auth/users/by-email/{email}` — domain services can call
  these endpoints when they need fresh, authoritative user data.
- Auth-service `AppConfig` narrowed from blanket `/auth/** → permitAll`
  to individual path matchers for only the true public endpoints
  (register/login/refresh/logout). `/auth/users/**` now requires a JWT.
- Gateway `JwtAuthFilter.PUBLIC_PATHS` updated to match — `/auth/users/**`
  is not in the bypass list.

**Pattern going forward:** store only `UUID userId` for FK integrity.
Read `user_ref.email` for fast display. Call `GET /auth/users/{id}` when
fresh data is required (e.g. before sending confirmation emails).

---

### Fix 2 — Package → Destination FK is now enforced, not optional

**The problem:** `TravelPackage` had two destination fields:
- `String destination` — free-text "Goa", stale-string problem
- `UUID destinationId` — correct FK, but nullable and not enforced

A package could be created with `destination = "Goa"` and no
`destinationId`, meaning the destination module couldn't browse/filter
packages by destination, and editing the destination name in the
`Destination` table wouldn't reflect on packages.

**What was changed:**
- `String destination` field removed from `TravelPackage` entirely.
- `UUID destinationId` is now `@Column(nullable = false)` — required.
- `CreatePackageRequest.destinationId` is `@NotNull` — validation
  fails at the HTTP layer if not provided.
- `GET /packages/search?destination=` (free-text search) replaced by
  `GET /packages/by-destination/{destinationId}` — proper UUID FK lookup.
- `TravelPackageRepository.findByDestinationContainingIgnoreCaseAndActiveTrue`
  replaced by `findByDestinationIdAndActiveTrue(UUID destinationId)`.

**Workflow now:** admin creates a `Destination` first
(`POST /destinations/admin`), gets back its UUID, then supplies that
UUID in `CreatePackageRequest.destinationId`.

---

### Fix 3 — Destination.rating renamed to manualRating

**The problem:** `Destination.rating = 0.0` was a field with no writer —
it looked like a live user-review aggregate but nothing ever updated it,
making it misleading.

**What was changed:**
- Field renamed from `rating` to `manualRating` across the entity,
  DTOs, repository (`findByActiveTrueOrderByManualRatingDesc`), and
  service impl.
- `UpdateDestinationRequest` now accepts `manualRating` (0.0–5.0,
  validated) so admins can explicitly set it via
  `PUT /destinations/admin/{id}`.
- The `GET /destinations/popular` endpoint still works — now orders
  by `manualRating DESC` explicitly.
- The name makes clear this is admin-set, not computed. When a review
  system is built, `manualRating` will likely be replaced by a computed
  average from a `Review` entity — at that point the field and the
  endpoint behavior change together, cleanly.
