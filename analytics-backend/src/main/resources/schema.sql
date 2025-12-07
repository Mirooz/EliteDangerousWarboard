-- Script SQL pour créer la base de données et les tables pour le module analytics
-- PostgreSQL

-- Créer la base de données (à exécuter manuellement en tant que superutilisateur)
-- CREATE DATABASE elite_warboard_analytics;
-- \c elite_warboard_analytics;

-- Créer l'utilisateur (optionnel, à exécuter en tant que superutilisateur)
-- CREATE USER elite_warboard WITH PASSWORD 'elite_warboard';
-- GRANT ALL PRIVILEGES ON DATABASE elite_warboard_analytics TO elite_warboard;

-- Table des sessions utilisateur
CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGSERIAL PRIMARY KEY,
    commander_name VARCHAR(255) NOT NULL,
    app_version VARCHAR(50),
    operating_system VARCHAR(100),
    session_start TIMESTAMP NOT NULL,
    session_end TIMESTAMP,
    duration_seconds BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des temps passés sur chaque panel
CREATE TABLE IF NOT EXISTS panel_times (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    panel_name VARCHAR(50) NOT NULL,
    duration_seconds BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES user_sessions(id) ON DELETE CASCADE
);

-- Index pour améliorer les performances des requêtes
CREATE INDEX IF NOT EXISTS idx_user_sessions_commander_name ON user_sessions(commander_name);
CREATE INDEX IF NOT EXISTS idx_user_sessions_session_start ON user_sessions(session_start);
CREATE INDEX IF NOT EXISTS idx_panel_times_session_id ON panel_times(session_id);
CREATE INDEX IF NOT EXISTS idx_panel_times_panel_name ON panel_times(panel_name);

-- Commentaires sur les tables
COMMENT ON TABLE user_sessions IS 'Stocke les sessions d''utilisation de l''application par commandant';
COMMENT ON TABLE panel_times IS 'Stocke le temps passé sur chaque panel pendant une session';

COMMENT ON COLUMN user_sessions.commander_name IS 'Nom du commandant Elite Dangerous';
COMMENT ON COLUMN user_sessions.app_version IS 'Version de l''application utilisée';
COMMENT ON COLUMN user_sessions.operating_system IS 'Système d''exploitation utilisé';
COMMENT ON COLUMN user_sessions.session_start IS 'Date et heure de début de la session';
COMMENT ON COLUMN user_sessions.session_end IS 'Date et heure de fin de la session';
COMMENT ON COLUMN user_sessions.duration_seconds IS 'Durée totale de la session en secondes';

COMMENT ON COLUMN panel_times.panel_name IS 'Nom du panel (Missions, Mining, Exploration)';
COMMENT ON COLUMN panel_times.duration_seconds IS 'Durée passée sur ce panel en secondes';

