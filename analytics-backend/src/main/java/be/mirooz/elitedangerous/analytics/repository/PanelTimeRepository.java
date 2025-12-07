package be.mirooz.elitedangerous.analytics.repository;

import be.mirooz.elitedangerous.analytics.model.PanelTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour les temps de panel
 */
@Repository
public interface PanelTimeRepository extends JpaRepository<PanelTime, Long> {

    /**
     * Trouve tous les temps de panel d'une session
     */
    List<PanelTime> findBySessionId(Long sessionId);

    /**
     * Calcule le temps total passé sur un panel pour un commandant
     */
    @Query("SELECT SUM(pt.durationSeconds) FROM PanelTime pt " +
           "JOIN pt.session s WHERE s.commanderName = :commanderName AND pt.panelName = :panelName")
    Long getTotalTimeByCommanderAndPanel(@Param("commanderName") String commanderName, 
                                         @Param("panelName") String panelName);

    /**
     * Trouve tous les temps de panel pour un panel spécifique
     */
    List<PanelTime> findByPanelName(String panelName);
}

