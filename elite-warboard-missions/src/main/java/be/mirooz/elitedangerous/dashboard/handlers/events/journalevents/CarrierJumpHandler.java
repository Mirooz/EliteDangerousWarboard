package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

public class CarrierJumpHandler extends AbstractJumpHandler {

    @Override
    public String getEventType() {
        return "CarrierJump";
    }

    @Override
    protected String getJumpLabel() {
        return "Carrier Jump";
    }
}

