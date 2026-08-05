# bus-booking-service

Admin-owned bus and driver inventory with simple, auto-confirm ticket
booking — the classic "operator publishes a fixed schedule, users buy
seats" model. No approval step, no peer-to-peer dynamics (that's
ride-share-service).

## Port

`8082`

## Database

`travel_bus_booking_db` — owns `bus`, `driver`, `bookingdetails`, and a
lightweight `user_ref` table (UUID + email, synced from gateway headers —
this service does not manage user accounts, see auth-service).

## Dependencies

- `spring-boot-starter-web`, `-data-jpa`, `-security`, `-validation`
- `spring-cloud-starter-netflix-eureka-client`
- `postgresql` driver
- `springdoc-openapi-starter-webmvc-ui`

## Roles

`ROLE_ADMIN` manages inventory (buses, drivers). `ROLE_USER` (any
authenticated user) searches and books. Enforced via `@PreAuthorize` on
`AdminController` plus gateway-trusted header checks in `SecurityConfig`.

## Identity model

This service does **not** parse JWTs itself. It trusts
`X-Authenticated-Email` / `X-Authenticated-Authorities` headers forwarded
by api-gateway (`JwtValidator` here just reads those headers into Spring
Security's context — see ARCHITECTURE.md for the network-isolation
assumption this depends on). User booking identity comes from those
headers, not from a `userId` in the URL.

## APIs

### Admin (`/admin/**`, requires `ROLE_ADMIN`)

| Method | Path | Description |
|---|---|---|
| POST | `/admin/addbus` | Add a bus. 400 if `busno` already exists. |
| POST | `/admin/editBus` | Update a bus (looked up by `busno`). |
| DELETE | `/admin/deletebus/{busNo}` | Delete a bus by bus number. |
| GET | `/admin/allbusses` | List all buses. |
| POST | `/admin/addDriver` | Add a driver. 400 if email already registered. |
| POST | `/admin/editDriver` | Update a driver. |
| DELETE | `/admin/deletedriver/{id}` | Delete a driver by UUID. |
| GET | `/admin/alldrivers` | List all drivers. |
| POST | `/admin/assignDriver/{busId}/{driverId}` | Assign a driver to a bus. |

### User (`/api/user/**`, any authenticated user)

| Method | Path | Description |
|---|---|---|
| GET | `/api/user/searchbusses/{source}/{destination}/{date}` | Search buses by route + date (`yyyy-MM-dd`). |
| POST | `/api/user/bookticket/{busId}` | Book a ticket. Passenger details in body; user identity from gateway headers. |
| GET | `/api/user/bookingDetails` | Booking history for the authenticated user. |
| DELETE | `/api/user/cancelbooking/{id}` | Cancel a booking by UUID. |

### Sample booking request

```json
POST /api/user/bookticket/{busId}
{
  "name": "Ravi Teja",
  "email": "ravi@example.com",
  "phoneno": "9876543210",
  "age": 32
}
```

## Notifications

Booking and cancellation each fire a call to `notification-service`'s internal
API (`NotificationClientImpl`) — in-app always, plus email since the booking
record already carries the traveler's email address. The call is `@Async`
and wrapped in try-catch: if notification-service is slow or down, the
booking/cancellation still succeeds.

## Error responses

| Status | When |
|---|---|
| 400 | Validation failure, duplicate bus number / driver email |
| 401 | Missing/invalid JWT at the gateway |
| 403 | Authenticated but not `ROLE_ADMIN` on an admin endpoint |
| 404 | Bus, driver, or booking not found |

## Running locally

```bash
mvn spring-boot:run
```

Needs `travel_bus_booking_db` to exist first. Start after service-registry.

## Swagger UI

http://localhost:8082/swagger-ui/index.html

## Tests

`BusServiceImplTest`, `DriverServiceImplTest`, `BookingdetailsServiceImplTest`,
`UserAdminServiceimplTest`, `AdminControllerTest`, `UserControllerTest`,
`JwtValidatorTest`.
