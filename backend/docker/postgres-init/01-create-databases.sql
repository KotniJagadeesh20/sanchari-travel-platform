-- Postgres' official image only creates the single database named by
-- POSTGRES_DB on first boot. This platform needs five separate databases
-- (one per service, matching the "each service owns its data" rule in
-- ARCHITECTURE.md), so this init script — mounted into
-- /docker-entrypoint-initdb.d/ — creates the remaining four on container
-- first-start. It only runs once, against a fresh, empty data volume.

CREATE DATABASE travel_auth_db;
CREATE DATABASE travel_bus_booking_db;
CREATE DATABASE travel_rideshare_db;
CREATE DATABASE travel_packages_db;
CREATE DATABASE travel_hotel_db;
CREATE DATABASE travel_notification_db;

-- travel_packages_db doubles as the database for the destinations module
-- (same Spring Boot app, same schema) — see ARCHITECTURE.md, "Why
-- destination discovery lives inside travel-packages-service".

-- travel_hotel_db belongs to hotel-service, a fully independent bounded
-- context (hotels, rooms, hotel bookings, reviews) that references
-- destinations and users only by UUID — no shared schema with any other service.

-- travel_notification_db belongs to notification-service — in-app +
-- email notifications, populated by other services calling its internal
-- (non-gateway-routed) API. No FK relationships to any other service's data.
