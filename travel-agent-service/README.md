# travel-agent-service

An AI travel assistant, built as a standalone Spring Boot service (not
folded into api-gateway) so it can be scaled, deployed, and rate-limited
independently of routing traffic. It owns no database of its own — it's a
pure orchestrator: it calls Anthropic's API directly (no SDK — see
`AnthropicClient`'s Javadoc for why) in a tool-use loop, where each tool
call forwards to one of the platform's existing search APIs
(destinations, packages, hotels, buses, rides) via Eureka-discovered
`RestTemplate` calls.

Routed through the Gateway at `/agent/**` like every other authenticated
endpoint — it is deliberately not exempt from the JWT requirement despite
being "just search," since Phase C (see Roadmap) will add booking
capability that must not be reachable without a real login.

## Port

`8088`

## Dependencies

- `spring-boot-starter-web`, `-security`, `-validation`, `-actuator`
- `spring-cloud-starter-netflix-eureka-client`,
  `spring-cloud-starter-loadbalancer` (`@LoadBalanced RestTemplate` — calls
  other services by name, e.g. `lb://hotel-service`, not a hardcoded host)
- `springdoc-openapi-starter-webmvc-ui`
- No database dependency — this service has none

## Configuration

Requires `ANTHROPIC_API_KEY` — no default, fails fast at startup if unset
(see `application.properties`). Optional: `ANTHROPIC_MODEL` (defaults to
`claude-sonnet-4-6` — confirm this is still current before deploying, model
names change over time) and `ANTHROPIC_MAX_TOKENS` (defaults to `2048`).

## Architecture

```
AgentController (POST /agent/chat)
 └── TravelAgentService — the tool-use loop, max 5 iterations
      ├── AnthropicClient — direct HTTP wrapper around /v1/messages
      ├── ToolRegistry — the 5 tool schemas offered to the model
      ├── ToolExecutor — dispatches a tool call to the right client
      │    ├── DestinationClient, PackageClient, HotelClient,
      │    │   BusClient, RideClient — RestTemplate wrappers,
      │    │   forward the caller's gateway-issued identity headers
      │    │   downstream (see AuthHeaders)
      │    └── ToolResultNormalizer — unwraps service-specific response
      │        shapes (e.g. search_hotels' Page.content, search_buses'
      │        wrapper.busses) into bare arrays before the model sees them
      └── system prompt forbids inventing data not returned by a tool call,
          and keeps booking out of scope for this phase
```

## API

**Chat** (`/agent/**`, authenticated)

| Method | Path | Description |
|---|---|---|
| POST | `/agent/chat` | Send the full conversation so far; get the assistant's final reply. |

Stateless — this service holds no conversation history of its own. The
client resends every prior message each turn.

### Sample request

```json
POST /agent/chat
{
  "messages": [
    { "role": "user", "content": "Find me a 3-star hotel in Goa under 5000/night" }
  ]
}
```

### Sample response

```json
{
  "reply": "I found a few options — Hotel Sea Breeze (₹4200/night, 3-star, Calangute) and..."
}
```

The model may call one or more of the 5 search tools internally before
replying (max 5 iterations); only the final text reply is returned to the
caller, not the intermediate tool calls.

## Roadmap

- **Phase A (current)** — search & recommendations only, read-only tools
  over the 5 existing search APIs. Code-complete, not yet verified
  end-to-end (see note below).
- **Phase B** — richer context (e.g. user's past bookings) fed into the
  system prompt.
- **Phase C** — booking capability. Will need its own careful review since
  it moves from "the model can look things up" to "the model can spend the
  user's money."

## Known limitation: not yet verified end-to-end

The unit tests (`ToolResultNormalizerTest`, `ToolExecutorTest`) and the
`@Disabled`-by-default `AgentChatSmokeTest` (a real end-to-end test hitting
real Anthropic + real domain services) exist in the test source but have
never actually been run — this service was scaffolded and code-reviewed in
an environment with no Maven Central or Docker access. Before trusting this
service, run locally:
```bash
mvn test                                    # unit tests
mvn verify                                  # + the 5 client integration tests, needs docker-compose up
# then remove @Disabled from AgentChatSmokeTest and run it directly —
# costs real Anthropic API usage, needs the full stack running
```

## Running locally

```bash
export ANTHROPIC_API_KEY=sk-ant-...
mvn spring-boot:run
```

Needs no database. Start after the services it calls (destinations,
packages, hotels, buses, rides) are up, or its tool calls will simply fail
for whichever service isn't reachable yet.

## Swagger UI

http://localhost:8088/swagger-ui/index.html

Only reachable when running via `mvn spring-boot:run` — under
`docker-compose`, this port isn't published to the host (see root
README's API documentation section).
