package be.mirooz.elitedangerous.backend.spansh;

/**
 * Exception levée lorsque le GUID Spansh a expiré ou n'est plus valide.
 */
public class SpanshGuidExpiredException extends Exception {

    public SpanshGuidExpiredException(String message) {
        super(message);
    }

    public SpanshGuidExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
