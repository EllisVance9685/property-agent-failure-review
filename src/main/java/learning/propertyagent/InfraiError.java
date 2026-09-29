package learning.propertyagent;

import java.io.IOException;

public final class InfraiError extends IOException {
    private static final long serialVersionUID = 1L;
    private final String code;
    private final int statusCode;

    public InfraiError(String code, String message, int statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public String code() { return code; }
    public int statusCode() { return statusCode; }
}
