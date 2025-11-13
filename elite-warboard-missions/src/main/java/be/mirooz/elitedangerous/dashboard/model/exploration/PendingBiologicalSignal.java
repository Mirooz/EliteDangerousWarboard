package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.Data;

/**
     * Classe interne pour représenter un signal biologique en attente.
     */
    @Data
    public class PendingBiologicalSignal {
        private final int bodyID;
        private final long systemAddress;
        private final String bodyName;
        private final int count;

        public PendingBiologicalSignal(int bodyID, long systemAddress, String bodyName, int count) {
            this.bodyID = bodyID;
            this.systemAddress = systemAddress;
            this.bodyName = bodyName;
            this.count = count;
        }
    }