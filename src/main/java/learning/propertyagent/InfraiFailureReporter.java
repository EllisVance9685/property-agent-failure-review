package learning.propertyagent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiFailureReporter implements FailureReporter {
    private static final String CAPTURE_PATH = "/v1/errors/capture";
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
    private static final Pattern MESSAGE = Pattern.compile("\\\"message\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");

    private final ServiceConfig config;
    private final HttpClient http;
    private final Sleeper sleeper;

    public InfraiFailureReporter(ServiceConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build(), Thread::sleep);
    }

    InfraiFailureReporter(ServiceConfig config, HttpClient http, Sleeper sleeper) {
        this.config = config;
        this.http = http;
        this.sleeper = sleeper;
    }

    /** Canonical call: infrai.errors.capture */
    @Override
    public void capture(PropertyWork.MaintenanceRequest request, String step, Exception exception)
            throws IOException, InterruptedException {
        String idempotencyKey = "property-agent:" + request.requestId() + ":" + step;
        String payload = exceptionPayload(request, step, exception);
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest outgoing = HttpRequest.newBuilder(config.infraiBaseUri().resolve(CAPTURE_PATH))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + config.infraiApiKey())
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = http.send(outgoing, HttpResponse.BodyHandlers.ofString());
            Envelope envelope = decodeEnvelope(response.body(), response.statusCode());
            if (!envelope.ok()) {
                if (response.statusCode() == 429 && attempt < 3) {
                    sleeper.sleep(retryDelay(response, attempt).toMillis());
                    continue;
                }
                throw new InfraiError(envelope.code(), envelope.message(), response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport response: HTTP " + response.statusCode());
            }
            return;
        }
        throw new IOException("Capture retry budget exhausted");
    }

    static Envelope decodeEnvelope(String body, int statusCode) throws IOException {
        Matcher ok = OK.matcher(body);
        if (!ok.find()) throw new IOException("Response was not an Infrai envelope (HTTP " + statusCode + ")");
        boolean accepted = Boolean.parseBoolean(ok.group(1));
        if (accepted) return new Envelope(true, "", "");
        return new Envelope(false, field(CODE, body, "REQUEST_REJECTED"), field(MESSAGE, body, "Request rejected"));
    }

    private static String field(Pattern pattern, String body, String fallback) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : fallback;
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        String value = response.headers().firstValue("Retry-After").orElse("").trim();
        try {
            if (!value.isEmpty()) return Duration.ofSeconds(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            try {
                Duration delay = Duration.between(ZonedDateTime.now(),
                        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME));
                if (!delay.isNegative()) return delay;
            } catch (DateTimeParseException ignoredDate) {
                // Fall through to bounded exponential backoff.
            }
        }
        return Duration.ofMillis(250L * (1L << attempt));
    }

    private static String exceptionPayload(PropertyWork.MaintenanceRequest request, String step, Exception failure) {
        return "{\"message\":\"" + json("Property agent step failed")
                + "\",\"level\":\"error\",\"fingerprint\":[\"property-agent\",\"" + json(step)
                + "\"],\"exception\":{\"type\":\"" + json(failure.getClass().getSimpleName())
                + "\",\"message\":\"" + json(failure.getMessage())
                + "\"},\"context\":{\"request_id\":\"" + json(request.requestId())
                + "\",\"unit_id\":\"" + json(request.unitId()) + "\",\"step\":\"" + json(step) + "\"}}";
    }

    private static String json(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    record Envelope(boolean ok, String code, String message) {}
    interface Sleeper { void sleep(long milliseconds) throws InterruptedException; }
}
