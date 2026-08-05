# ride-share-service

Peer-to-peer ride sharing — any authenticated user can post a ride (becoming
that ride's driver) and separately book seats on someone else's ride as a
passenger. No `ROLE_DRIVER` — "driver" just means "the user who owns this
ride," not a platform role. Bookings go through an explicit
PENDING → APPROVED/REJECTED flow, unlike bus-booking's auto-confirm.

## Port

`8083`

## Database

`travel_rideshare_db` — owns `ride`, `ride_booking`, and a lightweight
`user_ref` table (same pattern as bus-booking-service).

## Dependencies

- `spring-boot-starter-web`, `-data-jpa`, `-security`, `-validation`
- `spring-cloud-starter-netflix-eureka-client`
- `postgresql` driver
- `springdoc-openapi-starter-webmvc-ui`

## Roles

None beyond the platform's standard `ROLE_USER` — there's nothing for
`ROLE_ADMIN` to curate here, unlike bus-booking and packages. Authorization
is **ownership-based**, enforced in the service layer: every mutating
endpoint checks "is the caller the driver of this ride?" or "is the caller
the passenger who made this booking?" and returns 403 if not.

## Booking lifecycle

```
Passenger books seats  →  PENDING   (seats NOT yet deducted)
Driver approves          →  APPROVED  (seats deducted from availableSeats)
Driver rejects            →  REJECTED  (no seat change — none were taken)
Passenger cancels          →  CANCELLED (seats returned if it was APPROVED)
```

Seats are intentionally not deducted at PENDING time — a popular ride can
accumulate more PENDING requests than physical seats, and the driver
decides who to approve. `approveBooking` re-checks availability at
approval time, so a race between two simultaneous approvals is caught.

## APIs

All require JWT (`/rides/**` is fully authenticated, no public sub-paths).

| Method | Path | Description | Who |
|---|---|---|---|
| POST | `/rides` | Create a ride. Caller becomes the driver. | Any user |
| PUT | `/rides/{rideId}` | Update ride details. | Ride's driver only |
| DELETE | `/rides/{rideId}` | Cancel the ride (keeps booking history). | Ride's driver only |
| GET | `/rides/driver` | List rides I created as a driver. | Any user (own rides) |
| GET | `/rides/search?source=&destination=&date=` | Search SCHEDULED rides. | Any user |
| GET | `/rides/{rideId}` | Get ride details. | Any user |
| POST | `/rides/{rideId}/book` | Request seats → creates PENDING booking. | Any user (not the ride's own driver) |
| GET | `/rides/bookings` | List my bookings as a passenger. | Any user (own bookings) |
| DELETE | `/rides/bookings/{bookingId}` | Cancel my booking. | Booking's passenger only |
| GET | `/rides/{rideId}/bookings` | View bookings on my ride. | Ride's driver only |
| POST | `/rides/bookings/{bookingId}/approve` | Approve a PENDING booking. | Ride's driver only |
| POST | `/rides/bookings/{bookingId}/reject` | Reject a PENDING booking. | Ride's driver only |

### Sample requests

```json
POST /rides
{
  "source": "Hyderabad",
  "destination": "Vijayawada",
  "travelDate": "2026-06-25",
  "departureTime": "08:00",
  "totalSeats": 4,
  "pricePerSeat": 500.0
}

POST /rides/{rideId}/book
{ "seats": 2 }
```

## Business rules enforced

- A driver cannot book their own ride.
- Booking fails if requested seats exceed `availableSeats`.
- Booking fails on a ride that's COMPLETED or CANCELLED.
- `GET /rides/{rideId}/bookings` is driver-only — without this check, any
  authenticated user could view another driver's passenger list (names,
  emails) just by guessing a ride UUID. (This was a real bug caught during
  development — see ARCHITECTURE.md.)

## Notifications

Every booking state change fires a call to `notification-service`'s internal
API (`NotificationClientImpl`), always in-app plus email (via the cached
`UserRef.email`). Unlike hotel/bus/package bookings, ride-share is two-sided,
so the notification target flips depending on who needs to act next:

| Event | Notified | Why |
|---|---|---|
| `bookRide` (new PENDING request) | Driver | They need to approve/reject it |
| `approveBooking` | Passenger | Their booking is now confirmed |
| `rejectBooking` | Passenger | Their booking was declined |
| `cancelBooking` | Driver | Their seat(s) just freed up |

All calls are `@Async` and wrapped in try-catch — a slow or unreachable
notification-service never blocks or fails the booking action itself.

## Error responses

| Status | When |
|---|---|
| 400 | Validation failure, own-ride booking, insufficient seats, ride not bookable |
| 403 | Caller doesn't own the ride/booking they're trying to act on |
| 404 | Ride or booking not found |

## Running locally

```bash
mvn spring-boot:run
```

Needs `travel_rideshare_db` to exist first. Start after service-registry.

## Swagger UI

http://localhost:8083/swagger-ui/index.html

## Tests

`RideServiceImplTest`, `RideBookingServiceImplTest` (17 cases — booking
rules, approval/rejection, cancellation refund logic), `RideControllerTest`,
`UserRefServiceImplTest`, `JwtValidatorTest`.
