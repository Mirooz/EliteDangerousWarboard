package be.mirooz.elitedangerous.dashboard;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import org.junit.jupiter.api.Test;


public class JInputTest {
    @Test
    public void testGetController() throws Exception {
         Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        System.out.println("🎮 Contrôleurs détectés : " + controllers.length);
        for (Controller c : controllers) {
            System.out.println("- " + c.getName() + " [" + c.getType() + "]");
        }
    }
}
