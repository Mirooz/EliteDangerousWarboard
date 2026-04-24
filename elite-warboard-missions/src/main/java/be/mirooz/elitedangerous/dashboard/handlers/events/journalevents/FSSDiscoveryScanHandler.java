package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code FSSDiscoveryScan} (schéma {@code fssdiscoveryscan/1}). */
public class FSSDiscoveryScanHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "FSSDiscoveryScan";
    }
}
