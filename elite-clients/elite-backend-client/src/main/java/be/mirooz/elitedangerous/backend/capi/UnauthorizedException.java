package be.mirooz.elitedangerous.backend.capi;


import be.mirooz.elitedangerous.backend.generated.model.CapiApiResponse;
import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {

    private final CapiApiResponse response;

    public UnauthorizedException(CapiApiResponse response) {
        super("Unauthorized (401)");
        this.response = response;
    }

    public Object getData() {
        return response.getData();
    }

    public String getStatus() {
        return response.getStatus();
    }
}