package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code FSSAllBodiesFound} (schéma {@code fssallbodiesfound/1}). */
public class FSSAllBodiesFoundHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "FSSAllBodiesFound";
    }
}
