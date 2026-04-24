package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code Outfitting} (schéma {@code outfitting/2}, lit {@code Outfitting.json}). */
public class OutfittingHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "Outfitting";
    }
}
