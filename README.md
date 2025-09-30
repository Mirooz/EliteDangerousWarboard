# Elite Dangerous Dashboard

## Description

Le Elite Dangerous Dashboard est une application JavaFX conçue pour améliorer l'expérience de jeu Elite Dangerous en fournissant une vue d'ensemble en temps réel de la progression des missions, des vaisseaux détruits et des statistiques de bounty. Elle traite les événements de journal générés par le jeu pour afficher les informations pertinentes dans une interface propre et intuitive.

![img.png](src%2Fmain%2Fresources%2Fimages%2Fimg.png)
## Fonctionnalités

### 🎯 Suivi des missions en temps réel
- **Missions de massacre actives** : Affiche les missions actives avec les factions cibles, la progression et les récompenses attendues
- **Indicateur de mission Wing** : Les missions contenant "Wing_name" dans leur titre sont marquées avec une icône "✈"
- **Filtrage dynamique** : Filtre les missions par statut (Actives, Complétées, Abandonnées, Toutes)
- **Filtrage par faction** : Cliquez sur une entrée dans le tableau des statistiques du footer pour filtrer les missions actives par cette faction source et cible spécifique

### 🚀 Gestion des vaisseaux détruits
- **Journal des vaisseaux détruits** : Liste les vaisseaux récemment détruits, leurs bounties et l'heure de destruction
- **Statistiques de bounty** : Suit le bounty total gagné et la répartition par faction depuis le dernier reset
- **Reset automatique** : Tous les logs de vaisseaux détruits et statistiques de bounty sont automatiquement réinitialisés lors d'un événement `RedeemVoucher` (spécifiquement pour bounty)

### 💰 Gestion des crédits
- **Crédits en attente** : Pour les missions actives, les crédits des missions complétées mais non récupérées sont affichés séparément dans l'en-tête comme "CRÉDITS EN ATTENTE" en bleu
- **Crédits espérés** : Affiche le total des crédits attendus des missions actives

### 📊 Statistiques avancées
- **Mise en évidence des kills** : Dans le tableau des statistiques de faction du footer, le plus haut nombre de kills pour une faction cible est affiché en gras orange, tandis que les autres entrées montrent le nombre de kills et la différence au plus haut nombre en vert (ex: "8 (-7)")
- **Complétion automatique** : Les missions de massacre qui reçoivent un événement `MissionRedirected` auront leur `currentCount` défini à `targetCount` pour les marquer comme complétées, sans changer leur statut

### 🎨 Interface utilisateur
- **Thème Elite Dangerous** : CSS personnalisé pour un thème cohérent Elite Dangerous
- **Tailles de police ajustées** : Tailles de police améliorées pour une meilleure lisibilité des informations clés
- **Interface épurée** : Design moderne et intuitif sans boutons inutiles

## Technologies utilisées

- **JavaFX** : Pour construire l'interface utilisateur graphique
- **Maven** : Pour la gestion de projet et l'automatisation de build
- **Jackson** : Pour analyser les fichiers journal JSON
- **Lombok** : Pour réduire le code boilerplate (getters, setters)

## Prérequis

- Java Development Kit (JDK) 17 ou supérieur
- Apache Maven 3.6.0 ou supérieur
- Elite Dangerous installé (pour générer les fichiers journal)

## Installation et configuration

### 1. Cloner le repository
```bash
git clone https://github.com/your-repo/elite-dangerous-dashboard.git
cd elite-dangerous-dashboard/elite-dashboard
```

### 2. Configurer le dossier journal
L'application doit savoir où se trouvent vos fichiers journal Elite Dangerous. Vous pouvez configurer cela dans le fichier `pom.xml`.

Ouvrez `pom.xml` et localisez la configuration `exec-maven-plugin`. Mettez à jour la balise `<value>` dans `<systemProperty>` avec le chemin de votre dossier journal Elite Dangerous.

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    <configuration>
        <mainClass>be.mirooz.elitedangerous.dashboard.EliteDashboardApp</mainClass>
        <systemProperties>
            <systemProperty>
                <key>journal.folder</key>
                <value>C:\Users\ewen_\Saved Games\Frontier Developments\Elite Dangerous</value> <!-- METTRE À JOUR CE CHEMIN -->
            </systemProperty>
        </systemProperties>
    </configuration>
    <goals>
        <goal>java</goal>
    </goals>
