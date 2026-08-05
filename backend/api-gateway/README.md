# api-gateway

Single entry point for all client traffic. Validates JWTs once (so domain
services don't each need to), routes requests to the right service by path
prefix using Eureka-based service discovery, and forwards user identity
downstream via headers.

## Port

`8080`

## Dependencies

- `spring-cloud-starter-gateway` (reactive, WebFlux-based)
- `spring-cloud-starter-netflix-eureka-client`
- `jjwt-api` / `jjwt-impl` / `jjwt-jackson` (JWT validation)
- `spring-boot-starter-actuator`

## Configuration

| Property | Default | Override (env var) |
|---|---|---|
| `eureka.client.service-url.defaultZone` | `http://localhost:8761/eureka/` | `EUREKA_URI` |
| `jwt.secret` | (shared platform secret) | `JWT_SECRET` |

**Important:** this secret must match `JwtConstant.SECRET_KEY` in
auth-service — the gateway validates tokens that auth-service signs. If you
override `JWT_SECRET` here, update auth-service's constant to match or every
request will fail JWT validation.

## What it does

1. **Validates JWT** (`JwtAuthFilter`, a `GlobalFilter` ordered to run first)
   for every request *except* the public paths listed below.
2. **Forwards identity downstream** as three headers, read from the JWT's
   claims, for any service behind the gateway to trust without re-parsing
   the token itself:
   - `X-Authenticated-Email`
   - `X-Authenticated-User-Id`
   - `X-Authenticated-Authorities` (comma-separated, e.g. `ROLE_USER` or `ROLE_ADMIN`)
3. **Routes by path prefix** via Eureka service discovery (`lb://service-name`).
4. **CORS** — allows `http://localhost:5173` (the platform's Vite frontend).

## Public paths (no JWT required)

- `/auth/userRegister`, `/auth/registerAdmin`, `/auth/Loginin`, `/auth/refresh-token`, `/auth/logout`

`/auth/users/**` (user profile lookups) is **NOT** in this list and requires a valid JWT.
- `/actuator/**`
- `/swagger-ui/**`, `/v3/api-docs/**`

Every other path requires a valid `Authorization: Bearer <jwt>` header.

## Routing table

| Path prefix | Routes to |
|---|---|
| `/auth/**` | `lb://auth-service` |
| `/api/**`, `/admin/**` | `lb://bus-booking-service` |
| `/rides/**` | `lb://ride-share-service` |
| `/packages/**`, `/destinations/**` | `lb://travel-packages-service` |

## Running locally

```bash
mvn spring-boot:run
```

Start **last**, after service-registry and all four domain services — it
routes by looking up live service instances in Eureka, so starting it first
just means early requests fail to route until the others register.

## Security note

The gateway is a stateless JWT validator, not a session store. Domain
services trust the `X-Authenticated-*` headers it forwards rather than
re-validating the JWT themselves — this only works correctly if domain
services are **not** directly reachable from outside the Docker network
(in `docker-compose`, only `api-gateway`'s port is meant to be the public
surface; the others expose ports too for local debugging convenience, but
in a real deployment, only the gateway should be internet-facing).

## APIs

None directly — this service has no business endpoints of its own. It's a
proxy. Hit it at `http://localhost:8080/<routed-path>` and it forwards to
whichever domain service owns that path, per the routing table above.
