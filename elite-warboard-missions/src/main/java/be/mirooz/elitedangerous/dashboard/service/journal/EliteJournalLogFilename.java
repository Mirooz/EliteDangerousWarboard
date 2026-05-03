package be.mirooz.elitedangerous.dashboard.service.journal;

import java.util.regex.Pattern;

/**
 * Noms de fichiers journal Elite Dangerous : seuls les fichiers au format strict
 * {@code Journal.YYYY-MM-DDTHHmmss.N.log} (ex. {@code Journal.2026-05-03T201253.01.log}) sont pris en charge.
 */
public final class EliteJournalLogFilename {

    /**
     * {@code Journal.} + date ISO + {@code T} + heure sur 6 chiffres + {@code .} + suffixe numérique + {@code .log}
     */
    private static final Pattern CANONICAL =
            Pattern.compile("^Journal\\.\\d{4}-\\d{2}-\\d{2}T\\d{6}\\.\\d+\\.log$");

    private EliteJournalLogFilename() {}

    public static boolean matches(String filename) {
        return filename != null && CANONICAL.matcher(filename).matches();
    }
}
