package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.StarDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour une carte système dans la liste des systèmes visités
 */
public class SystemCardController implements Initializable {

    @FXML
    private VBox root;
    @FXML
    private Label systemNameLabel;
    @FXML
    private VBox bodiesContainer;

    private SystemVisited system;
    private boolean expanded = false;
    private final PlaneteRegistry planeteRegistry = PlaneteRegistry.getInstance();
    private java.util.function.Consumer<SystemVisited> onSystemClicked;
    private Runnable onCardExpanded;
    private Image exobioImage;
    private Image mappedImage;
    
    // Méthodes pour initialisation manuelle (sans FXML)
    public void setRoot(VBox root) {
        this.root = root;
    }
    
    public void setSystemNameLabel(Label systemNameLabel) {
        this.systemNameLabel = systemNameLabel;
    }
    
    public void setBodiesContainer(VBox bodiesContainer) {
        this.bodiesContainer = bodiesContainer;
    }
    
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (root != null) {
            root.setOnMouseClicked(this::onCardClicked);
        }
        loadImages();
    }
    
    private void loadImages() {
        // Charger les images (une seule fois)
        if (exobioImage == null) {
            try {
                exobioImage = new Image(getClass().getResourceAsStream("/images/exploration/exobio.png"));
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de l'image exobio.png: " + e.getMessage());
            }
        }
        if (mappedImage == null) {
            try {
                mappedImage = new Image(getClass().getResourceAsStream("/images/exploration/mapped.png"));
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de l'image mapped.png: " + e.getMessage());
            }
        }
    }

    public void setSystem(SystemVisited system) {
        this.system = system;
        if (system != null && systemNameLabel != null) {
            systemNameLabel.setText(system.getSystemName());
            //loadImages(); // S'assurer que les images sont chargées
            updateBodiesList();
        }
    }

    private void onCardClicked(MouseEvent event) {
        boolean wasExpanded = expanded;
        expanded = !expanded;
        updateExpandedState();
        
        // Si la carte vient d'être ouverte, notifier pour fermer les autres
        if (expanded && !wasExpanded && onCardExpanded != null) {
            onCardExpanded.run();
        }
        
        // Notifier le clic sur le système pour afficher dans la vue visuelle
        if (onSystemClicked != null && system != null) {
            onSystemClicked.accept(system);
        }
    }
    
    public void setOnSystemClicked(java.util.function.Consumer<SystemVisited> callback) {
        this.onSystemClicked = callback;
    }
    
    public void setOnCardExpanded(Runnable callback) {
        this.onCardExpanded = callback;
    }
    
    /**
     * Définit l'état d'expansion de la carte
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        updateExpandedState();
    }

    private void updateExpandedState() {
        bodiesContainer.setVisible(expanded);
        bodiesContainer.setManaged(expanded);
        
        if (expanded) {
            root.getStyleClass().add("exploration-system-card-expanded");
        } else {
            root.getStyleClass().remove("exploration-system-card-expanded");
        }
    }

    private void updateBodiesList() {
        bodiesContainer.getChildren().clear();
        
        if (system == null || system.getCelesteBodies() == null || system.getCelesteBodies().isEmpty()) {
            return;
        }

        // Créer une map pour lookup rapide par bodyID
        Map<Integer, ACelesteBody> bodiesMap = system.getCelesteBodies().stream()
                .collect(Collectors.toMap(ACelesteBody::getBodyID, body -> body));

        // Trier selon la hiérarchie orrery
        List<ACelesteBody> sortedBodies = sortBodiesHierarchically(system.getCelesteBodies());

        // Afficher avec indentation
        for (ACelesteBody body : sortedBodies) {
            int depth = calculateDepth(body, bodiesMap);
            createBodyLabel(body, depth);
        }
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
     * Trie les corps selon la hiérarchie orrery (soleils en premier, puis leurs enfants récursivement)
     */
    private List<ACelesteBody> sortBodiesHierarchically(Collection<ACelesteBody> bodies) {
        List<ACelesteBody> result = new ArrayList<>();
        Set<Integer> processed = new HashSet<>();
        
        // Identifier les soleils (sans parent ou avec parent "Null")
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
     * Calcule la profondeur hiérarchique d'un corps (0 = soleil, 1 = planète, 2 = lune, etc.)
     */
    private int calculateDepth(ACelesteBody body, Map<Integer, ACelesteBody> bodiesMap) {
        var parents = body.getParents();
        if (parents == null || parents.isEmpty()) {
            return 0; // C'est un soleil ou un corps sans parent
        }

        // Trouver le parent direct (le premier dans la liste qui existe dans notre map)
        for (var parent : parents) {
            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
            if (parentBody != null) {
                return calculateDepth(parentBody, bodiesMap) + 1;
            }
        }

        return 0;
    }

    /**
     * Crée un label pour un corps céleste avec l'indentation appropriée
     */
    private void createBodyLabel(ACelesteBody body, int depth) {
        HBox container = new HBox(5);
        container.getStyleClass().add("exploration-system-card-body-container");

        // Indentation
        Region indent = new Region();
        indent.setMinWidth(depth * 20); // 20px par niveau
        indent.setMaxWidth(depth * 20);
        container.getChildren().add(indent);

        // Label avec le nom du corps
        Label bodyLabel = new Label();
        
        String bodyType = "";
        if (body instanceof StarDetail) {
            bodyType = "★ ";
        } else if (body instanceof PlaneteDetail) {
            bodyType = "● ";
        }
        
        bodyLabel.setText(bodyType + getBodyNameWithoutSystem(body));
        bodyLabel.getStyleClass().add("exploration-system-card-body");
        
        // Ajouter des infos supplémentaires si c'est une planète avec exobio
        if (body instanceof PlaneteDetail planet) {
            if (planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) {
                bodyLabel.setText(bodyLabel.getText() + " (Exobio)");
                bodyLabel.getStyleClass().add("exploration-system-card-body-exobio");
            }
        }
        
        container.getChildren().add(bodyLabel);
        
        bodiesContainer.getChildren().add(container);
    }
}

