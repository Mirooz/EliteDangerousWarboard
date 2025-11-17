package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.model.exploration.Position;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DirectionReaderService {

    public static final int WAIT_TIME = 300;
    // Position actuelle
    private final ObjectProperty<Position> currentPosition = new SimpleObjectProperty<>();

    private volatile Position previousPosition = null;
    private static final String STATUS_FILE = "Status.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public static DirectionReaderService INSTANCE = new DirectionReaderService();

    private DirectionReaderService() {
    }

    public static DirectionReaderService getInstance() {
        return INSTANCE;
    }

    @Getter
    private final List<Position> currentBiologicalSamplePositions = new ArrayList<>();
    private ScheduledFuture<?> statusWatcherTask;
    // Thread de surveillance
    private volatile boolean watching = false;

    /**
     * Lit le fichier Status.json à un moment donné et retourne la position actuelle
     *
     * @return La position actuelle ou null si le fichier n'existe pas ou si les données sont invalides
     */
    public Position readCurrentPosition(double radius) {
        try {
            String journalFolder = preferencesService.getJournalFolder();
            if (journalFolder == null || journalFolder.isEmpty()) {
                System.out.println("⚠️ Dossier journal non configuré");
                return null;
            }

            Path statusFilePath = Paths.get(journalFolder, STATUS_FILE);
            if (!Files.exists(statusFilePath)) {
                System.out.println("⚠️ Fichier Status.json non trouvé: " + statusFilePath);
                return null;
            }

            String statusContent = Files.readString(statusFilePath);
            if (statusContent == null || statusContent.trim().isEmpty()) {
                System.out.println("⚠️ Fichier Status.json vide");
                return null;
            }

            JsonNode statusNode = objectMapper.readTree(statusContent);

            // Vérifier que c'est bien un événement Status
            if (!"Status".equals(statusNode.path("event").asText())) {
                System.out.println("⚠️ Le fichier Status.json ne contient pas un événement Status");
                return null;
            }

            // Lire latitude et longitude (peuvent être absents si on n'est pas sur une planète)
            if (!statusNode.has("Latitude") || !statusNode.has("Longitude")) {
                return null; // Pas sur une planète
            }

            double latitude = statusNode.path("Latitude").asDouble();
            double longitude = statusNode.path("Longitude").asDouble();
            Integer heading = statusNode.has("Heading") ? statusNode.path("Heading").asInt() : null;
            String timestamp = statusNode.has("timestamp") ? statusNode.path("timestamp").asText() : null;

            return new Position(latitude, longitude, radius,heading, timestamp);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la lecture du fichier Status.json: " + e.getMessage());
            return null;
        }
    }

    private volatile long lastModified = 0;

    public void startWatchingStatusFile(double radius) {
        if (watching) {
            System.out.println("⚠️ La surveillance de Status.json est déjà active");
            return;
        }

        watching = true;

        // Lire la position initiale
        Position initialPosition = readCurrentPosition(radius);
        if (initialPosition != null) {
            currentPosition.set(initialPosition);
            previousPosition = initialPosition;
            System.out.println("[StatusWatcher] Position initiale: " + initialPosition);
        }

        System.out.println("[StatusWatcher] Démarrage de la surveillance de Status.json");

        statusWatcherTask = scheduler.scheduleAtFixedRate(() -> {

            try {
                if (!watching) return;

                String journalFolder = preferencesService.getJournalFolder();
                if (journalFolder == null || journalFolder.isEmpty()) {
                    return; // on attend le prochain tick
                }

                Path statusFilePath = Paths.get(journalFolder, STATUS_FILE);
                File statusFile = statusFilePath.toFile();

                if (!statusFile.exists()) {
                    return; // pas de fichier → rien à faire
                }

                long currentModified = statusFile.lastModified();

                // Fichier modifié ?
                if (currentModified != lastModified) {
                    lastModified = currentModified;

                    Position newPosition = readCurrentPosition(radius);

                    if (newPosition != null) {
                        // Vérifier si la position a changé
                        if (currentPosition.get() == null ||
                                newPosition.isDifferentFrom(currentPosition.get(), 0.000001)) {

                            previousPosition = currentPosition.get();
                            for (Position position : currentBiologicalSamplePositions) {
                                position.setDistanceFromCurrent(getDistanceTo(newPosition,position));
                            }
                            currentPosition.set(newPosition);

                            if (previousPosition != null) {
                                double direction = previousPosition.calculateDirectionTo(newPosition);
                                String directionName = getDirectionName(direction);

                                System.out.printf(
                                        "[StatusWatcher] Position mise à jour: %s → %s (Direction: %.1f° %s)%n",
                                        previousPosition, newPosition, direction, directionName
                                );
                            } else {
                                System.out.println("[StatusWatcher] Position initiale: " + newPosition);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("[StatusWatcher] Erreur lors de la surveillance: " + e.getMessage());
            }

        }, 300, WAIT_TIME, TimeUnit.MILLISECONDS); // WAIT_TIME = 500 ms
    }


    /**
     * Arrête la surveillance du fichier Status.json
     */
    public void stopWatchingStatusFile() {
        watching = false;
        if (statusWatcherTask != null){
            statusWatcherTask.cancel(true);
            statusWatcherTask = null;
        }
        currentPosition.set(null);
        previousPosition = null;
    }

    /**
     * Retourne la position actuelle (peut être null si pas encore lue ou si on n'est pas sur une planète)
     */
    public Position getCurrentPosition() {
        return currentPosition.getValue();
    }

    /**
     * Retourne la position précédente
     */
    public Position getPreviousPosition() {
        return previousPosition;
    }

    /**
     * Convertit un angle en degrés en nom de direction
     */
    private String getDirectionName(double degrees) {
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) Math.round(degrees / 22.5) % 16;
        return directions[index];
    }

    public double getDistanceTo(Position current,Position targetPosition) {
        if (current == null || targetPosition == null) {
            return -1;
        }

        return computeSurfaceDistanceMeters(
                targetPosition.getRadius(),
                current.getLatitude(), current.getLongitude(),
                targetPosition.getLatitude(), targetPosition.getLongitude()
        );
    }
    private double computeSurfaceDistanceMeters(
            double radiusMeters,
            double lat1, double lon1,
            double lat2, double lon2
    ) {
        double R = radiusMeters; // ton rayon est déjà en mètres

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
