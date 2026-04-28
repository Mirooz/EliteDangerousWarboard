package be.mirooz.elitedangerous.backend.capi;

/**
 * Le backend CAPI a répondu {@code 418} (service indisponible / maintenance).
 */
public final class CapiServiceDownException extends RuntimeException {

    public CapiServiceDownException() {
        super("CAPI service down (HTTP 418)");
    }
}
