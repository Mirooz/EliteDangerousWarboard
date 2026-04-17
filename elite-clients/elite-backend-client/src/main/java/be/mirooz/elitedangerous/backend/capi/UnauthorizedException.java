package be.mirooz.elitedangerous.backend.capi;

import be.mirooz.elitedangerous.backend.generated.model.CapiApiErrorBody;
import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {

    private final CapiApiErrorBody error;

    public UnauthorizedException(CapiApiErrorBody error) {
        super("Unauthorized (401)");
        this.error = error;
    }
}
