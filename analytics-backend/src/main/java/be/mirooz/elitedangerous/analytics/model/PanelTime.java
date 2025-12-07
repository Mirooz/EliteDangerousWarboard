package be.mirooz.elitedangerous.analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité représentant le temps passé sur un panel spécifique pendant une session
 */
@Entity
@Table(name = "panel_times")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanelTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private UserSession session;

    @Column(name = "panel_name", nullable = false, length = 50)
    private String panelName;

    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds;

    /**
     * Enumération des panels disponibles dans l'application
     */
    public enum Panel {
        MISSIONS("Missions"),
        MINING("Mining"),
        EXPLORATION("Exploration");

        private final String displayName;

        Panel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}

