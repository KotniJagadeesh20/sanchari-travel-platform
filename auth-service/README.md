# auth-service

Owns user accounts, authentication, and JWT issuance for the entire
platform. No other service stores passwords or roles — they each keep a
lightweight `user_ref` (UUID + email) populated from gateway-forwarded
JWT claims, never calling this service directly at request time.

## Port

`8081`

## Database

`travel_auth_db` — owns `user_admin` and `refresh_token` tables.

## Dependencies

- `spring-boot-starter-web`, `-data-jpa`, `-security`, `-validation`
- `spring-cloud-starter-netflix-eureka-client`
- `postgresql` driver
- `jjwt-api` / `jjwt-impl` / `jjwt-jackson`
- `springdoc-openapi-starter-webmvc-ui` (Swagger)

## Auth model

- Two roles: `ROLE_USER`, `ROLE_ADMIN`. Registration endpoint determines
  which role is assigned (`/userRegister` → `ROLE_USER`, `/registerAdmin` → `ROLE_ADMIN`).
- **Access token** (JWT): 15-minute expiry. Contains `email`, `userId`, and
  `authorities` claims — the gateway reads all three and forwards them
  downstream as headers.
- **Refresh token**: UUID, stored server-side in the `refresh_token` table
  with an expiry and a `revoked` flag. 7-day expiry. **Rotates on every use**
  — each call to `/refresh-token` invalidates the old refresh token and
  issues a new one, so a leaked refresh token is only useful once before
  the legitimate next refresh fails (a signal you could act on for
  suspicious-activity detection, though that's not built yet).
- `AuthService` / `AuthServiceImpl` holds all the business logic; the
  controller is a thin HTTP adapter — see `AuthServiceImplTest` for the
  actual behavior contract (register/login/refresh/logout success and
  failure paths, password hashing verification, role assignment).

## APIs

All public — no JWT required (this is how you *get* a JWT).

| Method | Path | Description |
|---|---|---|
| POST | `/auth/userRegister` | Register a `ROLE_USER` account. Returns access token + refresh token. |
| POST | `/auth/registerAdmin` | Register a `ROLE_ADMIN` account. Same response shape. |
| POST | `/auth/Loginin` | Login by email + password. Returns access token + refresh token. |
| POST | `/auth/refresh-token` | Exchange a valid refresh token for a new access token (rotates the refresh token too). |
| POST | `/auth/logout` | Revoke a refresh token (single-session logout). |

### Request/response shapes

**Register** (`POST /auth/userRegister` or `/auth/registerAdmin`)
```json
// Request
{
  "name": "Asha Kumar",
  "email": "asha@example.com",
  "password": "Passw0rd!",
  "phone": "9876543210",
  "dob": "1995-06-15",
  "gender": "Female",
  "age": 29
}

// Response (201)
{
  "success": true,
  "message": "Account Created Successfully",
  "jwt": "<access token>",
  "refreshToken": "<uuid>",
  "userAdmin": { ... }
}
```
Password must be 8+ characters with at least one uppercase, lowercase,
digit, and special character. Phone must be a 10-digit Indian mobile
number starting with 6-9.

**Login** (`POST /auth/Loginin`)
```json
{ "email": "asha@example.com", "password": "Passw0rd!" }
```

**Refresh** (`POST /auth/refresh-token`)
```json
{ "refreshToken": "<uuid>" }
// → { "success": true, "accessToken": "<new jwt>", "refreshToken": "<new uuid>" }
```

**Logout** (`POST /auth/logout`)
```json
{ "refreshToken": "<uuid>" }
```

## User profile endpoints

Used by domain services when their cached `user_ref.email` may be stale.
Requires a valid JWT (these are **not** public — fall under `anyRequest().authenticated()`).

| Method | Path | Description |
|---|---|---|
| GET | `/auth/users/{userId}` | Get user profile by UUID. Returns name, email, role — **never** password. |
| GET | `/auth/users/by-email/{email}` | Get user profile by email address. |

```json
// Response
{
  "id": "3fa85f64-...",
  "name": "Asha Kumar",
  "email": "asha@example.com",
  "phone": "9876543210",
  "gender": "Female",
  "age": 29,
  "role": "ROLE_USER"
}
```

## Error responses

| Status | When |
|---|---|
| 400 | Validation failure, or email already registered |
| 401 | Bad credentials on login |
| 403 | Refresh/logout with an expired, revoked, or unknown refresh token |

## Running locally

```bash
mvn spring-boot:run
```

Needs `travel_auth_db` to exist in Postgres first (or use `docker-compose up`,
which creates it automatically). Start after service-registry.

## Swagger UI

http://localhost:8081/swagger-ui/index.html

## Tests

`AuthServiceImplTest` (service-layer business rules, 17 cases),
`UserAdminControllerTest` (HTTP layer, 18 cases), `UserProfileControllerTest`
(profile lookup, password-never-exposed assertion), `JwtProviderTest`,
`JwtValidatorTest`, `RefreshTokenServiceTest`.
