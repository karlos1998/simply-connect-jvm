package ovh.simplyconnect.client;

/** An HTTP or transport error returned while calling Simply Connect. */
public final class SimplyConnectException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int statusCode;
    private final String correlationId;
    private final String responseBody;

    public SimplyConnectException(String message, int statusCode, String correlationId, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.correlationId = correlationId;
        this.responseBody = responseBody;
    }

    public SimplyConnectException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.correlationId = null;
        this.responseBody = null;
    }

    public int statusCode() {
        return statusCode;
    }

    public String correlationId() {
        return correlationId;
    }

    public String responseBody() {
        return responseBody;
    }
}
