package be.mirooz.elitedangerous.analytics.repository;

import be.mirooz.elitedangerous.analytics.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour les sessions utilisateur
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /**
     * Trouve toutes les sessions d'un commandant
     */
    List<UserSession> findByCommanderNameOrderBySessionStartDesc(String commanderName);

    /**
     * Trouve la dernière session d'un commandant
     */
    UserSession findFirstByCommanderNameOrderBySessionStartDesc(String commanderName);
}

