package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.dashboard.controller.IRefreshable;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.TooltipComponent;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.Scan;
import be.mirooz.elitedangerous.dashboard.model.exploration.SpeciesProbability;
import be.mirooz.elitedangerous.dashboard.model.exploration.StarDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Composant pour la vue visuelle du système (orrery)
 */
public class SystemVisualViewComponent implements Initializable, IRefreshable {

    @FXML
    private VBox bodiesListPanel;
    @FXML
    private VBox bodiesListContainer;
    
    private RadarComponent radarComponent;
    @FXML
    private CheckBox showOnlyHighValueBodiesCheckBox;
    @FXML
    private ScrollPane bodiesScrollPane;
    @FXML
    private Group bodiesGroup;
    @FXML
    private Pane bodiesPane;
    @FXML
    private VBox jsonDetailPanel;
    @FXML
    private Label jsonBodyNameLabel;
    @FXML
    private Button closeJsonPanelButton;
    @FXML
    private TreeView<JsonTreeItem> jsonTreeView;

    private Image gasImage;
    private Image starImage;
    // Images par type de planète
    private final Map<be.mirooz.elitedangerous.biologic.BodyType, Image> planetImages = new HashMap<>();
    private Image exobioImage;
    private Image mappedImage;
    private SystemVisited currentSystem;
    private Map<Integer, BodyPosition> bodyPositions = new HashMap<>();
    private Scale zoomTransform;
    private double currentZoom = 1.0;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 5.0;
    private static final double ZOOM_FACTOR = 0.1;
    private ACelesteBody currentJsonBody; // Corps actuellement affiché dans le panneau JSON

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Charger les images
        try {
            gasImage = new Image(getClass().getResourceAsStream("/images/exploration/gas.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image gas.png: " + e.getMessage());
        }
        try {
            starImage = new Image(getClass().getResourceAsStream("/images/exploration/star.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image star.png: " + e.getMessage());
        }
        try {
            exobioImage = new Image(getClass().getResourceAsStream("/images/exploration/exobio.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image exobio.png: " + e.getMessage());
        }
        try {
            mappedImage = new Image(getClass().getResourceAsStream("/images/exploration/mapped.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image mapped.png: " + e.getMessage());
        }
        
        // Charger les images par type de planète
        loadPlanetImages();
        
        // Initialiser la transformation de zoom
        zoomTransform = new Scale(1.0, 1.0);
        bodiesGroup.getTransforms().add(zoomTransform);
        
        // Désactiver les scrollbars
        bodiesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodiesScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // Empêcher complètement le défilement du ScrollPane
        bodiesScrollPane.setPannable(false);
        bodiesScrollPane.setFitToWidth(true);
        bodiesScrollPane.setFitToHeight(true);
        
        // Bloquer tous les événements de défilement qui pourraient causer un scroll
        bodiesScrollPane.addEventFilter(ScrollEvent.ANY, event -> {
            // Consommer l'événement pour empêcher le défilement par défaut
            event.consume();
            // Gérer le zoom manuellement
            handleScroll(event);
        });
        
        // Cocher la checkbox par défaut
        if (showOnlyHighValueBodiesCheckBox != null) {
            showOnlyHighValueBodiesCheckBox.setSelected(true);
        }
        
        // Initialiser le radar
        initializeRadar();
    }
    
    /**
     * Initialise le composant radar
     */
    private void initializeRadar() {
        // Créer le composant radar
        radarComponent = new RadarComponent();
        
        // Ajouter le radar au-dessus de la liste des corps
        if (bodiesListPanel != null) {
            // Insérer le radar après le titre et la checkbox, avant le ScrollPane
            // Le titre est à l'index 0, la checkbox à l'index 1, le ScrollPane à l'index 2
            // On insère le radar à l'index 2, ce qui déplace le ScrollPane à l'index 3
            if (bodiesListPanel.getChildren().size() >= 2) {
                bodiesListPanel.getChildren().add(2, radarComponent.getRadarPane());
            } else {
                bodiesListPanel.getChildren().add(radarComponent.getRadarPane());
            }
        }
    }
    
    /**
     * Gère le zoom avec la molette de la souris
     * Zoom simple sans tenir compte de la position de la souris
     */
    private void handleScroll(ScrollEvent event) {
        // L'événement est déjà consommé par l'EventFilter, pas besoin de le consommer à nouveau
        
        double deltaY = event.getDeltaY();
        if (deltaY == 0) {
            return;
        }
        
        // Calculer le nouveau niveau de zoom
        double zoomChange = deltaY > 0 ? (1 + ZOOM_FACTOR) : (1 - ZOOM_FACTOR);
        double newZoom = currentZoom * zoomChange;
        
        // Limiter le zoom entre MIN_ZOOM et MAX_ZOOM
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        
        if (newZoom != currentZoom) {
            // Appliquer le zoom simple
            zoomTransform.setPivotX(0);
            zoomTransform.setPivotY(0);
            zoomTransform.setX(newZoom);
            zoomTransform.setY(newZoom);
            currentZoom = newZoom;
        }
    }
    
    /**
     * Calcule et applique le zoom optimal pour afficher tout le contenu dans la vue
     * Le coin haut gauche est toujours visible
     */
    private void calculateAndApplyOptimalZoom() {
        // Obtenir les dimensions du viewport
        double viewportWidth = bodiesScrollPane.getViewportBounds().getWidth();
        double viewportHeight = bodiesScrollPane.getViewportBounds().getHeight();
        
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            // Réessayer plus tard si les dimensions ne sont pas encore disponibles
            Platform.runLater(() -> calculateAndApplyOptimalZoom());
            return;
        }
        
        // Calculer les dimensions réelles du contenu en parcourant les positions des corps
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        
        // Parcourir toutes les positions des corps pour trouver les limites réelles
        boolean hasBodies = false;
        for (BodyPosition pos : bodyPositions.values()) {
            hasBodies = true;
            // Prendre en compte la taille de l'image (60x60, centrée)
            double bodyRadius = 30;
            minX = Math.min(minX, pos.x - bodyRadius);
            maxX = Math.max(maxX, pos.x + bodyRadius);
            minY = Math.min(minY, pos.y - bodyRadius);
            maxY = Math.max(maxY, pos.y);
        }
        
        // Si aucun corps n'a été trouvé, utiliser les dimensions du pane
        double contentWidth;
        double contentHeight;
        double offsetX = 0;
        double offsetY = 0;
        
        if (hasBodies && minX != Double.MAX_VALUE) {
            // Utiliser les dimensions réelles du contenu avec une marge
            // Le minX et minY représentent le coin haut gauche réel
            offsetX = minX - 50; // Marge de 50px à gauche
            offsetY = minY - 50; // Marge de 50px en haut
            contentWidth = (maxX - minX) + 100; // Marge de 50px de chaque côté
            contentHeight = (maxY - minY) + 50;
        } else {
            // Fallback sur les dimensions du pane
            contentWidth = Math.max(bodiesPane.getPrefWidth(), bodiesPane.getMinWidth());
            contentHeight = Math.max(bodiesPane.getPrefHeight(), bodiesPane.getMinHeight());
            
            if (contentWidth <= 0) {
                contentWidth = bodiesPane.getWidth();
            }
            if (contentHeight <= 0) {
                contentHeight = bodiesPane.getHeight();
            }
        }
        
        if (contentWidth <= 0 || contentHeight <= 0) {
            // Réessayer plus tard si les dimensions ne sont pas encore disponibles
            Platform.runLater(() -> calculateAndApplyOptimalZoom());
            return;
        }
        
        // Calculer le zoom nécessaire pour que tout le contenu soit visible
        // Avec une marge de 15% de chaque côté pour être sûr que tout est visible
        double zoomX = (viewportWidth * 0.85) / contentWidth;
        double zoomY = (viewportHeight * 0.85) / contentHeight;
        
        // Prendre le minimum pour que tout soit visible
        double optimalZoom = Math.min(zoomX, zoomY);
        
        // S'assurer qu'on peut dézoomer suffisamment (ne pas limiter trop haut)
        // Limiter le zoom entre MIN_ZOOM et MAX_ZOOM, mais permettre un zoom plus faible si nécessaire
        optimalZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, optimalZoom));
        
        // Si le zoom calculé est trop grand (contenu plus petit que le viewport), prendre un zoom qui affiche tout
        if (optimalZoom > 1.0 && (contentWidth * optimalZoom < viewportWidth * 0.9 || contentHeight * optimalZoom < viewportHeight * 0.9)) {
            // Recalculer avec une marge plus grande
            zoomX = (viewportWidth * 0.95) / contentWidth;
            zoomY = (viewportHeight * 0.95) / contentHeight;
            optimalZoom = Math.min(zoomX, zoomY);
            optimalZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, optimalZoom));
        }
        
        // Appliquer le zoom optimal
        // Le pivot doit être à (0, 0) pour que le zoom se fasse depuis le coin haut gauche
        zoomTransform.setPivotX(0);
        zoomTransform.setPivotY(0);
        zoomTransform.setX(optimalZoom);
        zoomTransform.setY(optimalZoom);
        currentZoom = optimalZoom;
        
        // Positionner le coin haut gauche visible
        // Toujours positionner le scroll à 0,0 pour que la première étoile soit en haut à gauche
        bodiesScrollPane.setHvalue(0.0);
        bodiesScrollPane.setVvalue(0.0);
    }

    @Override
    public void refreshUI() {
        refresh();
    }

    public void refresh() {
        if (currentSystem != null) {
            displaySystem(currentSystem);
        } else {
            clearDisplay();
        }
    }

    /**
     * Affiche les corps célestes d'un système dans la vue visuelle avec tri orrery et liens visuels
     */
    public void displaySystem(SystemVisited system) {
        Platform.runLater(() -> {
            // Fermer le panneau JSON si un nouveau système est sélectionné
            if (this.currentSystem != null && system != null && 
                !this.currentSystem.equals(system)) {
                closeJsonPanel();
            }
            
            // Réinitialiser le corps JSON affiché si on change de système
            if (system == null || (this.currentSystem != null && system != null && 
                !this.currentSystem.equals(system))) {
                currentJsonBody = null;
            }
            
            this.currentSystem = system;
            bodiesPane.getChildren().clear();
            bodyPositions.clear();
            
            // Mettre à jour la liste des corps à gauche
            updateBodiesList(system);
            
            // Le zoom optimal sera calculé après le positionnement des corps

            if (system == null || system.getCelesteBodies() == null || system.getCelesteBodies().isEmpty()) {
                return;
            }

            // Créer une map pour lookup rapide
            Map<Integer, ACelesteBody> bodiesMap = system.getCelesteBodies().stream()
                    .collect(Collectors.toMap(ACelesteBody::getBodyID, body -> body));

            // Trier selon la hiérarchie orrery
            List<ACelesteBody> sortedBodies = sortBodiesHierarchically(system.getCelesteBodies());

            // Disposer les corps : étoiles verticalement à gauche, planètes en chaîne horizontale, lunes verticalement
            // Ajouter un gap pour que les planètes ne collent pas au bord
            int gapLeft = 50; // Espace à gauche
            int gapTop = 50; // Espace en haut
            int startX = gapLeft; // Position de départ avec gap
            int startY = gapTop;
            int starVerticalSpacing = 200; // Espacement vertical entre les étoiles
            int horizontalSpacing = 120; // Espacement horizontal entre les planètes
            int moonVerticalSpacing = 80; // Espacement vertical entre les lunes

            // Organiser la hiérarchie : étoiles -> planètes directes -> lunes -> lunes de lune
            List<ACelesteBody> stars = new ArrayList<>();
            Map<ACelesteBody, List<ACelesteBody>> starToDirectPlanets = new HashMap<>();
            Map<ACelesteBody, List<ACelesteBody>> planetToMoons = new HashMap<>();
            Map<ACelesteBody, List<ACelesteBody>> moonToSubMoons = new HashMap<>();

            // Identifier les étoiles
            for (ACelesteBody body : sortedBodies) {
                if (body instanceof StarDetail) {
                    var parents = body.getParents();
                    if (parents == null || parents.isEmpty() ||
                        parents.stream().anyMatch(p -> "Null".equalsIgnoreCase(p.getType()))) {
                        stars.add(body);
                        starToDirectPlanets.put(body, new ArrayList<>());
                    }
                }
            }

            // Identifier les planètes directes des étoiles, les lunes des planètes, et les lunes de lune
            for (ACelesteBody body : sortedBodies) {
                if (body instanceof PlaneteDetail) {
                    var parents = body.getParents();
                    if (parents != null && !parents.isEmpty()) {
                        // Trouver le parent direct en utilisant le type
                        // Le parent direct est le parent avec le bodyID le plus élevé de type "Planet" ou "Star" (pas "Null")
                        ACelesteBody directParent = null;
                        String directParentType = null;
                        int maxBodyID = -1;
                        
                        for (var parent : parents) {
                            if ("Null".equalsIgnoreCase(parent.getType())) {
                                continue; // Ignorer les parents "Null"
                            }
                            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
                            if (parentBody != null && parent.getBodyID() > maxBodyID) {
                                directParent = parentBody;
                                directParentType = parent.getType();
                                maxBodyID = parent.getBodyID();
                            }
                        }
                        
                        if (directParent != null && directParentType != null) {
                            if ("Star".equalsIgnoreCase(directParentType) && directParent instanceof StarDetail) {
                                // C'est une planète directe de l'étoile
                                starToDirectPlanets.computeIfAbsent(directParent, k -> new ArrayList<>()).add(body);
                                planetToMoons.put(body, new ArrayList<>());
                            } else if ("Planet".equalsIgnoreCase(directParentType) && directParent instanceof PlaneteDetail) {
                                // C'est une lune ou une lune de lune
                                // Vérifier si le parent direct est une planète directe (a une étoile comme parent)
                                boolean isDirectPlanet = true;
                                var parentParents = directParent.getParents();
                                if (parentParents != null && !parentParents.isEmpty()) {
                                    for (var pp : parentParents) {
                                        if ("Planet".equalsIgnoreCase(pp.getType())) {
                                            ACelesteBody ppBody = bodiesMap.get(pp.getBodyID());
                                            if (ppBody instanceof PlaneteDetail) {
                                                isDirectPlanet = false;
                                                break;
                                            }
                                        }
                                    }
                                }
                                
                                if (isDirectPlanet) {
                                    // C'est une lune d'une planète directe
                                    planetToMoons.computeIfAbsent(directParent, k -> new ArrayList<>()).add(body);
                                    moonToSubMoons.put(body, new ArrayList<>());
                                } else {
                                    // C'est une lune de lune (sub-lune)
                                    moonToSubMoons.computeIfAbsent(directParent, k -> new ArrayList<>()).add(body);
                                }
                            }
                        }
                    }
                }
            }

            // Disposer chaque étoile avec ses planètes et lunes
            int currentStarY = startY;
            int maxX = startX;
            
            for (ACelesteBody star : stars) {
                // Positionner l'étoile verticalement à gauche
                bodyPositions.put(star.getBodyID(), new BodyPosition(startX, currentStarY, star));
                ImageView starView = createBodyImageView(star, startX, currentStarY);
                bodiesPane.getChildren().add(starView);

                // Positionner les planètes directes de cette étoile en chaîne horizontale à droite
                List<ACelesteBody> directPlanets = starToDirectPlanets.get(star);
                if (directPlanets != null && !directPlanets.isEmpty()) {
                    int planetX = startX + horizontalSpacing;
                    int planetY = currentStarY;
                    
                    // Ensuite, positionner chaque planète et ses lunes
                    for (int i = 0; i < directPlanets.size(); i++) {
                        ACelesteBody planet = directPlanets.get(i);
                        
                        // Positionner la planète
                        bodyPositions.put(planet.getBodyID(), new BodyPosition(planetX, planetY, planet));
                        ImageView planetView = createBodyImageView(planet, planetX, planetY);
                        bodiesPane.getChildren().add(planetView);
                        
                        // Ajouter les icônes sous la planète (exobio et mapped)
                        addPlanetIcons(planet, planetX, planetY);

                        // Positionner les lunes de cette planète verticalement en dessous
                        // et calculer la position X maximale atteinte par les sub-lunes
                        int maxSubMoonX = planetX; // Position X maximale des sub-lunes
                        List<ACelesteBody> moons = planetToMoons.get(planet);
                        if (moons != null && !moons.isEmpty()) {
                            int moonY = planetY + moonVerticalSpacing;
                            for (ACelesteBody moon : moons) {
                                // Positionner la lune
                                bodyPositions.put(moon.getBodyID(), new BodyPosition(planetX, moonY, moon));
                                ImageView moonView = createBodyImageView(moon, planetX, moonY);
                                bodiesPane.getChildren().add(moonView);
                                
                                // Ajouter les icônes sous la lune (exobio et mapped)
                                addPlanetIcons(moon, planetX, moonY);

                                // Positionner les lunes de lune (sub-lunes) horizontalement à droite de la lune
                                List<ACelesteBody> subMoons = moonToSubMoons.get(moon);
                                if (subMoons != null && !subMoons.isEmpty()) {
                                    int subMoonX = planetX + horizontalSpacing;
                                    for (ACelesteBody subMoon : subMoons) {
                                        bodyPositions.put(subMoon.getBodyID(), new BodyPosition(subMoonX, moonY, subMoon));
                                        ImageView subMoonView = createBodyImageView(subMoon, subMoonX, moonY);
                                        bodiesPane.getChildren().add(subMoonView);
                                        
                                        // Ajouter les icônes sous la lune de lune (exobio et mapped)
                                        addPlanetIcons(subMoon, subMoonX, moonY);
                                        
                                        subMoonX += horizontalSpacing;
                                        maxSubMoonX = Math.max(maxSubMoonX, subMoonX);
                                    }
                                    maxX = Math.max(maxX, subMoonX);
                                }

                                moonY += moonVerticalSpacing;
                            }
                        }
                        
                        // Calculer la position X pour la planète suivante
                        // Si cette planète a des sub-lunes, décaler la planète suivante pour qu'elle soit
                        // au-delà de la position maximale des sub-lunes, avec une marge de sécurité
                        if (i + 1 < directPlanets.size()) {
                            if (maxSubMoonX > planetX) {
                                // Il y a des sub-lunes, positionner la planète suivante après la dernière sub-lune
                                // avec une marge de sécurité (horizontalSpacing) pour éviter les superpositions
                                planetX = maxSubMoonX ;
                            } else {
                                // Pas de sub-lunes, espacement normal
                                planetX += horizontalSpacing;
                            }
                        }
                    }
                    maxX = Math.max(maxX, planetX);
                }

                // Calculer la prochaine position Y pour l'étoile suivante
                // Prendre en compte la hauteur nécessaire pour cette étoile et ses planètes/lunes
                int maxHeightForThisStar = currentStarY;
                if (directPlanets != null && !directPlanets.isEmpty()) {
                    for (ACelesteBody planet : directPlanets) {
                        List<ACelesteBody> moons = planetToMoons.get(planet);
                        if (moons != null && !moons.isEmpty()) {
                            // Calculer la position Y de la dernière lune
                            int lastMoonY = currentStarY + moonVerticalSpacing * moons.size();
                            maxHeightForThisStar = Math.max(maxHeightForThisStar, lastMoonY);
                        }
                    }
                }
                // Positionner la prochaine étoile en dessous de tout ce qui précède
                currentStarY = maxHeightForThisStar + starVerticalSpacing;
            }
            
            // Deuxième passe : traiter les lunes de lune qui n'ont pas été positionnées
            // (peut arriver si elles n'ont pas été correctement détectées dans la première passe)
            for (ACelesteBody body : sortedBodies) {
                if (body instanceof PlaneteDetail && !bodyPositions.containsKey(body.getBodyID())) {
                    var parents = body.getParents();
                    if (parents != null && !parents.isEmpty()) {
                        for (var parent : parents) {
                            if ("Planet".equalsIgnoreCase(parent.getType()) && !"Null".equalsIgnoreCase(parent.getType())) {
                                ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
                                if (parentBody instanceof PlaneteDetail) {
                                    // Vérifier si le parent est une lune (a un parent de type "Planet", pas "Star")
                                    var parentParents = parentBody.getParents();
                                    boolean parentIsMoon = false;
                                    if (parentParents != null && !parentParents.isEmpty()) {
                                        for (var pp : parentParents) {
                                            if ("Planet".equalsIgnoreCase(pp.getType()) && !"Null".equalsIgnoreCase(pp.getType())) {
                                                parentIsMoon = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (parentIsMoon && bodyPositions.containsKey(parentBody.getBodyID())) {
                                        // C'est une lune de lune, l'ajouter à la map et la positionner
                                        moonToSubMoons.computeIfAbsent(parentBody, k -> new ArrayList<>()).add(body);
                                        BodyPosition parentPos = bodyPositions.get(parentBody.getBodyID());
                                        if (parentPos != null) {
                                            int subMoonX = (int)parentPos.x + horizontalSpacing;
                                            int subMoonY = (int)parentPos.y;
                                            bodyPositions.put(body.getBodyID(), new BodyPosition(subMoonX, subMoonY, body));
                                            ImageView subMoonView = createBodyImageView(body, subMoonX, subMoonY);
                                            bodiesPane.getChildren().add(subMoonView);
                                            
                                            // Ajouter les icônes sous la lune de lune (exobio et mapped)
                                            addPlanetIcons(body, subMoonX, subMoonY);
                                            
                                            maxX = Math.max(maxX, subMoonX + horizontalSpacing);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dessiner les lignes de connexion
            drawConnectionsHierarchical(bodiesMap, stars, starToDirectPlanets, planetToMoons, moonToSubMoons);

            // Fixer une taille constante pour le pane (ne pas adapter au contenu)
            // Utiliser une taille fixe de 800x450 pour tous les systèmes
            double fixedWidth = 800;
            double fixedHeight = 450;
            bodiesPane.setMinWidth(600);
            bodiesPane.setMinHeight(300);
            bodiesPane.setPrefWidth(fixedWidth);
            bodiesPane.setPrefHeight(fixedHeight);
            bodiesPane.setMaxWidth(fixedWidth);
            bodiesPane.setMaxHeight(600);
            
            // Calculer et appliquer le zoom optimal pour afficher tout le contenu
            // Utiliser plusieurs Platform.runLater pour s'assurer que les dimensions sont mises à jour
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    calculateAndApplyOptimalZoom();
                });
            });
        });
    }

    /**
     * Retire le nom du système du début du bodyName
     */
    private String getBodyNameWithoutSystem(ACelesteBody body) {
        String bodyName = body.getBodyName();
        String systemName = body.getStarSystem();
        
        if (systemName != null && bodyName != null && bodyName.startsWith(systemName)) {
            // Retirer le nom du système et l'espace qui suit
            String nameWithoutSystem = bodyName.substring(systemName.length()).trim();
            // Si le nom commence par un espace, le retirer
            if (nameWithoutSystem.startsWith(" ")) {
                nameWithoutSystem = nameWithoutSystem.substring(1);
            }
            return nameWithoutSystem.isEmpty() ? bodyName : nameWithoutSystem;
        }
        
        return bodyName;
    }
    
    /**
     * Charge les images par type de planète
     */
    private void loadPlanetImages() {
        be.mirooz.elitedangerous.biologic.BodyType[] types = be.mirooz.elitedangerous.biologic.BodyType.values();
        for (be.mirooz.elitedangerous.biologic.BodyType type : types) {
            String imageName = getImageNameForBodyType(type);
            try {
                Image image = new Image(getClass().getResourceAsStream("/images/exploration/" + imageName));
                planetImages.put(type, image);
            } catch (Exception e) {
                // Si l'image n'existe pas, utiliser l'image par défaut (gas.png)
                System.err.println("Image non trouvée pour " + type + " (" + imageName + "), utilisation de gas.png par défaut");
            }
        }
    }
    
    /**
     * Retourne le nom de l'image correspondant au type de planète
     */
    private String getImageNameForBodyType(be.mirooz.elitedangerous.biologic.BodyType type) {
        return switch (type) {
            case METAL_RICH -> "metal-rich.png";
            case HIGH_METAL_CONTENT -> "high-metal-content.png";
            case ROCKY -> "rocky.png";
            case ROCKY_ICE -> "rocky-ice.png";
            case ICY -> "icy.png";
            case WATER_WORLD -> "water-world.png";
            case EARTHLIKE -> "earthlike.png";
            case AMMONIA -> "ammonia.png";
            case GAS_GIANT, GAS_GIANT_I, GAS_GIANT_II, GAS_GIANT_III, GAS_GIANT_IV, GAS_GIANT_V -> "gas.png";
            case HELIUM_RICH_GG, HELIUM_GG -> "helium-gas.png";
            case ICE_GIANT -> "ice-giant.png";
            case CLUSTER -> "cluster.png";
            case UNKNOWN -> "gas.png"; // Par défaut
        };
    }
    
    /**
     * Obtient l'image appropriée pour un corps céleste
     */
    private Image getImageForBody(ACelesteBody body) {
        if (body instanceof StarDetail) {
            return starImage;
        } else if (body instanceof PlaneteDetail planet) {
            be.mirooz.elitedangerous.biologic.BodyType planetClass = planet.getPlanetClass();
            if (planetClass != null && planetImages.containsKey(planetClass)) {
                return planetImages.get(planetClass);
            }
            // Fallback sur gas.png si l'image n'est pas trouvée
            return gasImage;
        }
        return gasImage;
    }
    
    /**
     * Crée une ImageView pour un corps céleste
     */
    private ImageView createBodyImageView(ACelesteBody body, double x, double y) {
        Image image = getImageForBody(body);
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(60);
        imageView.setFitHeight(60);
        imageView.setPreserveRatio(true);
        imageView.setX(x - 30); // Centrer l'image
        imageView.setY(y - 30);

        // Tooltip
        String bodyName = getBodyNameWithoutSystem(body);
        if (body instanceof StarDetail) {
            bodyName = "★ " + bodyName;
        } else if (body instanceof PlaneteDetail) {
            bodyName = "● " + bodyName;
        }
        Tooltip tooltip = new TooltipComponent(bodyName);
        Tooltip.install(imageView, tooltip);

        imageView.getStyleClass().add("exploration-visual-body");
        if (body instanceof StarDetail) {
            imageView.getStyleClass().add("exploration-visual-star");
        } else if (body instanceof PlaneteDetail) {
            imageView.getStyleClass().add("exploration-visual-planet");
        }

        // Gestionnaire de clic pour afficher le JSON
        imageView.setOnMouseClicked(event -> showJsonDialog(body, event));

        return imageView;
    }

    /**
     * Ajoute les icônes exobio et mapped en haut à droite d'une planète dans un badge
     */
    private void addPlanetIcons(ACelesteBody body, double planetX, double planetY) {
        if (!(body instanceof PlaneteDetail planet)) {
            return;
        }

        // Vérifier quelles icônes afficher
        boolean hasExobio = exobioImage != null && 
            ((planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) ||
             (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()));
        
        // Vérifier si la planète respecte les conditions pour mapped
        // Si la planète est déjà mapped, on affiche toujours l'icône + V vert
        // Sinon, on affiche l'icône seulement si elle respecte les conditions (terraformable OU baseK > 50000)
        boolean shouldShowMappedIcon = false;
        boolean isMapped = planet.isMapped();
        
        if (isMapped) {
            // Si la planète est déjà mapped, toujours afficher l'icône + V vert
            shouldShowMappedIcon = true;
        } else if (planet.getPlanetClass() != null) {
            // Sinon, vérifier si elle respecte les conditions
            int baseK = planet.getPlanetClass().getBaseK();
            shouldShowMappedIcon = planet.isTerraformable() || baseK > 50000;
        }
        
        if (!hasExobio && !shouldShowMappedIcon) {
            return; // Pas d'icônes à afficher
        }

        // Taille des icônes
        double iconSize = 14; // Taille des icônes
        double iconSpacing = 2; // Espacement vertical entre les icônes
        double padding = 3; // Padding du badge
        
        // Créer un conteneur vertical pour les icônes
        VBox iconsContainer = new VBox(iconSpacing);
        iconsContainer.setAlignment(javafx.geometry.Pos.CENTER);
        
        // Ajouter les icônes
        if (hasExobio) {
            // Créer un HBox pour l'icône exobio et le compteur
            HBox exobioContainer = new HBox(3);
            exobioContainer.setAlignment(javafx.geometry.Pos.CENTER);
            
            ImageView exobioIcon = new ImageView(exobioImage);
            exobioIcon.setFitWidth(iconSize);
            exobioIcon.setFitHeight(iconSize);
            exobioIcon.setPreserveRatio(true);
            exobioContainer.getChildren().add(exobioIcon);
            
            // Calculer le nombre d'espèces collectées et détectées
            int confirmedSpeciesCount = 0;
            int numSpeciesDetected = 0;
            
            if (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()) {
                confirmedSpeciesCount = (int) planet.getConfirmedSpecies().stream()
                        .filter(species -> species.isCollected())
                        .count();
            }
            if (planet.getNumSpeciesDetected() != null) {
                numSpeciesDetected = planet.getNumSpeciesDetected();
            }
            
            // Déterminer la couleur selon le nombre d'espèces collectées
            String color;
            if (confirmedSpeciesCount == 0) {
                // 0/Y → rouge
                color = "#FF4444";
            } else if (numSpeciesDetected > 0 && confirmedSpeciesCount == numSpeciesDetected) {
                // Y/Y → vert
                color = "#00FF88";
            } else {
                // Entre 1 et Y-1 → orange
                color = "#FF8800";
            }
            
            // Ajouter le label avec le format X/Y
            Label speciesCountLabel = new Label(String.format("%d/%d", confirmedSpeciesCount, numSpeciesDetected));
            speciesCountLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 9px; -fx-font-weight: bold;", color));
            exobioContainer.getChildren().add(speciesCountLabel);
            
            iconsContainer.getChildren().add(exobioContainer);
        }
        
        if (shouldShowMappedIcon && mappedImage != null) {
            // Créer un HBox pour l'icône mapped et l'indicateur de statut
            HBox mappedContainer = new HBox(3);
            mappedContainer.setAlignment(javafx.geometry.Pos.CENTER);
            
            ImageView mappedIcon = new ImageView(mappedImage);
            mappedIcon.setFitWidth(iconSize);
            mappedIcon.setFitHeight(iconSize);
            mappedIcon.setPreserveRatio(true);
            mappedContainer.getChildren().add(mappedIcon);
            
            // Ajouter l'indicateur de statut (V vert si mapped, croix rouge si non mapped)
            Label statusLabel = new Label();
            if (isMapped) {
                statusLabel.setText("✓");
                statusLabel.setStyle("-fx-text-fill: #00FF00; -fx-font-size: 10px; -fx-font-weight: bold;");
            } else {
                statusLabel.setText("✗");
                statusLabel.setStyle("-fx-text-fill: #FF0000; -fx-font-size: 10px; -fx-font-weight: bold;");
            }
            mappedContainer.getChildren().add(statusLabel);
            
            iconsContainer.getChildren().add(mappedContainer);
        }
        
        // Créer un badge avec fond semi-transparent
        StackPane badge = new StackPane();
        badge.getChildren().add(iconsContainer);
        
        // Fond du badge avec coins arrondis
        // Si exobio est présent, le badge doit être plus large pour accommoder l'icône + le compteur
        // Si mapped est présent, le badge doit être plus large pour accommoder l'icône + le statut
        double badgeWidth = hasExobio ? 
            (iconSize + 25 + (padding * 2)) : // icône + texte (~25px) + padding
            (shouldShowMappedIcon ? (iconSize + 12 + (padding * 2)) : (iconSize + (padding * 2))); // icône + statut (~12px) + padding
        double badgeHeight = ((hasExobio ? 1 : 0) + (shouldShowMappedIcon ? 1 : 0) > 1 ? 
            (iconSize * 2) + iconSpacing : iconSize) + (padding * 2);
        
        Rectangle background = new Rectangle(badgeWidth, badgeHeight);
        background.setArcWidth(8);
        background.setArcHeight(8);
        background.setFill(Color.rgb(0, 0, 0, 0.7)); // Fond noir semi-transparent
        background.setStroke(Color.rgb(255, 255, 255, 0.3)); // Bordure blanche légère
        background.setStrokeWidth(1);
        
        badge.getChildren().add(0, background); // Ajouter le fond en premier
        
        // Position du badge : coin supérieur droit de la planète
        // La planète fait 60x60 et est centrée sur (planetX, planetY)
        double offsetX = 4; // Décalage depuis le bord droit
        double offsetY = 4; // Décalage depuis le bord supérieur
        double badgeX = planetX + 30 - badgeWidth + offsetX; // 30px = demi-largeur de la planète
        double badgeY = planetY - 30 - offsetY; // -30px = demi-hauteur de la planète
        
        badge.setLayoutX(badgeX);
        badge.setLayoutY(badgeY);
        
        bodiesPane.getChildren().add(badge);
    }

    /**
     * Affiche le panneau JSON pour un corps céleste
     */
    private void showJsonDialog(ACelesteBody body, MouseEvent event) {
        if (body == null || jsonDetailPanel == null) {
            return;
        }

        // Si on clique sur le même corps et que le panneau est ouvert, le fermer
        if (currentJsonBody != null && currentJsonBody.getBodyID() == body.getBodyID() && 
            jsonDetailPanel.isVisible()) {
            closeJsonPanel();
            return;
        }

        // Mémoriser le corps actuellement affiché
        currentJsonBody = body;

        // Définir le nom du corps avec les icônes si c'est une planète
        String bodyName = body.getBodyName() != null ? body.getBodyName() : "Corps céleste";
        
        // Vérifier si c'est une planète avec exobio ou mapped
        boolean[] icons = LabelIconHelper.checkPlanetIcons(body);
        boolean hasExobio = icons[0];
        boolean hasMapped = icons[1];
        
        // Mettre à jour le label avec les icônes
        LabelIconHelper.updateLabelWithIcons(
            jsonBodyNameLabel, 
            bodyName, 
            hasExobio, 
            hasMapped, 
            exobioImage, 
            mappedImage
        );

        // Configurer la cellule personnalisée pour le TreeView
        jsonTreeView.setCellFactory(treeView -> new JsonTreeCell());
        
        // Construire le TreeView à partir du JSON
        JsonNode jsonNode = body.getJsonNode();
        if (jsonNode != null) {
            TreeItem<JsonTreeItem> root = buildJsonTree(jsonNode, "");
            jsonTreeView.setRoot(root);
            jsonTreeView.setShowRoot(false); // Masquer le root pour un affichage plus propre
            // Développer seulement le premier niveau (pas les arrays)
            root.setExpanded(true);
            if (root.getChildren() != null && !root.getChildren().isEmpty()) {
                root.getChildren().forEach(child -> {
                    JsonTreeItem childValue = child.getValue();
                    // Ne pas développer les arrays par défaut
                    if (childValue != null && childValue.getValueType() != JsonTreeItem.JsonValueType.ARRAY) {
                        child.setExpanded(true);
                        // Ne pas développer le niveau suivant
                        if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                            child.getChildren().forEach(grandChild -> grandChild.setExpanded(false));
                        }
                    } else {
                        child.setExpanded(false);
                    }
                });
            }
        } else {
            JsonTreeItem emptyItem = new JsonTreeItem("", null);
            TreeItem<JsonTreeItem> root = new TreeItem<>(emptyItem);
            root.setValue(new JsonTreeItem("Aucun JSON disponible", null));
            jsonTreeView.setRoot(root);
            jsonTreeView.setShowRoot(true);
        }

        // Afficher le panneau
        jsonDetailPanel.setVisible(true);
        jsonDetailPanel.setManaged(true);
    }

    /**
     * Construit un TreeItem à partir d'un JsonNode avec un affichage plus joli
     */
    private TreeItem<JsonTreeItem> buildJsonTree(JsonNode node, String key) {
        JsonTreeItem jsonItem = new JsonTreeItem(key, node);
        TreeItem<JsonTreeItem> item = new TreeItem<>(jsonItem);
        
        if (node == null || node.isNull()) {
            return item;
        }

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldKey = field.getKey();
                // Filtrer les champs event, timestamp, scantype, parents
                if (!"event".equalsIgnoreCase(fieldKey) && 
                    !"timestamp".equalsIgnoreCase(fieldKey) && 
                    !"scantype".equalsIgnoreCase(fieldKey) &&
                    !"parents".equalsIgnoreCase(fieldKey)) {
                    TreeItem<JsonTreeItem> child = buildJsonTree(field.getValue(), fieldKey);
                    item.getChildren().add(child);
                }
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                TreeItem<JsonTreeItem> child = buildJsonTree(node.get(i), "[" + i + "]");
                item.getChildren().add(child);
            }
        }
        
        return item;
    }

    /**
     * Ferme le panneau JSON
     */
    @FXML
    private void closeJsonPanel() {
        if (jsonDetailPanel != null) {
            jsonDetailPanel.setVisible(false);
            jsonDetailPanel.setManaged(false);
            currentJsonBody = null; // Réinitialiser le corps affiché
        }
    }

    /**
     * Dessine les lignes de connexion hiérarchique : étoile -> planètes (chaîne) et planète -> lunes (vertical) et lune -> lunes de lune (horizontal)
     */
    private void drawConnectionsHierarchical(Map<Integer, ACelesteBody> bodiesMap,
                                            List<ACelesteBody> stars,
                                            Map<ACelesteBody, List<ACelesteBody>> starToPlanets,
                                            Map<ACelesteBody, List<ACelesteBody>> planetToMoons,
                                            Map<ACelesteBody, List<ACelesteBody>> moonToSubMoons) {
        for (ACelesteBody star : stars) {
            List<ACelesteBody> planets = starToPlanets.get(star);
            if (planets == null || planets.isEmpty()) {
                continue;
            }

            BodyPosition starPos = bodyPositions.get(star.getBodyID());
            if (starPos == null) continue;

            // Ligne de l'étoile vers la première planète
            ACelesteBody firstPlanet = planets.get(0);
            BodyPosition firstPlanetPos = bodyPositions.get(firstPlanet.getBodyID());
            if (firstPlanetPos != null) {
                Line connection = new Line();
                connection.setStartX(starPos.x);
                connection.setStartY(starPos.y);
                connection.setEndX(firstPlanetPos.x);
                connection.setEndY(firstPlanetPos.y);
                connection.getStyleClass().add("exploration-visual-connection");
                bodiesPane.getChildren().add(0, connection);
            }

            // Lignes entre planètes consécutives (chaîne horizontale)
            for (int i = 1; i < planets.size(); i++) {
                BodyPosition prevPos = bodyPositions.get(planets.get(i - 1).getBodyID());
                BodyPosition currPos = bodyPositions.get(planets.get(i).getBodyID());
                if (prevPos != null && currPos != null) {
                    Line connection = new Line();
                    connection.setStartX(prevPos.x);
                    connection.setStartY(prevPos.y);
                    connection.setEndX(currPos.x);
                    connection.setEndY(currPos.y);
                    connection.getStyleClass().add("exploration-visual-connection");
                    bodiesPane.getChildren().add(0, connection);
                }
            }

            // Lignes entre planètes et leurs lunes (vertical)
            for (ACelesteBody planet : planets) {
                List<ACelesteBody> moons = planetToMoons.get(planet);
                if (moons != null && !moons.isEmpty()) {
                    BodyPosition planetPos = bodyPositions.get(planet.getBodyID());
                    if (planetPos != null) {
                        // Ligne de la planète vers sa première lune
                        ACelesteBody firstMoon = moons.get(0);
                        BodyPosition firstMoonPos = bodyPositions.get(firstMoon.getBodyID());
                        if (firstMoonPos != null) {
                            Line connection = new Line();
                            connection.setStartX(planetPos.x);
                            connection.setStartY(planetPos.y);
                            connection.setEndX(firstMoonPos.x);
                            connection.setEndY(firstMoonPos.y);
                            connection.getStyleClass().add("exploration-visual-connection");
                            bodiesPane.getChildren().add(0, connection);
                        }

                        // Lignes entre lunes consécutives (chaîne verticale)
                        for (int i = 1; i < moons.size(); i++) {
                            BodyPosition prevMoonPos = bodyPositions.get(moons.get(i - 1).getBodyID());
                            BodyPosition currMoonPos = bodyPositions.get(moons.get(i).getBodyID());
                            if (prevMoonPos != null && currMoonPos != null) {
                                Line connection = new Line();
                                connection.setStartX(prevMoonPos.x);
                                connection.setStartY(prevMoonPos.y);
                                connection.setEndX(currMoonPos.x);
                                connection.setEndY(currMoonPos.y);
                                connection.getStyleClass().add("exploration-visual-connection");
                                bodiesPane.getChildren().add(0, connection);
                            }
                        }

                        // Lignes entre lunes et leurs sub-lunes (horizontal)
                        for (ACelesteBody moon : moons) {
                            List<ACelesteBody> subMoons = moonToSubMoons.get(moon);
                            if (subMoons != null && !subMoons.isEmpty()) {
                                BodyPosition moonPos = bodyPositions.get(moon.getBodyID());
                                if (moonPos != null) {
                                    // Ligne de la lune vers sa première sub-lune
                                    ACelesteBody firstSubMoon = subMoons.get(0);
                                    BodyPosition firstSubMoonPos = bodyPositions.get(firstSubMoon.getBodyID());
                                    if (firstSubMoonPos != null) {
                                        Line connection = new Line();
                                        connection.setStartX(moonPos.x);
                                        connection.setStartY(moonPos.y);
                                        connection.setEndX(firstSubMoonPos.x);
                                        connection.setEndY(firstSubMoonPos.y);
                                        connection.getStyleClass().add("exploration-visual-connection");
                                        bodiesPane.getChildren().add(0, connection);
                                    }

                                    // Lignes entre sub-lunes consécutives (chaîne horizontale)
                                    for (int i = 1; i < subMoons.size(); i++) {
                                        BodyPosition prevSubMoonPos = bodyPositions.get(subMoons.get(i - 1).getBodyID());
                                        BodyPosition currSubMoonPos = bodyPositions.get(subMoons.get(i).getBodyID());
                                        if (prevSubMoonPos != null && currSubMoonPos != null) {
                                            Line connection = new Line();
                                            connection.setStartX(prevSubMoonPos.x);
                                            connection.setStartY(prevSubMoonPos.y);
                                            connection.setEndX(currSubMoonPos.x);
                                            connection.setEndY(currSubMoonPos.y);
                                            connection.getStyleClass().add("exploration-visual-connection");
                                            bodiesPane.getChildren().add(0, connection);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Trouve l'étoile parente d'un corps (remonte la chaîne de parents)
     */
    private ACelesteBody findParentStar(ACelesteBody body, Map<Integer, ACelesteBody> bodiesMap) {
        var parents = body.getParents();
        if (parents == null || parents.isEmpty()) {
            return null;
        }

        for (var parent : parents) {
            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
            if (parentBody != null) {
                if (parentBody instanceof StarDetail) {
                    var starParents = parentBody.getParents();
                    if (starParents == null || starParents.isEmpty() ||
                        starParents.stream().anyMatch(p -> "Null".equalsIgnoreCase(p.getType()))) {
                        return parentBody;
                    }
                }
                // Récursivement chercher l'étoile parente
                ACelesteBody star = findParentStar(parentBody, bodiesMap);
                if (star != null) {
                    return star;
                }
            }
        }
        return null;
    }

    /**
     * Trie les corps selon la hiérarchie orrery (comme dans SystemCardController)
     */
    private List<ACelesteBody> sortBodiesHierarchically(Collection<ACelesteBody> bodies) {
        List<ACelesteBody> result = new ArrayList<>();
        Set<Integer> processed = new HashSet<>();

        // Identifier les soleils
        List<ACelesteBody> stars = bodies.stream()
                .filter(body -> body instanceof StarDetail)
                .filter(body -> {
                    var parents = body.getParents();
                    return parents == null || parents.isEmpty() ||
                           parents.stream().anyMatch(p -> "Null".equalsIgnoreCase(p.getType()));
                })
                .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                .collect(Collectors.toList());

        // Pour chaque soleil, ajouter le soleil puis ses enfants récursivement
        for (ACelesteBody star : stars) {
            if (!processed.contains(star.getBodyID())) {
                result.add(star);
                processed.add(star.getBodyID());
                addChildrenRecursively(star, bodies, result, processed);
            }
        }

        // Ajouter les corps orphelins
        bodies.stream()
                .filter(body -> !processed.contains(body.getBodyID()))
                .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                .forEach(body -> {
                    result.add(body);
                    processed.add(body.getBodyID());
                    addChildrenRecursively(body, bodies, result, processed);
                });

        return result;
    }

    /**
     * Ajoute récursivement les enfants d'un parent
     */
    private void addChildrenRecursively(ACelesteBody parent, Collection<ACelesteBody> allBodies,
                                       List<ACelesteBody> result, Set<Integer> processed) {
        List<ACelesteBody> children = allBodies.stream()
                .filter(body -> !processed.contains(body.getBodyID()))
                .filter(body -> {
                    var parents = body.getParents();
                    if (parents == null || parents.isEmpty()) {
                        return false;
                    }
                    return parents.stream().anyMatch(p -> p.getBodyID() == parent.getBodyID());
                })
                .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                .collect(Collectors.toList());

        for (ACelesteBody child : children) {
            if (!processed.contains(child.getBodyID())) {
                result.add(child);
                processed.add(child.getBodyID());
                addChildrenRecursively(child, allBodies, result, processed);
            }
        }
    }

    /**
     * Calcule la profondeur hiérarchique d'un corps
     */
    private int calculateDepth(ACelesteBody body, Map<Integer, ACelesteBody> bodiesMap) {
        var parents = body.getParents();
        if (parents == null || parents.isEmpty()) {
            return 0;
        }

        for (var parent : parents) {
            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
            if (parentBody != null) {
                return calculateDepth(parentBody, bodiesMap) + 1;
            }
        }

        return 0;
    }

    /**
     * Met à jour la liste des corps à gauche
     */
    private void updateBodiesList(SystemVisited system) {
        if (bodiesListContainer == null) {
            return;
        }
        
        bodiesListContainer.getChildren().clear();
        
        if (system == null || system.getCelesteBodies() == null || system.getCelesteBodies().isEmpty()) {
            return;
        }
        
        // Créer une map pour lookup rapide
        Map<Integer, ACelesteBody> bodiesMap = system.getCelesteBodies().stream()
                .collect(Collectors.toMap(ACelesteBody::getBodyID, body -> body));
        
        // Trier les corps hiérarchiquement
        List<ACelesteBody> sortedBodies = sortBodiesHierarchically(system.getCelesteBodies());
        
        // Créer une map pour savoir si un corps a des frères suivants au même niveau
        Map<Integer, Boolean> hasNextSibling = new HashMap<>();
        
        // Pour chaque corps, trouver son parent direct et vérifier s'il a des frères suivants
        for (int i = 0; i < sortedBodies.size(); i++) {
            ACelesteBody body = sortedBodies.get(i);
            int depth = calculateBodyDepth(body, bodiesMap);
            
            // Trouver le parent direct (celui qui est à depth-1)
            ACelesteBody directParent = findDirectParent(body, bodiesMap, depth);
            
            if (directParent != null) {
                // Trouver tous les frères de ce corps (enfants du même parent au même niveau)
                List<ACelesteBody> siblings = sortedBodies.stream()
                        .filter(b -> {
                            int bDepth = calculateBodyDepth(b, bodiesMap);
                            if (bDepth != depth) return false;
                            ACelesteBody bParent = findDirectParent(b, bodiesMap, bDepth);
                            return bParent != null && bParent.getBodyID() == directParent.getBodyID();
                        })
                        .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                        .collect(Collectors.toList());
                
                // Trouver l'index de ce corps dans ses frères
                int siblingIndex = siblings.indexOf(body);
                hasNextSibling.put(body.getBodyID(), siblingIndex >= 0 && siblingIndex < siblings.size() - 1);
            } else {
                // Pas de parent, donc pas de frère suivant
                hasNextSibling.put(body.getBodyID(), false);
            }
        }
        
        // Créer une map pour savoir si un parent a des frères suivants (pour les lignes verticales)
        Map<Integer, Boolean> parentHasNextSibling = new HashMap<>();
        for (ACelesteBody body : sortedBodies) {
            int depth = calculateBodyDepth(body, bodiesMap);
            for (int d = 0; d < depth; d++) {
                final int level = d; // Variable finale pour la lambda
                // Pour chaque niveau, trouver le parent à ce niveau
                ACelesteBody parentAtLevel = findParentAtDepth(body, bodiesMap, level);
                if (parentAtLevel != null && !parentHasNextSibling.containsKey(parentAtLevel.getBodyID())) {
                    // Vérifier si ce parent a un frère suivant
                    ACelesteBody parentParent = findDirectParent(parentAtLevel, bodiesMap, level);
                    if (parentParent != null) {
                        List<ACelesteBody> parentSiblings = sortedBodies.stream()
                                .filter(b -> {
                                    int bDepth = calculateBodyDepth(b, bodiesMap);
                                    if (bDepth != level) return false;
                                    ACelesteBody bParent = findDirectParent(b, bodiesMap, bDepth);
                                    return bParent != null && bParent.getBodyID() == parentParent.getBodyID();
                                })
                                .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                                .collect(Collectors.toList());
                        int parentSiblingIndex = parentSiblings.indexOf(parentAtLevel);
                        parentHasNextSibling.put(parentAtLevel.getBodyID(), 
                                parentSiblingIndex >= 0 && parentSiblingIndex < parentSiblings.size() - 1);
                    }
                }
            }
        }
        
        // Filtrer les corps si la checkbox est cochée
        List<ACelesteBody> filteredBodies = sortedBodies;
        if (showOnlyHighValueBodiesCheckBox != null && showOnlyHighValueBodiesCheckBox.isSelected()) {
            filteredBodies = sortedBodies.stream()
                    .filter(this::isHighValueBody)
                    .collect(Collectors.toList());
        }
        
        // Créer les cartes avec les lignes hiérarchiques
        for (int i = 0; i < filteredBodies.size(); i++) {
            ACelesteBody body = filteredBodies.get(i);
            int depth = calculateBodyDepth(body, bodiesMap);
            boolean hasNext = hasNextSibling.getOrDefault(body.getBodyID(), false);
            
            // Créer une map pour chaque niveau de profondeur
            Map<Integer, Boolean> levelHasNext = new HashMap<>();
            for (int d = 0; d < depth; d++) {
                ACelesteBody parentAtLevel = findParentAtDepth(body, bodiesMap, d);
                if (parentAtLevel != null) {
                    levelHasNext.put(d, parentHasNextSibling.getOrDefault(parentAtLevel.getBodyID(), false));
                }
            }
            levelHasNext.put(depth, hasNext);
            
            VBox card = createBodyCard(body, depth, levelHasNext, bodiesMap);
            if (card != null) {
                bodiesListContainer.getChildren().add(card);
            }
        }
    }
    
    /**
     * Calcule la profondeur hiérarchique d'un corps (nombre de niveaux de parents)
     */
    private int calculateBodyDepth(ACelesteBody body, Map<Integer, ACelesteBody> bodiesMap) {
        var parents = body.getParents();
        if (parents == null || parents.isEmpty()) {
            return 0;
        }
        
        int maxDepth = 0;
        for (var parent : parents) {
            if ("Null".equalsIgnoreCase(parent.getType())) {
                continue;
            }
            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
            if (parentBody != null) {
                int parentDepth = calculateBodyDepth(parentBody, bodiesMap);
                maxDepth = Math.max(maxDepth, parentDepth + 1);
            }
        }
        
        return maxDepth;
    }
    
    /**
     * Trouve le parent direct d'un corps (celui qui est à depth-1)
     */
    private ACelesteBody findDirectParent(ACelesteBody body, Map<Integer, ACelesteBody> bodiesMap, int depth) {
        if (depth == 0) {
            return null;
        }
        
        var parents = body.getParents();
        if (parents == null || parents.isEmpty()) {
            return null;
        }
        
        // Trouver le parent qui a la profondeur depth-1
        for (var parent : parents) {
            if ("Null".equalsIgnoreCase(parent.getType())) {
                continue;
            }
            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
            if (parentBody != null) {
                int parentDepth = calculateBodyDepth(parentBody, bodiesMap);
                if (parentDepth == depth - 1) {
                    return parentBody;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Trouve le parent d'un corps à un niveau de profondeur donné
     */
    private ACelesteBody findParentAtDepth(ACelesteBody body, Map<Integer, ACelesteBody> bodiesMap, int targetDepth) {
        if (targetDepth < 0) {
            return null;
        }
        
        int currentDepth = calculateBodyDepth(body, bodiesMap);
        if (targetDepth >= currentDepth) {
            return null;
        }
        
        // Remonter la chaîne de parents jusqu'au niveau cible
        ACelesteBody current = body;
        int currentD = currentDepth;
        
        while (currentD > targetDepth && current != null) {
            ACelesteBody parent = findDirectParent(current, bodiesMap, currentD);
            if (parent == null) {
                break;
            }
            current = parent;
            currentD--;
        }
        
        return (currentD == targetDepth) ? current : null;
    }
    
    /**
     * Crée une carte pour un corps céleste avec toutes les informations
     */
    private VBox createBodyCard(ACelesteBody body, int depth, Map<Integer, Boolean> levelHasNext, Map<Integer, ACelesteBody> bodiesMap) {
        VBox card = new VBox(5);
        card.getStyleClass().add("exploration-body-card");
        
        // Créer un conteneur pour les lignes hiérarchiques et le contenu
        HBox mainContainer = new HBox(0);
        mainContainer.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        // Créer les lignes verticales pour la hiérarchie
        VBox hierarchyLines = new VBox(0);
        hierarchyLines.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        hierarchyLines.setMinWidth(20 * depth);
        hierarchyLines.setPrefWidth(20 * depth);
        hierarchyLines.setMaxWidth(20 * depth);

        
        // Contenu de la carte
        VBox cardContent = new VBox(5);
        cardContent.setPadding(new javafx.geometry.Insets(8));
        
        // Nom du corps
        HBox headerRow = new HBox(5);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        String bodyName = getBodyNameWithoutSystem(body);
        Label nameLabel = new Label(bodyName);
        nameLabel.getStyleClass().add("exploration-body-card-name");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        headerRow.getChildren().add(nameLabel);
        
        // Ajouter l'icône mapped si nécessaire (pour les planètes)
        if (body instanceof PlaneteDetail planet) {
            // Vérifier si la planète respecte les conditions pour mapped
            boolean shouldShowMappedIcon = false;
            boolean isMapped = planet.isMapped();
            
            if (isMapped) {
                shouldShowMappedIcon = true;
            } else if (planet.getPlanetClass() != null) {
                int baseK = planet.getPlanetClass().getBaseK();
                shouldShowMappedIcon = planet.isTerraformable() || baseK > 50000;
            }
            
            if (shouldShowMappedIcon && mappedImage != null) {
                ImageView mappedIconView = new ImageView(mappedImage);
                mappedIconView.setFitWidth(20);
                mappedIconView.setFitHeight(20);
                mappedIconView.setPreserveRatio(true);
                headerRow.getChildren().add(mappedIconView);
                
                // Ajouter l'indicateur de statut
                Label statusLabel = new Label();
                if (isMapped) {
                    statusLabel.setText("✓");
                    statusLabel.setStyle("-fx-text-fill: #00FF00; -fx-font-size: 15px; -fx-font-weight: bold;");
                } else {
                    statusLabel.setText("✗");
                    statusLabel.setStyle("-fx-text-fill: #FF0000; -fx-font-size: 15px; -fx-font-weight: bold;");
                }
                headerRow.getChildren().add(statusLabel);
                
                // Ajouter "FIRST" si firstMapped (wasMapped est false)
                if (!planet.isWasMapped()) {
                    Label firstMappedLabel = new Label("FIRST");
                    firstMappedLabel.getStyleClass().add("exploration-body-first-discovery");
                    headerRow.getChildren().add(firstMappedLabel);
                }
                //PRICE
                long bodyValue = planet.computeBodyValue();
                Label price = new Label(String.format("%,d Cr", bodyValue));
                price.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14px; -fx-font-weight: bold;");
                headerRow.getChildren().add(price);
            }
        }
        
        cardContent.getChildren().add(headerRow);
        
        // Informations exobio (X/Y) - seulement pour les planètes
        if (body instanceof PlaneteDetail planet) {
            // Calculer le nombre d'espèces collectées et détectées
            int confirmedSpeciesCount = 0;
            int numSpeciesDetected = 0;
            
            if (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()) {
                confirmedSpeciesCount = (int) planet.getConfirmedSpecies().stream()
                        .filter(species -> species.isCollected())
                        .count();
            }
            if (planet.getNumSpeciesDetected() != null) {
                numSpeciesDetected = planet.getNumSpeciesDetected();
            }
            
            // Afficher l'info exobio si nécessaire
            if (exobioImage != null && 
                ((planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) ||
                 (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()))) {
                
                // Conteneur pour l'info exobio avec fond visible - remplir tout le panneau
                HBox exobioContainer = new HBox(10);
                exobioContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                exobioContainer.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
                exobioContainer.getStyleClass().add("exploration-body-exobio-container");
                exobioContainer.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(exobioContainer, javafx.scene.layout.Priority.ALWAYS);
                
                ImageView exobioIconView = new ImageView(exobioImage);
                exobioIconView.setFitWidth(20);
                exobioIconView.setFitHeight(20);
                exobioIconView.setPreserveRatio(true);
                exobioContainer.getChildren().add(exobioIconView);
                
                // Déterminer la couleur selon le nombre d'espèces collectées
                String color;
                if (confirmedSpeciesCount == 0) {
                    color = "#FF4444";
                } else if (numSpeciesDetected > 0 && confirmedSpeciesCount == numSpeciesDetected) {
                    color = "#00FF88";
                } else {
                    color = "#FF8800";
                }
                
                Label speciesCountLabel = new Label(String.format("%d/%d", confirmedSpeciesCount, numSpeciesDetected));
                speciesCountLabel.setStyle(String.format(
                    "-fx-text-fill: %s; " +
                    "-fx-font-size: 15px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 5px 10px;",
                    color));
                speciesCountLabel.getStyleClass().add("exploration-body-exobio-count");
                exobioContainer.getChildren().add(speciesCountLabel);
                
                // Ajouter un label si wasFootfalled est false (première découverte)
                if (!planet.isWasFootfalled()) {
                    Label firstDiscoveryLabel = new Label("FIRST");
                    firstDiscoveryLabel.getStyleClass().add("exploration-body-first-discovery");
                    exobioContainer.getChildren().add(firstDiscoveryLabel);
                }
                
                cardContent.getChildren().add(exobioContainer);
            }
            
            // Liste des BioSpecies avec probabilités
            if (planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) {
                VBox speciesList = new VBox(4);
                speciesList.setPadding(new javafx.geometry.Insets(8, 8, 8, 8));
                speciesList.getStyleClass().add("exploration-body-species-list");
                
                // Créer une map des confirmedSpecies par nom pour lookup rapide
                Map<String, BioSpecies> confirmedSpeciesMap = new HashMap<>();
                if (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()) {
                    for (BioSpecies confirmed : planet.getConfirmedSpecies()) {
                        if (confirmed.getName() != null) {
                            confirmedSpeciesMap.put(confirmed.getName(), confirmed);
                        }
                    }
                }
                
                // Prendre le scan le plus récent (niveau le plus élevé)
                Scan latestScan = planet.getBioSpecies().stream()
                        .max(Comparator.comparingInt(Scan::getScanNumber))
                        .orElse(null);
                
                if (latestScan != null && latestScan.getSpeciesProbabilities() != null) {
                    // Set pour tracker les noms déjà traités avec confirmedSpecies
                    Set<String> processedConfirmedNames = new HashSet<>();
                    
                    for (SpeciesProbability sp : latestScan.getSpeciesProbabilities()) {
                        BioSpecies bioSpecies = sp.getBioSpecies();
                        BioSpecies confirmedSpecies = null;
                        String bioSpeciesName = bioSpecies.getName();
                        
                        // Vérifier si une confirmedSpecies a le même nom
                        if (bioSpeciesName != null && confirmedSpeciesMap.containsKey(bioSpeciesName)) {
                            // Si on n'a pas encore traité ce nom avec une confirmedSpecies, on le remplace
                            if (!processedConfirmedNames.contains(bioSpeciesName)) {
                                confirmedSpecies = confirmedSpeciesMap.get(bioSpeciesName);
                                processedConfirmedNames.add(bioSpeciesName);
                            } else {
                                // Sinon, on ignore ce bioSpecies (doublon)
                                continue;
                            }
                        }
                        
                        HBox speciesRow = new HBox(8);
                        speciesRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        speciesRow.setPadding(new javafx.geometry.Insets(4, 6, 4, 6));
                        speciesRow.getStyleClass().add("exploration-body-species-row");
                        
                        // Afficher le nom (confirmedSpecies si disponible, sinon bioSpecies)
                        String speciesName;
                        if (confirmedSpecies != null) {
                            speciesName = confirmedSpecies.getFullName();
                        } else {
                            speciesName = bioSpecies.getFullName();
                        }
                        
                        Label speciesNameLabel = new Label(speciesName);
                        speciesNameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-elite-text;");
                        speciesNameLabel.setMaxWidth(Double.MAX_VALUE);
                        speciesNameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                        HBox.setHgrow(speciesNameLabel, javafx.scene.layout.Priority.ALWAYS);
                        
                        // Afficher ✓ ou ✗ si confirmedSpecies, sinon le pourcentage
                        Label statusLabel = new Label();
                        if (confirmedSpecies != null) {
                            if (confirmedSpecies.isCollected()) {
                                statusLabel.setText("✓");
                                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #00FF00; -fx-font-weight: bold;");
                            } else {
                                statusLabel.setText("✗");
                                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FF0000; -fx-font-weight: bold;");
                            }
                        } else {
                            statusLabel.setText(String.format("%.1f%%", sp.getProbability()));
                            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #00FF88; -fx-font-weight: bold;");
                            statusLabel.getStyleClass().add("exploration-body-species-prob");
                        }
                        // S'assurer que le pourcentage ne soit jamais coupé - pas de contrainte de largeur
                        statusLabel.setMinWidth(0);
                        statusLabel.setPrefWidth(-1);
                        statusLabel.setMaxWidth(Double.MAX_VALUE);
                        
                        // Calculer et afficher le prix
                        // Utiliser confirmedSpecies si disponible, sinon bioSpecies
                        BioSpecies speciesForPrice = (confirmedSpecies != null) ? confirmedSpecies : bioSpecies;
                        long price;
                        if (!planet.isWasFootfalled()) {
                            // Si wasFootfalled est false, prendre bonusValue
                            price = speciesForPrice.getBonusValue();
                        } else {
                            // Sinon, prendre baseValue
                            price = speciesForPrice.getBaseValue();
                        }
                        
                        Label priceLabel = new Label(String.format("%,d Cr", price));
                        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFD700; -fx-font-weight: bold;");
                        // S'assurer que le prix ne soit jamais coupé - pas de contrainte de largeur
                        priceLabel.setMinWidth(0);
                        priceLabel.setPrefWidth(-1);
                        priceLabel.setMaxWidth(Double.MAX_VALUE);
                        statusLabel.setMinWidth(Region.USE_PREF_SIZE);
                        priceLabel.setMinWidth(Region.USE_PREF_SIZE);
                        speciesRow.getChildren().addAll(speciesNameLabel, statusLabel, priceLabel);
                        speciesList.getChildren().add(speciesRow);
                    }
                }
                
                if (!speciesList.getChildren().isEmpty()) {
                    cardContent.getChildren().add(speciesList);
                }
            }
        }
        
        // Assembler le tout
        if (depth > 0) {
            mainContainer.getChildren().add(hierarchyLines);
        }
        mainContainer.getChildren().add(cardContent);
        card.getChildren().add(mainContainer);
        
        return card;
    }
    
    /**
     * Classe pour stocker la position d'un corps
     */
    private static class BodyPosition {
        double x, y;
        ACelesteBody body;

        BodyPosition(double x, double y, ACelesteBody body) {
            this.x = x;
            this.y = y;
            this.body = body;
        }
    }

    /**
     * Vérifie si un corps est "high value" (contient exobiologie ou est mappable)
     */
    private boolean isHighValueBody(ACelesteBody body) {
        // Vérifier l'exobiologie
        if (body instanceof PlaneteDetail planet) {
            boolean hasExobio = (planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) ||
                               (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty());
            
            if (hasExobio) {
                return true;
            }
            
            // Vérifier si mappable (terraformable ou baseK > 50000)
            if (planet.getPlanetClass() != null) {
                int baseK = planet.getPlanetClass().getBaseK();
                return planet.isTerraformable() || baseK > 50000;
            }
        }
        
        return false;
    }
    
    /**
     * Gère le changement de la checkbox de filtre
     */
    @FXML
    private void onFilterChanged() {
        if (currentSystem != null) {
            updateBodiesList(currentSystem);
        }
    }
    
    /**
     * Efface l'affichage
     */
    private void clearDisplay() {
        Platform.runLater(() -> {
            bodiesPane.getChildren().clear();
            if (bodiesListContainer != null) {
                bodiesListContainer.getChildren().clear();
            }
            currentSystem = null;
        });
    }
}

