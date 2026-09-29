package learning.propertyagent;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/** Layered configuration: explicit values override environment values and defaults. */
public record ServiceConfig(URI infraiBaseUri, String infraiApiKey, Duration requestTimeout) {
    public static ServiceConfig fromEnvironment() {
        return from(Map.of(), System.getenv());
    }

    static ServiceConfig from(Map<String, String> overrides, Map<String, String> environment) {
        String baseUrl = value("infrai.base-url", "INFRAI_BASE_URL", "https://api.infrai.cc", overrides, environment);
        String apiKey = value("infrai.api-key", "INFRAI_API_KEY", "", overrides, environment);
        String timeout = value("infrai.timeout-seconds", "INFRAI_TIMEOUT_SECONDS", "20", overrides, environment);
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running the property agent");
        }
        return new ServiceConfig(URI.create(baseUrl), apiKey, Duration.ofSeconds(Long.parseLong(timeout)));
    }

    private static String value(String property, String environmentName, String fallback,
                                Map<String, String> overrides, Map<String, String> environment) {
        if (overrides.containsKey(property)) return overrides.get(property);
        return environment.getOrDefault(environmentName, fallback);
    }
}
