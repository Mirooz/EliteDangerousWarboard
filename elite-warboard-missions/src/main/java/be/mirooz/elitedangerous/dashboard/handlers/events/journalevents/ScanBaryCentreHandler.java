package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code ScanBaryCentre} (schéma {@code scanbarycentre/1}). */
public class ScanBaryCentreHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "ScanBaryCentre";
    }
}
