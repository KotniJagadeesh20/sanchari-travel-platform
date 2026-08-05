# travel-packages-service

Two domain modules in one Spring Boot service — curated travel packages
(with itinerary, inclusions, and capacity-enforced booking) and destination
discovery (browse, search, filter destinations by category/budget/keyword).
They share a database and a single deployment because packages reference
destinations on nearly every read. See ARCHITECTURE.md — "Why destination
discovery lives inside travel-packages-service" — for the full reasoning.

## Port

`8084`

## Database

`travel_packages_db` — shared by both modules. Tables:
- Packages module: `travel_package`, `package_itinerary`, `package_booking`
- Destinations module: `destination`, `attraction`, `destination_activity`
- Shared: `user_ref` (UUID + cached email — see architecture note below)

## Dependencies

- `spring-boot-starter-web`, `-data-jpa`, `-security`, `-validation`
- `spring-cloud-starter-netflix-eureka-client`
- `postgresql` driver
- `jackson-databind` (`StringListConverter` for JSON column lists)
- `springdoc-openapi-starter-webmvc-ui`

## Roles

`ROLE_ADMIN` creates/manages packages and destinations.
`ROLE_USER` (any authenticated user) browses and books.
Enforced via `@PreAuthorize` + `SecurityConfig` path matchers for
`/packages/admin/**` and `/destinations/admin/**`.

---

## Module 1: Travel Packages

Admin-curated packages with inclusions, exclusions, places covered,
activities, image gallery, and a day-wise itinerary. Bookings auto-confirm
immediately (no approval step — see ride-share-service for the approval
flow model). Capacity is tracked via `availableSlots` on each package,
decremented on booking and returned on cancellation.

### Entity design note

`inclusions`, `exclusions`, `placesCovered`, `activities`, and `imageUrls`
are stored as JSON columns (backed by a `StringListConverter`) rather than
`@ElementCollection` join tables. `@ElementCollection` rewrites the entire
collection on every update; a JSON column is one row write regardless of
how many items changed. `PackageItinerary` is a real `@OneToMany` child
entity because it has structure (day number, ordering) that the flat string
lists don't.

`TravelPackage.destinationId` is now a **required** FK to `Destination`
(same database). Create a destination first via `POST /destinations/admin`,
then supply its UUID when creating a package. The free-text `destination`
string field has been removed — the display name comes from the linked
`Destination` record.

### Package APIs

**Traveler** (`/packages/**`, any authenticated user)

| Method | Path | Description |
|---|---|---|
| GET | `/packages` | Browse all listed (active) packages. |
| GET | `/packages/by-destination/{destinationId}` | Find packages linked to a destination UUID. |
| GET | `/packages/{packageId}` | Full package details (inclusions, itinerary, etc.). |
| POST | `/packages/{packageId}/book` | Book a package — auto-confirms, deducts slots. |
| GET | `/packages/bookings` | My booking history. |
| DELETE | `/packages/bookings/{bookingId}` | Cancel my booking — returns slots if confirmed. |

**Admin** (`/packages/admin/**`, `ROLE_ADMIN`)

| Method | Path | Description |
|---|---|---|
| POST | `/packages/admin` | Create a package (inclusions, itinerary in one call). |
| PUT | `/packages/admin/{packageId}` | Update (only non-null fields applied; itinerary replaced if provided). |
| DELETE | `/packages/admin/{packageId}` | Delist (soft-delete, sets `active=false`). |
| GET | `/packages/admin` | List all packages including delisted ones. |
| GET | `/packages/admin/{packageId}/bookings` | View all bookings on a package. |

### Sample package create request

```json
POST /packages/admin
{
  "title": "Goa Beach Getaway",
  "destination": "Goa",
  "durationDays": 4,
  "durationNights": 3,
  "price": 15999.0,
  "maxPeople": 20,
  "inclusions": ["Hotel Stay", "Breakfast", "Airport Pickup"],
  "exclusions": ["Flights", "Personal Expenses"],
  "placesCovered": ["Baga Beach", "Dudhsagar Falls"],
  "activities": ["Scuba Diving", "Parasailing"],
  "itinerary": [
    { "dayNumber": 1, "plan": "Arrival and hotel check-in" },
    { "dayNumber": 2, "plan": "North Goa beach tour" },
    { "dayNumber": 3, "plan": "Water sports at Baga" },
    { "dayNumber": 4, "plan": "Checkout and departure" }
  ]
}
```

### Booking request

```json
POST /packages/{packageId}/book
{ "travelersCount": 2 }
```

