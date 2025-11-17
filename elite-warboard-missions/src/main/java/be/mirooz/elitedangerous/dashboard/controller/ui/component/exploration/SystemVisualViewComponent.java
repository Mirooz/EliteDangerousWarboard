package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.dashboard.controller.IRefreshable;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.StarDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
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
    private ScrollPane bodiesScrollPane;
    @FXML
    private Group bodiesGroup;
    @FXML
    private Pane bodiesPane;

    private Image gasImage;
    private Image starImage;
    private SystemVisited currentSystem;
    private Map<Integer, BodyPosition> bodyPositions = new HashMap<>();
    private Scale zoomTransform;
    private double currentZoom = 1.0;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 5.0;
    private static final double ZOOM_FACTOR = 0.1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Charger les images
        try {
            gasImage = new Image(getClass().getResourceAsStream("/images/exploration/gas.png"));
            starImage = new Image(getClass().getResourceAsStream("/images/exploration/star.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des images: " + e.getMessage());
        }
        
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
            maxY = Math.max(maxY, pos.y + bodyRadius);
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
            contentHeight = (maxY - minY) + 100;
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
            this.currentSystem = system;
            bodiesPane.getChildren().clear();
            bodyPositions.clear();
            
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
                        // Le parent direct est le premier parent de type "Planet" ou "Star" (pas "Null")
                        ACelesteBody directParent = null;
                        String directParentType = null;
                        
                        for (var parent : parents) {
                            if ("Null".equalsIgnoreCase(parent.getType())) {
                                continue; // Ignorer les parents "Null"
                            }
                            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
                            if (parentBody != null) {
                                directParent = parentBody;
                                directParentType = parent.getType();
                                break; // Prendre le premier parent non-Null comme parent direct
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
                                boolean isDirectPlanet = false;
                                var parentParents = directParent.getParents();
                                if (parentParents != null && !parentParents.isEmpty()) {
                                    for (var pp : parentParents) {
                                        if ("Star".equalsIgnoreCase(pp.getType())) {
                                            ACelesteBody ppBody = bodiesMap.get(pp.getBodyID());
                                            if (ppBody instanceof StarDetail) {
                                                isDirectPlanet = true;
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
                    
                    for (int i = 0; i < directPlanets.size(); i++) {
                        ACelesteBody planet = directPlanets.get(i);
                        
                        // Positionner la planète
                        bodyPositions.put(planet.getBodyID(), new BodyPosition(planetX, planetY, planet));
                        ImageView planetView = createBodyImageView(planet, planetX, planetY);
                        bodiesPane.getChildren().add(planetView);

                        // Positionner les lunes de cette planète verticalement en dessous
                        List<ACelesteBody> moons = planetToMoons.get(planet);
                        if (moons != null && !moons.isEmpty()) {
                            int moonY = planetY + moonVerticalSpacing;
                            for (ACelesteBody moon : moons) {
                                // Positionner la lune
                                bodyPositions.put(moon.getBodyID(), new BodyPosition(planetX, moonY, moon));
                                ImageView moonView = createBodyImageView(moon, planetX, moonY);
                                bodiesPane.getChildren().add(moonView);

                                // Positionner les lunes de lune (sub-lunes) horizontalement à droite de la lune
                                List<ACelesteBody> subMoons = moonToSubMoons.get(moon);
                                if (subMoons != null && !subMoons.isEmpty()) {
                                    int subMoonX = planetX + horizontalSpacing;
                                    for (ACelesteBody subMoon : subMoons) {
                                        bodyPositions.put(subMoon.getBodyID(), new BodyPosition(subMoonX, moonY, subMoon));
                                        ImageView subMoonView = createBodyImageView(subMoon, subMoonX, moonY);
                                        bodiesPane.getChildren().add(subMoonView);
                                        subMoonX += horizontalSpacing;
                                    }
                                    maxX = Math.max(maxX, subMoonX);
                                }

                                moonY += moonVerticalSpacing;
                            }
                        }

                        planetX += horizontalSpacing;
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
            // Utiliser une taille fixe de 800x600 pour tous les systèmes
            double fixedWidth = 800;
            double fixedHeight = 600;
            bodiesPane.setMinWidth(fixedWidth);
            bodiesPane.setMinHeight(fixedHeight);
            bodiesPane.setPrefWidth(fixedWidth);
            bodiesPane.setPrefHeight(fixedHeight);
            bodiesPane.setMaxWidth(fixedWidth);
            bodiesPane.setMaxHeight(fixedHeight);
            
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
     * Crée une ImageView pour un corps céleste
     */
    private ImageView createBodyImageView(ACelesteBody body, double x, double y) {
        Image image = (body instanceof StarDetail) ? starImage : gasImage;
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
        Tooltip tooltip = new Tooltip(bodyName);
        Tooltip.install(imageView, tooltip);

        imageView.getStyleClass().add("exploration-visual-body");
        if (body instanceof StarDetail) {
            imageView.getStyleClass().add("exploration-visual-star");
        } else if (body instanceof PlaneteDetail) {
            imageView.getStyleClass().add("exploration-visual-planet");
        }

        return imageView;
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
     * Efface l'affichage
     */
    private void clearDisplay() {
        Platform.runLater(() -> {
            bodiesPane.getChildren().clear();
            currentSystem = null;
        });
    }
}

