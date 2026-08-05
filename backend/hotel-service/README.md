# hotel-service

An independent bounded context: hotels, rooms, hotel bookings, and hotel
reviews. It owns none of Destination or User — it references them only by
UUID (`destinationId`, `userId`), never by JPA relationship or shared
database. Hotels can be booked directly, without ever going through
travel-packages-service.

## Port

`8085`

## Database

`travel_hotel_db` — owned exclusively by this service. Tables: `hotel`,
`hotel_image`, `hotel_amenity`, `room`, `room_image`, `room_amenity`,
`hotel_booking`, `hotel_review`.

## Dependencies

- `spring-boot-starter-web`, `-data-jpa`, `-security`, `-validation`
- `spring-cloud-starter-netflix-eureka-client`
- `postgresql` driver
- `springdoc-openapi-starter-webmvc-ui`

## Roles

`ROLE_ADMIN` manages hotels, rooms, images, and amenities under
`/hotels/admin/**`. Any authenticated user can book a room and post a
review. Browsing (search, hotel details, reading reviews) is public — no
JWT required, matching how a hotel search page usually works before login.

## Domain

```
Hotel
 ├── HotelImage      (1:N)
 ├── HotelAmenity    (1:N)
 ├── Room            (1:N)
 │    ├── RoomImage    (1:N)
 │    └── RoomAmenity  (1:N)
 ├── HotelBooking    (1:N)
 └── HotelReview     (1:N)
```

## APIs

**Admin** (`/hotels/admin/**`, `ROLE_ADMIN`)

| Method | Path | Description |
|---|---|---|
| POST | `/hotels/admin` | Create a hotel. |
| PUT | `/hotels/admin/{hotelId}` | Update a hotel (partial — only non-null fields applied). |
| DELETE | `/hotels/admin/{hotelId}` | Delist a hotel (soft-delete, sets `active=false`). |
| POST | `/hotels/admin/{hotelId}/images` | Add a hotel image. |
| POST | `/hotels/admin/{hotelId}/amenities` | Add a hotel amenity. |
| POST | `/hotels/admin/{hotelId}/rooms` | Add a room type to a hotel. |
| PUT | `/hotels/admin/rooms/{roomId}` | Update a room (partial). |
| DELETE | `/hotels/admin/rooms/{roomId}` | Delete (delist) a room. |
| POST | `/hotels/admin/rooms/{roomId}/images` | Add a room image. |
| POST | `/hotels/admin/rooms/{roomId}/amenities` | Add a room amenity. |

**Public browsing** (`/hotels/**`, no auth required)

| Method | Path | Description |
|---|---|---|
| GET | `/hotels?destinationId=&checkIn=&checkOut=&guests=&minPrice=&maxPrice=&starRating=&roomType=&page=&size=` | Search hotels (paginated). |
| GET | `/hotels/{hotelId}` | Hotel details — rooms, amenities, images. |
| GET | `/hotels/{hotelId}/reviews` | All reviews for a hotel. |

**Bookings** (`/hotel-bookings/**`, authenticated)

| Method | Path | Description |
|---|---|---|
| POST | `/hotel-bookings` | Book a room — auto-confirms immediately. |
| DELETE | `/hotel-bookings/{bookingId}` | Cancel my booking. |
| GET | `/hotel-bookings/me` | My hotel booking history. |

**Reviews** (`POST /hotels/{hotelId}/reviews`, authenticated)

One review per user per hotel, enforced both in `HotelReviewServiceImpl`
and by a DB unique constraint on `(hotel_id, user_id)`.

### Sample hotel create request

```json
POST /hotels/admin
{
  "name": "Taj Exotica Resort",
  "description": "Beachfront resort with private villas.",
  "destinationId": "2f3b9a0e-....",
  "address": "Benaulim Beach Road",
  "city": "Benaulim",
  "state": "Goa",
  "country": "India",
  "starRating": 5,
  "contactEmail": "reservations@example.com",
  "checkInTime": "14:00:00",
  "checkOutTime": "11:00:00"
}
```

### Sample room create request

```json
POST /hotels/admin/{hotelId}/rooms
{
  "roomNumber": "TYPE-DELUXE",
  "roomType": "DELUXE",
  "capacity": 3,
  "pricePerNight": 8500.0,
  "totalRooms": 12,
  "description": "Garden-view room with king bed."
}
```

### Sample booking request

```json
POST /hotel-bookings
{
  "hotelId": "….",
  "roomId": "….",
  "checkInDate": "2026-08-10",
  "checkOutDate": "2026-08-13",
  "numberOfGuests": 2,
  "specialRequest": "High floor if possible"
}
```

## Business rules enforced

- Delisted hotels/rooms (`active=false`) cannot be booked.
- A room can only be booked while `availableRooms > 0`.
- `checkOutDate` must be after `checkInDate`.
- Cancelling a booking that isn't already `CANCELLED`/`CHECKED_OUT` returns
  the room to inventory.
- Only the user who made a booking can cancel it (403 otherwise).
- One review per hotel per user.

### Known simplification: `Room.availableRooms`

`availableRooms` is a single counter per room type, decremented on booking
and restored on cancellation — the same pattern `TravelPackage.availableSlots`
uses. It is **not** a date-indexed calendar: two bookings for
non-overlapping date ranges still compete for the same counter. `checkIn`,
`checkOut`, and `guests` are accepted by the search and booking APIs for
contract compatibility but don't yet drive true per-night availability. If
the business needs real overlapping-stay protection, replace this counter
with a date-range availability table — the DTO/API contracts don't need to
change.

## No payments yet

Bookings auto-confirm immediately (`bookingStatus=CONFIRMED`,
`paymentStatus=PENDING`) — there's no Payment Service integration yet, by
design (see the module's design notes). Once a Payment Service exists,
this is the natural place to flip booking creation to start `PENDING`
until payment succeeds.

## Notifications

Booking confirmation and cancellation each fire a call to
`notification-service`'s internal API (`NotificationClientImpl`) — always
in-app, plus email if the request carried an authenticated email address
(taken from the gateway's `X-Authenticated-Email` header, not looked up
separately). This call is `@Async` and wrapped in try-catch: if
notification-service is slow or down, the booking/cancellation still
succeeds — see `NotificationClientImpl`'s Javadoc for the full reasoning.

## Error responses

| Status | When |
|---|---|
| 400 | Validation failure, invalid dates, room not available, duplicate review |
| 403 | Caller is not the booking's owner on cancel |
| 404 | Hotel, room, or booking not found |

## Running locally

```bash
mvn spring-boot:run
```

Needs `travel_hotel_db` to exist first. Start after service-registry.

## Swagger UI

http://localhost:8085/swagger-ui/index.html