---

## Module 2: Destination Discovery

Answers "Where should I go?" — not a booking flow. Destinations have
categories (beach, hill station, adventure, etc.), attractions, activities,
budget info, and images. Used as the browsing/filter layer that packages,
hotels, and future trip-planning features will eventually link to.

### Destination APIs

**User** (`/destinations/**`, any authenticated user)

| Method | Path | Description |
|---|---|---|
| GET | `/destinations` | Browse all listed destinations (summary view). |
| GET | `/destinations/{destinationId}` | Full detail — includes attractions, activities, images. |
| GET | `/destinations/search?keyword=&category=&maxBudget=` | Search (keyword OR category OR budget — first non-null wins). |
| GET | `/destinations/category/{category}` | Filter by category enum value. |
| GET | `/destinations/popular` | Destinations ordered by rating (desc). |

**Admin** (`/destinations/admin/**`, `ROLE_ADMIN`)

| Method | Path | Description |
|---|---|---|
| POST | `/destinations/admin` | Create a destination with attractions and activities. |
| PUT | `/destinations/admin/{destinationId}` | Update (attractions/activities fully replaced if provided). |
| DELETE | `/destinations/admin/{destinationId}` | Delist (soft-delete, sets `active=false`). |

### Category values

`BEACH` · `HILL_STATION` · `ADVENTURE` · `RELIGIOUS` · `FAMILY` · `NATURE` · `ROAD_TRIP`

### Sample destination create request

```json
POST /destinations/admin
{
  "name": "Goa",
  "state": "Goa",
  "country": "India",
  "description": "Popular beach destination on India's west coast.",
  "bestTimeToVisit": "Nov-Feb",
  "averageBudget": 12000.0,
  "recommendedDays": 4,
  "category": "BEACH",
  "imageUrls": ["https://cdn.example.com/goa-beach.jpg"],
  "attractions": [
    { "name": "Baga Beach", "attractionType": "Beach", "description": "Lively beach known for water sports." }
  ],
  "activities": [
    { "name": "Scuba Diving", "category": "Water Sports" }
  ]
}
```

---

## Two response shapes for destinations

- **Summary** (`DestinationSummaryResponse`) — used by list/browse/search
  views. Includes core fields + first image URL as thumbnail. Does NOT
  include attractions/activities (avoids sending large lists when the
  frontend just needs cards).
- **Detail** (`DestinationDetailResponse`) — used by the detail page.
  Includes everything: attractions, activities, all images.

---

## Architecture notes

**`user_ref` stores UUID + cached email.** The email column is nullable
and has no uniqueness constraint — it's a display convenience, not an
authoritative identity source. If a user changes their email in auth-service,
the cached copy in `user_ref` may become stale. Domain services can call
`GET /auth/users/{id}` to get fresh user data when accuracy matters
(e.g. sending confirmation emails).

**`destinationId` is now required on packages.** Create a destination first,
then use its UUID when creating a package. This enables
`GET /packages/by-destination/{destinationId}` to work correctly and ensures
the full destination detail (category, budget, attractions) is always
accessible for any package.

## Business rules enforced

- Delisted packages (`active=false`) cannot be booked.
- `travelersCount` must not exceed `availableSlots` at booking time.
- Cancelling a CONFIRMED booking returns slots; cancelling anything else
  (e.g. already-CANCELLED) is rejected.
- Only the traveler who made a booking can cancel it (403 otherwise).

## Notifications

Package booking confirmation and cancellation each fire a call to
`notification-service`'s internal API (`NotificationClientImpl`) — in-app
always, plus email via the cached `UserRef.email`. The call is `@Async` and
wrapped in try-catch: if notification-service is slow or down, the booking
still confirms/cancels normally.

## Error responses

| Status | When |
|---|---|
| 400 | Validation failure, delisted package, insufficient slots |
| 403 | Caller is not the booking's owner on cancel |
| 404 | Package, booking, or destination not found |

## Running locally

```bash
mvn spring-boot:run
```

Needs `travel_packages_db` to exist first. Start after service-registry.

## Swagger UI

http://localhost:8084/swagger-ui/index.html

## Tests

**Packages:** `PackageServiceImplTest`, `PackageBookingServiceImplTest`,
`PackageControllerTest`, `PackageAdminControllerTest`, `UserRefServiceImplTest`,
`JwtValidatorTest`.

**Destinations:** `DestinationServiceImplTest`, `DestinationControllerTest`,
`DestinationAdminControllerTest`.
