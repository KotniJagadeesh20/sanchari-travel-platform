# service-registry

Eureka server — service discovery for the platform. Every other service
registers itself here on startup; the API Gateway and any future
service-to-service calls resolve service names (`auth-service`,
`bus-booking-service`, etc.) through this registry instead of hardcoded
hostnames/ports.

## Port

`8761`

## Dependencies

- `spring-cloud-starter-netflix-eureka-server`
- `spring-boot-starter-actuator` (healthcheck endpoint for Docker)

## Configuration

| Property | Default | Override (env var) |
|---|---|---|
| `eureka.instance.hostname` | `localhost` | `EUREKA_HOSTNAME` |

This is the **only** service in the platform that doesn't itself register
with Eureka (`register-with-eureka: false`, `fetch-registry: false`) — it
*is* the registry.

## Running locally

```bash
mvn spring-boot:run
```

Must be the **first** service started — everything else fails to register
(though doesn't necessarily fail to start) without it running.

## Verifying it's up

Open `http://localhost:8761` in a browser — the Eureka dashboard lists every
currently-registered service instance. After the full platform is up, you
should see all 5 other services listed under "Instances currently registered
with Eureka."

## APIs

None — this is infrastructure, not a domain service. No Swagger UI, no
business endpoints. The dashboard above and `/eureka/apps` (Eureka's own
REST API, used internally by clients) are the only HTTP surfaces.
