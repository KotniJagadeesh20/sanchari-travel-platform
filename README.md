# Travel Platform

[![CI](https://github.com/KotniJagadeesh20/sanchari-travel-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/KotniJagadeesh20/sanchari-travel-platform/actions/workflows/ci.yml)

A Spring Boot microservices platform for trip planning — bus tickets, peer-to-peer
ride sharing, curated travel packages, destination discovery, hotel booking,
in-app/email notifications, and an AI travel assistant, all behind a single
API gateway with JWT-based auth.

## Services

| Service | Port | What it does | Docs |
|---|---|---|---|
| service-registry | 8761 | Eureka — service discovery | [README](service-registry/README.md) |
| api-gateway | 8080 | Routes by path, validates JWT once, forwards identity headers | [README](api-gateway/README.md) |
| auth-service | 8081 | Register / login / refresh / logout, issues JWTs | [README](auth-service/README.md) |
| bus-booking-service | 8082 | Admin-owned bus/driver inventory, ticket booking | [README](bus-booking-service/README.md) |
| ride-share-service | 8083 | Peer-to-peer rides, approval-based booking | [README](ride-share-service/README.md) |
| travel-packages-service | 8084 | Curated packages + destination discovery | [README](travel-packages-service/README.md) |
| hotel-service | 8085 | Hotels, rooms, hotel booking, reviews — books independently of packages | [README](hotel-service/README.md) |
| notification-service | 8086 | In-app + email notifications — internal API only, not gateway-routed for creation | [README](notification-service/README.md) |
| travel-agent-service | 8088 | AI travel assistant — Claude tool-use loop over the other services' search APIs | [README](travel-agent-service/README.md) |

For the full architectural reasoning — why services are split the way they are,
the JWT propagation pattern, database ownership, and design decisions for each
domain — see **[ARCHITECTURE.md](ARCHITECTURE.md)**.

## Quick start (Docker — recommended)

Requires Docker and Docker Compose.

```bash
git clone <repo>
cd travel-platform
cp .env.example .env      # then fill in JWT_SECRET — see .env.example
docker-compose up --build
```

This builds all 9 services, starts a Postgres container with seven databases
pre-created (one per service — see `docker/postgres-init/`), starts Mailhog
(fake SMTP for notification-service's email channel — view sent mail at
http://localhost:8025), and brings everything up in the right order using
healthcheck-gated startup (`depends_on: condition: service_healthy`), not
just container-start order.

First boot takes a few minutes (Maven downloads + 9 builds). Subsequent
`docker-compose up` runs are fast unless source changed (add `--build` to
rebuild).

**Verify it's up:**
```bash
curl http://localhost:8761                      # Eureka dashboard — all 8 services should be registered
curl http://localhost:8080/auth/userRegister ... # via the gateway, see auth-service/README.md
```

**Stop everything:**
```bash
docker-compose down          # stop, keep data
docker-compose down -v       # stop AND wipe the postgres volume
```

## Quick start (local, no Docker)

Requires Java 17, Maven, and a local PostgreSQL instance.

1. Set `JWT_SECRET` and `NOTIFICATION_INTERNAL_API_KEY` in your shell —
   every service that reads either one fails to start without it, and each
   must see the *same* value across every service that reads it
   (`JWT_SECRET`: auth-service + api-gateway; `NOTIFICATION_INTERNAL_API_KEY`:
   notification-service + bus-booking/ride-share/packages/hotel):
   ```bash
   export JWT_SECRET=$(openssl rand -base64 64)
   export NOTIFICATION_INTERNAL_API_KEY=$(openssl rand -base64 32)
   ```
2. Create seven databases: `travel_auth_db`, `travel_bus_booking_db`,
   `travel_rideshare_db`, `travel_packages_db`, `travel_hotel_db`,
   `travel_notification_db` (the `docker/postgres-init/01-create-databases.sql`
   script shows the exact statements if you want to run them manually).
   notification-service also needs an SMTP server reachable at
   `spring.mail.host`/`port` — run Mailhog yourself
   (`docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog`) or just leave
   `notification.email.enabled=false` if you don't care about email locally.
3. Each service's `application.properties` defaults to
   `jdbc:postgresql://localhost:5432/<db_name>` with username/password
   `postgres`/`postgres` — override via env vars
   (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`)
   if your local Postgres differs.
4. Start in this order (each needs the previous one up first):
   ```bash
   cd service-registry        && mvn spring-boot:run   # wait for Eureka to be up
   cd auth-service             && mvn spring-boot:run
   cd bus-booking-service       && mvn spring-boot:run
   cd ride-share-service        && mvn spring-boot:run
   cd travel-packages-service   && mvn spring-boot:run
   cd hotel-service              && mvn spring-boot:run
   cd notification-service       && mvn spring-boot:run
   cd travel-agent-service       && mvn spring-boot:run   # optional — only if testing the AI agent; needs ANTHROPIC_API_KEY
   cd api-gateway                && mvn spring-boot:run   # start last
   ```

## API documentation

Each service exposes Swagger UI once running, **but only when running
locally via `mvn spring-boot:run`** — under `docker-compose`, these ports
are intentionally not published to the host (see the `expose` comments in
`docker-compose.yml`: these services trust gateway-forwarded identity
headers, so publishing them would let anyone on localhost forge those
headers and bypass auth). To browse Swagger UI against the Dockerized
stack, `docker exec` into the container and curl locally, or temporarily
add a `ports:` mapping back for that one service while you're debugging.

- auth-service: http://localhost:8081/swagger-ui/index.html
- bus-booking-service: http://localhost:8082/swagger-ui/index.html
- ride-share-service: http://localhost:8083/swagger-ui/index.html
- travel-packages-service: http://localhost:8084/swagger-ui/index.html
- hotel-service: http://localhost:8085/swagger-ui/index.html
- notification-service: http://localhost:8086/swagger-ui/index.html
- travel-agent-service: http://localhost:8088/swagger-ui/index.html

(service-registry and api-gateway don't expose business APIs, so no Swagger UI.)

All protected endpoints use `Authorization: Bearer <jwt>` — get a token from
`POST /auth/Loginin` first. See each service's README for full endpoint lists,
or hit them through the gateway at `http://localhost:8080`.

## Auth model

- Two roles platform-wide: `ROLE_USER`, `ROLE_ADMIN` — issued by auth-service,
  carried in every JWT.
- Access tokens expire in 15 minutes; refresh tokens (UUID, stored in
  auth-service's database) last 7 days and rotate on every use.
- The gateway validates the JWT once, then forwards `X-Authenticated-Email`,
  `X-Authenticated-User-Id`, and `X-Authenticated-Authorities` headers to
  whichever domain service it routes to — those services trust the headers
  rather than re-parsing the JWT. See ARCHITECTURE.md for why, and the
  network-isolation assumption this depends on.

## Tech stack

Java 17 · Spring Boot 3.2.4 · Spring Cloud 2023.0.1 (Eureka, Gateway) ·
PostgreSQL 16 · Spring Data JPA · Spring Security · JWT (jjwt) · springdoc-openapi ·
JUnit 5 + Mockito · Maven (multi-module reactor) · Docker / Docker Compose

## Project layout

```
travel-platform/
├── pom.xml                          ← parent reactor pom
├── docker-compose.yml
├── docker/postgres-init/            ← multi-database init script
├── README.md                        ← you are here
├── ARCHITECTURE.md                  ← deep design rationale
├── service-registry/
├── api-gateway/
├── auth-service/
├── bus-booking-service/
├── ride-share-service/
├── travel-packages-service/
│   └── (packages + destinations modules, same DB — see ARCHITECTURE.md)
├── hotel-service/
│   └── (independent bounded context — no shared DB with any other service)
├── notification-service/
│   └── (in-app + email; /internal/notifications is not gateway-routed)
└── travel-agent-service/
    └── (no database of its own — pure orchestrator over the other services)
```

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs on every push/PR:

- **build-and-test** — `mvn clean test` across all 9 modules (compiles
  everything, runs unit + JPA-slice tests). Deliberately not `mvn verify`:
  travel-agent-service's `*ClientIT.java` tests need the full docker-compose
  stack running for real, which a CI runner can't provide out of the box —
  see that job's inline comment, and `travel-agent-service/README.md`'s
  "Known limitation" section for how to run those locally.
- **validate-compose** — `docker compose config` to catch syntax/interpolation
  errors in `docker-compose.yml` without actually starting anything.

Run the same checks locally before pushing:
```bash
mvn clean test
JWT_SECRET=x NOTIFICATION_INTERNAL_API_KEY=x docker compose config --quiet
```

## Known limitations / not yet built

- No Redis caching yet (planned for destination/package search).
- No Kafka / event bus (no current multi-consumer event need — see ARCHITECTURE.md).
- `restaurant-service` is the next planned domain service, not yet built.
- No Payment Service yet — hotel-service (like the other booking services)
  auto-confirms bookings immediately instead of gating on payment.
- All four booking services (bus, ride-share, packages, hotel) now call
  notification-service on booking/cancel/approve/reject — see each
  service's README for exactly which events fire and who gets notified.
