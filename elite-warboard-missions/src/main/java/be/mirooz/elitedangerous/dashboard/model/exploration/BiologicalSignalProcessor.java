package be.mirooz.elitedangerous.dashboard.model.exploration;

import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Classe pour gérer les signaux biologiques en attente.
 * Vérifie périodiquement si les planètes correspondantes sont dans le registre
 * et applique le calcul biologique quand elles sont disponibles.
 * Le scheduler ne démarre que lorsqu'il y a des signaux en attente.
 */
@Data
public class BiologicalSignalProcessor {

    private static final BiologicalSignalProcessor INSTANCE = new BiologicalSignalProcessor();

    private static final long CHECK_INTERVAL_MS = 300; // Vérifie toutes les 1 secondes

    private final Map<String,PendingBiologicalSignal> pendingSignals = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "BiologicalSignalProcessor");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> scheduledTask = null;

    private BiologicalSignalProcessor() {
        // Ne démarre pas le scheduler au constructeur
    }

    public static BiologicalSignalProcessor getInstance() {
        return INSTANCE;
    }

    /**
     * Ajoute un signal biologique en attente (niveau 1 - FSSBodySignals).
     * Démarre le scheduler si ce n'est pas déjà fait.
     */
    public synchronized void addPendingBiologicalSignal(int bodyID, String bodyName, int count, int level) {
        addPendingBiologicalSignal(bodyID, bodyName, count, level, null);
    }

    /**
     * Ajoute un signal biologique en attente avec genuses (niveau 2 - SAASignalsFound).
     * Démarre le scheduler si ce n'est pas déjà fait.
     */
    public synchronized void addPendingBiologicalSignal(int bodyID, String bodyName, int count, int level, List<String> genuses) {
        PendingBiologicalSignal signal = new PendingBiologicalSignal(bodyID, bodyName, count, level, genuses);

        System.out.printf("📋 Signal biologique (niveau %d) ajouté à la file d'attente: BodyID=%d, BodyName=%s%n", level, bodyID, bodyName);
        // Démarrer le scheduler si ce n'est pas déjà fait
        //startProcessingIfNeeded();
        checkIfPlanetInRegistry(signal);
    }
    public void clear() {
        pendingSignals.clear();
    }

    /**
     * Arrête le scheduler si la liste est vide.
     */
    private void stopProcessingIfEmpty() {
        if (pendingSignals.isEmpty() && scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
            scheduledTask = null;
        }
    }

    /**
     * Vérifie périodiquement si les planètes sont dans le registre
     * et applique calcBiological() quand elles sont disponibles.
     */
    private synchronized void checkIfPlanetInRegistry(PendingBiologicalSignal signal) {
        PlaneteRegistry registry = PlaneteRegistry.getInstance();
        registry.getByBodyID(signal.getBodyID())
                .filter(body -> body instanceof PlaneteDetail)
                .map(body -> (PlaneteDetail) body)
                .ifPresentOrElse(planete -> {
                            // Appliquer le calcul biologique avec le niveau et les genuses
                            planete.calculBioScan(signal.getCount(), signal.getLevel(), signal.getGenuses());
                            System.out.printf("✅ Calcul biologique (niveau %d) appliqué pour: %s (BodyID: %d)%n",
                                    signal.getLevel(), signal.getBodyName(), signal.getBodyID());
                        },
                        () -> {
                            pendingSignals.put(signal.bodyName, signal);
                        });
    }

    public void startProcessingIfPresent(String boydName) {
        if (pendingSignals.containsKey(boydName)) {
            checkIfPlanetInRegistry(pendingSignals.get(boydName));
        }
    }
    /**
     * Arrête le processeur (utile pour les tests ou l'arrêt de l'application).
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * Classe interne pour représenter un signal biologique en attente.
     */
    @Data
    private static class PendingBiologicalSignal {
        private final int bodyID;
        private final String bodyName;
        private final int count;
        private final int level; // 1 pour FSSBodySignals, 2 pour SAASignalsFound
        private final List<String> genuses; // null pour level 1, liste des genuses pour level 2

        public PendingBiologicalSignal(int bodyID, String bodyName, int count, int level, List<String> genuses) {
            this.bodyID = bodyID;
            this.bodyName = bodyName;
            this.count = count;
            this.level = level;
            this.genuses = genuses != null ? new ArrayList<>(genuses) : null;
        }
    }
}