</plugin>
```

**Note :** Assurez-vous que le chemin est correct pour votre système. Si votre chemin contient des espaces, il devrait être géré correctement par Maven.

### 3. Construire et exécuter
Naviguez vers le répertoire `elite-dashboard` dans votre terminal et exécutez :

```bash
mvn clean install
mvn exec:java
```

La fenêtre de l'application devrait apparaître, affichant vos données de missions et bounty Elite Dangerous en lisant vos fichiers journal.

## Utilisation

### Interface utilisateur

- **En-tête** : Fournit un aperçu rapide des missions actives, des factions impliquées et des crédits attendus/en attente
- **Panneau gauche (VAISSEAUX DÉTRUITS)** : Affiche une liste des vaisseaux que vous avez détruits et votre bounty total. Cette liste et le bounty total se réinitialisent lorsque vous récupérez les bounties en jeu
- **Panneau droit (MISSIONS DE MASSACRE)** : Affiche vos missions de massacre actives. Les barres de progression indiquent votre nombre de kills vers l'objectif. Utilisez les boutons de filtre pour voir les missions par statut
- **Footer** : Affiche votre nom de commandant, système actuel, station et vaisseau. Le côté droit du footer affiche des statistiques détaillées de kills par faction cible et source. Cliquez sur une ligne dans ce tableau pour filtrer les missions actives dans le panneau droit pour ne montrer que les missions liées à cette faction source et cible spécifique. Cliquez à nouveau pour effacer le filtre

### Fonctionnalités interactives

1. **Filtrage des missions** : Utilisez les boutons en haut du panneau des missions pour filtrer par statut
2. **Filtrage par faction** : Cliquez sur une ligne dans le tableau des statistiques du footer pour filtrer les missions actives
3. **Surbrillance** : La ligne sélectionnée dans le tableau des factions est mise en surbrillance en orange
4. **Annulation du filtre** : Cliquez à nouveau sur la même ligne pour annuler le filtre

## Structure du projet

```
elite-dashboard/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── be/mirooz/elitedangerous/dashboard/
│   │   │       ├── controller/          # Contrôleurs JavaFX
│   │   │       ├── handlers/            # Gestionnaires d'événements journal
│   │   │       ├── model/               # Modèles de données
│   │   │       ├── service/             # Services métier
│   │   │       └── ui/                  # Gestionnaire d'interface
│   │   └── resources/
│   │       ├── css/                     # Styles CSS
│   │       ├── fxml/                    # Fichiers FXML
│   │       └── exemple/                 # Exemples de fichiers journal
│   └── test/                            # Tests unitaires
├── pom.xml                              # Configuration Maven
└── README.md                            # Ce fichier
```

## Événements journal supportés

L'application traite les événements suivants du journal Elite Dangerous :

- `MissionAccepted` : Ajoute une nouvelle mission
- `MissionCompleted` : Marque une mission comme complétée
- `MissionFailed` : Marque une mission comme échouée
- `MissionRedirected` : Marque les missions de massacre comme complétées
- `Bounty` : Met à jour le compteur de kills et les bounties
- `RedeemVoucher` : Réinitialise les statistiques de bounty
- `Commander` : Met à jour le nom du commandant
- `LoadGame` : Met à jour les informations du vaisseau
- `Location` : Met à jour la position actuelle

## Développement

### Ajout de nouvelles fonctionnalités

1. **Nouveaux événements journal** : Ajoutez un nouveau handler dans `src/main/java/be/mirooz/elitedangerous/dashboard/handlers/events/`
2. **Nouveaux modèles** : Ajoutez les modèles de données dans `src/main/java/be/mirooz/elitedangerous/dashboard/model/`
3. **Nouveaux contrôleurs** : Ajoutez les contrôleurs JavaFX dans `src/main/java/be/mirooz/elitedangerous/dashboard/controller/`

### Tests

```bash
mvn test
```

## Contribution

1. Fork le projet
2. Créez une branche pour votre fonctionnalité (`git checkout -b feature/AmazingFeature`)
3. Committez vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

## Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## Support

Si vous rencontrez des problèmes ou avez des questions :

1. Vérifiez que votre chemin journal est correct dans `pom.xml`
2. Assurez-vous que Elite Dangerous génère des fichiers journal
3. Vérifiez que Java 17+ et Maven 3.6+ sont installés
4. Ouvrez une issue sur GitHub avec les détails de votre problème

## Changelog

### Version 1.0.0
- Suivi des missions de massacre en temps réel
- Journal des vaisseaux détruits
- Statistiques de bounty par faction
- Filtrage interactif des missions
- Interface utilisateur Elite Dangerous
- Support des missions Wing
- Reset automatique des bounties
- Gestion des crédits en attente