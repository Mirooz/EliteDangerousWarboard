package be.mirooz.elitedangerous.capi.client;

import be.mirooz.elitedangerous.capi.generated.model.CapiApiResponse;

public class UnauthorizedException extends RuntimeException {

    private final CapiApiResponse response;

    public UnauthorizedException(CapiApiResponse response) {
        super("Unauthorized (401)");
        this.response = response;
    }

    public CapiApiResponse getResponse() {
        return response;
    }

    public Object getData() {
        return response.getData();
    }

    public String getStatus() {
        return response.getStatus();
    }
}