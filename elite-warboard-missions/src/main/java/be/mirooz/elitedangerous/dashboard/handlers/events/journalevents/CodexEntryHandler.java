package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code CodexEntry} (schéma {@code codexentry/1}). */
public class CodexEntryHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "CodexEntry";
    }
}
