package be.mirooz.elitedangerous.dashboard;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;


public class JInputTest {
    public static void main(String[] args) {
         Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        System.out.println("🎮 Contrôleurs détectés : " + controllers.length);
        for (Controller c : controllers) {
            System.out.println("- " + c.getName() + " [" + c.getType() + "]");
        }
    }
}
