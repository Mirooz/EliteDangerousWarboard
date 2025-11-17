package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.Getter;
import lombok.Setter;

/**
 * Classe pour représenter une position (latitude, longitude, heading)
 */
public class Position {
    private final double latitude;
    private final double longitude;
    @Getter
    private final double radius;
    private final Integer heading;
    private final String timestamp;
    @Setter
    @Getter
    private double distanceFromCurrent;

    public Position(double latitude, double longitude, double radius, Integer heading, String timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.heading = heading;
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Integer getHeading() {
        return heading;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("Position{lat=%.6f, lon=%.6f, heading=%s, timestamp=%s}",
                latitude, longitude, heading, timestamp);
    }

    /**
     * Calcule la direction (en degrés) depuis cette position vers une autre position
     *
     * @param to Position de destination
     * @return Direction en degrés (0-360, où 0 = Nord, 90 = Est, 180 = Sud, 270 = Ouest)
     */
    public double calculateDirectionTo(Position to) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(to.latitude);
        double deltaLon = Math.toRadians(to.longitude - this.longitude);

        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360; // Normaliser entre 0 et 360
    }

    /**
     * Vérifie si cette position est différente d'une autre (avec une tolérance)
     */
    public boolean isDifferentFrom(Position other, double tolerance) {
        if (other == null) return true;
        return Math.abs(this.latitude - other.latitude) > tolerance ||
                Math.abs(this.longitude - other.longitude) > tolerance;
    }
}
