package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code DockingDenied} (schéma {@code dockingdenied/1}). */
public class DockingDeniedHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "DockingDenied";
    }
}
