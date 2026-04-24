package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code Location} (schéma {@code journal/v1}). */
public class LocationHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "Location";
    }
}
