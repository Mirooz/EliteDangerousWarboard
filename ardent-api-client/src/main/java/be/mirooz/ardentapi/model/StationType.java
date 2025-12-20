package be.mirooz.ardentapi.model;

import lombok.Getter;

public enum StationType {
    CORIOLIS("coriolis.png"),
    PORT("port.png"),
    FLEET("fleet.png");

    @Getter
    private final String image;
    StationType(String image) {
        this.image=image;
    }
}
