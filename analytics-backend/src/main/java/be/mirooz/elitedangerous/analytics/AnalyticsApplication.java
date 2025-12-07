package be.mirooz.elitedangerous.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application Spring Boot pour le module analytics
 * Cette application peut être démarrée de manière autonome ou intégrée dans l'application principale
 */
@SpringBootApplication
public class AnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }
}

