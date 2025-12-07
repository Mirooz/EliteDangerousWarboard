package be.mirooz.elitedangerous.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * DTO pour la requête de fermeture de session
 * Contient les temps passés sur chaque panel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndSessionRequest {
    private Map<String, Long> panelTimes; // Map<panelName, durationSeconds>
}

