package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

/** Relais EDDN pour l'event {@code NavBeaconScan} (schéma {@code navbeaconscan/1}). */
public class NavBeaconScanHandler extends AbstractEddnOnlyJournalEventHandler {
    @Override
    public String getEventType() {
        return "NavBeaconScan";
    }
}
