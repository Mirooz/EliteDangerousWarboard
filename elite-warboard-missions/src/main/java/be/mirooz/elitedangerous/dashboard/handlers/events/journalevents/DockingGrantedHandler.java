package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code DockingGranted} (schéma {@code dockinggranted/1}). */
public class DockingGrantedHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "DockingGranted";
    }
}
