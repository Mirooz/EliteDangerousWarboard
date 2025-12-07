package be.mirooz.elitedangerous.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse de démarrage de session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionResponse {
    private Long sessionId;
    private String message;
}

