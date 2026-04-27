package be.mirooz.elitedangerous.dashboard.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Curseur de reprise : timestamp du dernier event journal dispatché, nom du fichier journal
 * contenant cet event, et numéro de ligne physique (1-based) de cette ligne dans le fichier.
 * Permet à {@code JournalService.parseAllJournalFiles()} de sauter le replay des lignes déjà
 * traitées lors de la session précédente.
 *
 * <p>Les journaux Elite ne sont jamais réécrits en arrière : la reprise préfère
 * {@code lastLineNumber} sur le fichier {@code lastJournalFile}. Si {@code lastLineNumber}
 * est absent (curseurs JSON anciens), on retombe sur la comparaison lexicographique du
 * timestamp ISO-8601 UTC.</p>
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

    /**
     * Dernière ligne physique dispatchée dans {@code lastJournalFile} (1 = première ligne).
     * {@code null} si curseur hérité sans ce champ.
     */
    private Integer lastLineNumber;
}
