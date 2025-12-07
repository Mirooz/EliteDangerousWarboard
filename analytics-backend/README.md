# Analytics Backend Module

Module backend pour le suivi de l'utilisation de l'application Elite Warboard.

**Technologie** : Spring Boot 3.2 avec Spring Data JPA

## Fonctionnalités

- **Suivi des sessions** : Enregistre l'ouverture et la fermeture de l'application avec le nom du commandant
- **Suivi du temps par panel** : Enregistre le temps passé sur chaque panel (Missions, Mining, Exploration)

## Configuration

### Base de données PostgreSQL

1. **Créer la base de données** :
```sql
CREATE DATABASE elite_warboard_analytics;
```

2. **Créer l'utilisateur** (optionnel) :
```sql
CREATE USER elite_warboard WITH PASSWORD 'elite_warboard';
GRANT ALL PRIVILEGES ON DATABASE elite_warboard_analytics TO elite_warboard;
```

3. **Exécuter le script SQL** :
```bash
psql -U elite_warboard -d elite_warboard_analytics -f src/main/resources/schema.sql
```

### Configuration Spring Boot

Le module utilise Spring Boot avec `application.properties`. Les paramètres peuvent être configurés via :

1. **Variables d'environnement** :
   - `ANALYTICS_DB_URL` : URL de connexion (défaut: `jdbc:postgresql://localhost:5432/elite_warboard_analytics`)
   - `ANALYTICS_DB_USER` : Nom d'utilisateur (défaut: `elite_warboard`)
   - `ANALYTICS_DB_PASSWORD` : Mot de passe (défaut: `elite_warboard`)

2. **Fichier `application.properties`** dans `src/main/resources/`

3. **Propriétés système Java** au démarrage

La configuration utilise HikariCP pour le pool de connexions (inclus dans Spring Boot).

## Structure de la base de données

### Table `user_sessions`
Stocke les sessions d'utilisation de l'application.

| Colonne | Type | Description |
|---------|------|-------------|
| id | BIGSERIAL | Identifiant unique de la session |
| commander_name | VARCHAR(255) | Nom du commandant Elite Dangerous |
| app_version | VARCHAR(50) | Version de l'application utilisée |
| session_start | TIMESTAMP | Date et heure de début de la session |
| session_end | TIMESTAMP | Date et heure de fin de la session |
| duration_seconds | BIGINT | Durée totale de la session en secondes |

### Table `panel_times`
Stocke le temps passé sur chaque panel pendant une session.

| Colonne | Type | Description |
|---------|------|-------------|
| id | BIGSERIAL | Identifiant unique |
| session_id | BIGINT | Référence à la session (FK) |
| panel_name | VARCHAR(50) | Nom du panel (Missions, Mining, Exploration) |
| duration_seconds | BIGINT | Durée passée sur ce panel en secondes |

## Utilisation

### Démarrage du backend

Le backend Spring Boot doit être démarré séparément :

```bash
cd analytics-backend
mvn spring-boot:run
```

Ou depuis l'application principale :
```bash
java -jar analytics-backend-1.2.0-SNAPSHOT.jar
```

Le serveur démarre sur le port 8080 par défaut (configurable via `application.properties`).

### Appels depuis l'application cliente

L'application cliente utilise `AnalyticsClient` pour appeler le backend via HTTP :

1. **Au démarrage de l'application** : 
   - POST `/api/analytics/sessions/start` avec `commanderName` et `appVersion`
   - Retourne un `sessionId`

2. **Lors du changement de panel** : 
   - Les temps sont stockés en mémoire (pas d'appel HTTP)

3. **À la fermeture de l'application** : 
   - POST `/api/analytics/sessions/{sessionId}/end` avec les `panelTimes` accumulés
   - Tous les temps de panel sont envoyés en une seule requête

### Configuration du client

L'URL du backend peut être configurée via :
- Variable d'environnement : `ANALYTICS_BACKEND_URL` (défaut: `http://localhost:8080`)
- Propriété système : `analytics.backend.url`

## Exemple de requêtes SQL

### Sessions par commandant
```sql
SELECT commander_name, COUNT(*) as nb_sessions, 
       SUM(duration_seconds) as total_seconds
FROM user_sessions
GROUP BY commander_name
ORDER BY nb_sessions DESC;
```

### Temps passé par panel
```sql
SELECT panel_name, SUM(duration_seconds) as total_seconds,
       COUNT(*) as nb_visites
FROM panel_times
GROUP BY panel_name
ORDER BY total_seconds DESC;
```

### Détails d'une session
```sql
SELECT us.commander_name, us.session_start, us.duration_seconds,
       pt.panel_name, pt.duration_seconds
FROM user_sessions us
LEFT JOIN panel_times pt ON us.id = pt.session_id
WHERE us.id = 1
ORDER BY pt.id;
```

