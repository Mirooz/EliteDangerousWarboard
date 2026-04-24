package be.mirooz.elitedangerous.dashboard.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Curseur de reprise : timestamp du dernier event journal dispatché et nom du fichier journal
 * contenant cet event. Permet à {@code JournalService.parseAllJournalFiles()} de sauter le
 * replay des fichiers et lignes déjà traités lors de la session précédente.
 *
 * <p>Le timestamp est au format ISO-8601 UTC ({@code 2026-04-24T13:54:12Z}) tel qu'écrit
 * par Elite Dangerous dans le journal — la comparaison lexicographique est équivalente à
 * la comparaison chronologique.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalCursor {

    /** Timestamp ISO du dernier event dispatché. */
    private String lastTimestamp;

    /** Nom de fichier (pas chemin absolu) du dernier journal dispatché. */
    private String lastJournalFile;
}
