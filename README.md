# Elite Dangerous Dashboard

Un dashboard JavaFX pour afficher les missions en cours dans Elite Dangerous, avec un style visuel authentique du jeu.

## Fonctionnalités

- **Interface style Elite Dangerous** : Thème visuel avec les couleurs orange/cyan caractéristiques du jeu
- **Affichage des missions** : Liste des missions actives avec détails complets
- **Types de missions supportés** : Transport, chasse aux primes, exploration, etc.
- **Statistiques en temps réel** : Nombre de missions, récompenses totales, factions
- **Barres de progression** : Suivi de l'avancement des missions
- **Interface responsive** : Adaptation à différentes tailles d'écran

## Technologies utilisées

- **Java 17** : Langage de programmation
- **JavaFX 21** : Framework d'interface utilisateur
- **Maven** : Gestion des dépendances
- **CSS** : Styles personnalisés pour le thème Elite Dangerous

## Structure du projet

```
elite-dashboard/
├── src/main/java/com/elitedangerous/dashboard/
│   ├── EliteDashboardApp.java          # Application principale
│   ├── controller/
│   │   └── DashboardController.java    # Contrôleur de l'interface
│   ├── model/
│   │   ├── Mission.java               # Modèle de données Mission
│   │   ├── MissionType.java           # Types de missions
│   │   └── MissionStatus.java         # Statuts des missions
│   └── service/
│       └── MissionService.java        # Service de gestion des missions
├── src/main/resources/
│   ├── fxml/
│   │   └── dashboard.fxml             # Interface utilisateur
│   ├── css/
│   │   └── elite-theme.css            # Thème visuel Elite Dangerous
│   └── images/                        # Ressources graphiques
└── pom.xml                           # Configuration Maven
```

## Installation et exécution

### Prérequis

- Java 17 ou supérieur
- Maven 3.6 ou supérieur

### Compilation et exécution

1. **Cloner le projet** :
   ```bash
   git clone <repository-url>
   cd elite-dashboard
   ```

2. **Compiler le projet** :
   ```bash
   mvn clean compile
   ```

3. **Exécuter l'application** :
   ```bash
   mvn javafx:run
   ```

### Alternative avec JAR exécutable

1. **Créer le JAR** :
   ```bash
   mvn clean package
   ```

2. **Exécuter le JAR** :
   ```bash
   java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -jar target/elite-dashboard-1.0.0.jar
   ```

## Types de missions supportés

- **Transport** : Courrier, livraison, passagers
- **Combat** : Assassinat, chasse aux primes, massacre
- **Exploration** : Scan, exploration, récupération
- **Commerce** : Trading, contrebande, mining

## Personnalisation

### Couleurs du thème

Le thème Elite Dangerous utilise les couleurs suivantes :
- **Orange** : `#FF6B00` - Couleur principale
- **Cyan** : `#00FFFF` - Couleur d'accent
- **Bleu** : `#00B4FF` - Couleur secondaire
- **Fond sombre** : `#0A0A0A` - Arrière-plan principal

### Ajout de nouveaux types de missions

1. Modifier l'enum `MissionType` dans `src/main/java/com/elitedangerous/dashboard/model/MissionType.java`
2. Ajouter le nouveau type dans `MissionService.generateTestMissions()`

## Intégration avec l'API Elite Dangerous

Pour connecter le dashboard à l'API officielle d'Elite Dangerous :

1. Implémenter un client HTTP dans `MissionService`
2. Utiliser l'API Journal pour récupérer les missions en temps réel
3. Configurer l'authentification OAuth si nécessaire

## Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :
- Signaler des bugs
- Proposer de nouvelles fonctionnalités
- Améliorer le design
- Optimiser les performances

## Licence

Ce projet est sous licence MIT. Voir le fichier LICENSE pour plus de détails.

## Crédits

- **Elite Dangerous** : Frontier Developments
- **JavaFX** : Oracle Corporation
- **Maven** : Apache Software Foundation
