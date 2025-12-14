package be.mirooz.elitedangerous.dashboard.service.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse contenant la dernière version de l'application
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatestVersionResponse {
    private String tagName;
    private String name;
    private String publishedAt;
    private String htmlUrl;
    private String body;
}

