package be.mirooz.elitedangerous.analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité représentant une session d'utilisation de l'application
 */
@Entity
@Table(name = "user_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "commander_name", nullable = false)
    private String commanderName;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "session_start", nullable = false)
    private LocalDateTime sessionStart;

    @Column(name = "session_end")
    private LocalDateTime sessionEnd;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PanelTime> panelTimes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (sessionStart == null) {
            sessionStart = LocalDateTime.now();
        }
    }

    /**
     * Calcule et met à jour la durée de la session en secondes
     */
    public void calculateDuration() {
        if (sessionStart != null && sessionEnd != null) {
            durationSeconds = java.time.Duration.between(sessionStart, sessionEnd).getSeconds();
        }
    }
}

