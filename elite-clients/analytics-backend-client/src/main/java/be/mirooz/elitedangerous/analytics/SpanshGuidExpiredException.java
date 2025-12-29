package be.mirooz.elitedangerous.analytics;

/**
 * Exception levée lorsque le GUID Spansh a expiré ou n'est plus valide.
 * Dans ce cas, il faut réinitialiser le GUID et faire une nouvelle demande.
 */
public class SpanshGuidExpiredException extends Exception {
    
    public SpanshGuidExpiredException(String message) {
        super(message);
    }
    
    public SpanshGuidExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}

