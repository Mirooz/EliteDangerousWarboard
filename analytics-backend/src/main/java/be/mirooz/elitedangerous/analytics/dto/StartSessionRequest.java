package be.mirooz.elitedangerous.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête de démarrage de session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionRequest {
    private String commanderName;
    private String appVersion;
    private String operatingSystem;
}

