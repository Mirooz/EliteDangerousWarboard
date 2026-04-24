package be.mirooz.elitedangerous.dashboard.persistence;

/**
 * Contrat commun à tous les stores de registries persistés dans {@code ~/.elite-warboard/}.
 *
 * <p>Implémentation type : prend en dépendance le {@link java.nio.file.Path} du fichier cible,
 * et délègue la sérialisation à un Snapshot DTO dédié. Voir {@link CarrierStatusStore} comme
 * référence.</p>
 *
 * <p>Les stores sont enregistrés dans
 * {@code be.mirooz.elitedangerous.dashboard.service.persistence.PersistenceService}
 * qui orchestre {@link #loadIfExists()} / {@link #save()} / {@link #deleteIfExists()}
 * pour l'ensemble.</p>
 */
public interface RegistryStore {

    /** Identifiant court pour les logs ("carrier-status", "missions", ...). */
    String name();

    /** Persiste l'état courant du registry (création du dossier parent incluse). */
    void save();

    /**
     * Restaure l'état si le fichier existe.
     *
     * @return {@code true} si un fichier a été chargé avec succès, {@code false} s'il n'existait
     *         pas.
     * @throws IllegalStateException si le fichier existe mais ne peut pas être désérialisé.
     *         L'orchestrateur traite cette exception en effaçant les fichiers pour forcer un
     *         fallback sur le full-replay des journaux.
     */
    boolean loadIfExists();

    /** Supprime le fichier s'il existe (utilisé par {@code deleteAll}). */
    void deleteIfExists();
}
