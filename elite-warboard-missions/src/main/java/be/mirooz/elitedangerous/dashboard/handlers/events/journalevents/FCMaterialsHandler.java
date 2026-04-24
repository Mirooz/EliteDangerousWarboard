package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code FCMaterials} (schéma {@code fcmaterials_journal/1}). */
public class FCMaterialsHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "FCMaterials";
    }
}
