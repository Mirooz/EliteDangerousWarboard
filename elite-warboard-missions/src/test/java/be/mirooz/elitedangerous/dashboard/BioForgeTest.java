package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.service.BioSpeciesService;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import org.junit.jupiter.api.Test;

import java.util.List;

public class BioForgeTest {
    @Test
    public void testGetSpecies() throws Exception {
        List<BioSpecies> bioSpecies  = BioSpeciesService.getInstance().getSpecies();
        bioSpecies.stream().filter(e -> e.getName().equals("Tubus")).forEach(
                e -> System.out.println(e.getFullName())
        );
    }
}
