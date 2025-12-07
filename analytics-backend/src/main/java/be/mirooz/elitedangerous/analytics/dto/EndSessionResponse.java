package be.mirooz.elitedangerous.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse de fermeture de session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndSessionResponse {
    private Long sessionId;
    private Long durationSeconds;
    private String message;
}

