-- Script de migration pour ajouter la colonne operating_system à la table user_sessions
-- À exécuter si la table existe déjà

-- Ajouter la colonne operating_system si elle n'existe pas déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'user_sessions' 
        AND column_name = 'operating_system'
    ) THEN
        ALTER TABLE user_sessions 
        ADD COLUMN operating_system VARCHAR(100);
        
        COMMENT ON COLUMN user_sessions.operating_system IS 'Système d''exploitation utilisé';
        
        RAISE NOTICE 'Colonne operating_system ajoutée avec succès';
    ELSE
        RAISE NOTICE 'La colonne operating_system existe déjà';
    END IF;
END $$;

