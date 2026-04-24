package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code ApproachSettlement} (schéma {@code approachsettlement/1}). */
public class ApproachSettlementHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "ApproachSettlement";
    }
}
