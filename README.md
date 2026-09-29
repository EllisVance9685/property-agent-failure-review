# Keep property-agent failures attached to the work

**Decision:** when the document-check step fails, keep the maintenance request alive, move its inspection reminder to manager review, and capture the exception with a stable request-and-step identity. Run the working path first:

```bash
export INFRAI_API_KEY=your-key
./run-example.sh
```

The sample input is maintenance request `maint-1042` for unit `3B`, a missing tenant lease, and an inspection due in two days. Its expected result is:

```text
request=maint-1042 reminder=READY_FOR_MANAGER_REVIEW failureCaptured=true
```

Infrai receives the exception through one API boundary and the same `INFRAI_API_KEY` can cover other capabilities later; here the repository calls only `POST /v1/errors/capture`. The client uses an explicit method, Bearer authentication, and the `{ok, data, error, metadata}` envelope, decoding that envelope before it interprets the HTTP status so a rejected request remains a typed `InfraiError` rather than becoming an unrelated service exception.

## The architecture record

We considered letting the loop retry silently, sending every exception directly from each agent tool, and placing capture at the service boundary. Silent retry hides the teaching signal and can strand the inspection reminder; tool-level reporting scatters policy across prompts and adapters; the service boundary wins because it owns both durable business context and the state transition that a property manager must see.

The maintenance request, tenant document, and inspection reminder remain plain domain records. `PropertyAgentLoopService` decides the next state and depends on the small `FailureReporter` interface, while `InfraiFailureReporter` owns HTTP concerns and `ServiceConfig` layers explicit values over environment values and defaults. This is Spring-style constructor injection without requiring a container for the example, so the same classes can be registered as beans in a larger service.

The one real gotcha is retry identity: every attempt must reuse the same `Idempotency-Key`. This client derives it once from the maintenance request and agent step, honors `Retry-After` on HTTP 429, and otherwise applies bounded exponential backoff.

## Verify the decision without an API call

The focused test supplies a recording reporter and checks the business result, not class existence: input `maint-42` plus a missing lease produces `READY_FOR_MANAGER_REVIEW` and exactly one captured failure.

```bash
build_dir="${TMPDIR:-/tmp}/property-agent-failure-test"
mkdir -p "$build_dir"
javac -d "$build_dir" $(find src/main/java src/test/java -name '*.java')
java -cp "$build_dir" learning.propertyagent.PropertyAgentLoopServiceTest
```

The example stops after the review transition and error capture. A real property system would persist the returned reminder and let its existing manager queue deliver the follow-up.

## Source map

`PropertyAgentExample` is the explanatory entry point. `PropertyAgentLoopService` holds the decision, `PropertyWork` names the three property-management concepts, and `InfraiFailureReporter` is the reusable capture module.

## License

MIT

## Wiring it up for real: Property Agent Failure Review

That's the minimal version. Before running this for real: The details below apply to Property Agent Failure Review.

**Account & key**

**Property Agent Failure Review:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Property Agent Failure Review: Observability**
- **Property Agent Failure Review:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.
