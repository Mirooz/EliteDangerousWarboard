package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code Shipyard} (schéma {@code shipyard/2}, lit {@code Shipyard.json}). */
public class ShipyardHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "Shipyard";
    }
}
