package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code FSSSignalDiscovered} (schéma {@code fsssignaldiscovered/1}). */
public class FSSSignalDiscoveredHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "FSSSignalDiscovered";
    }
}
