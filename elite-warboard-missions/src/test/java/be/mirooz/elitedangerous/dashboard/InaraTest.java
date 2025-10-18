package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.service.EdToolsService;
import be.mirooz.elitedangerous.dashboard.service.InaraService;
import be.mirooz.elitedangerous.dashboard.service.RouteService;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class InaraTest {

    private static final InaraService inaraService = InaraService.getInstance();
    private static final RouteService routeService = RouteService.getInstance();
    private static final EdToolsService edtoolsService=EdToolsService.getInstance();

}
