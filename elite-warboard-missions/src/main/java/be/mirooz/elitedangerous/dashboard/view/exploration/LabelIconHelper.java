package be.mirooz.elitedangerous.dashboard.view.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Classe utilitaire pour ajouter des icônes (exobio, mapped) à côté d'un label
 */
public class LabelIconHelper {
    
    private static final int ICON_SIZE = 16;
    private static final int ICON_SPACING = 5;
    
    /**
     * Met à jour un label avec des icônes exobio et/ou mapped si nécessaire
     * @param label Le label à mettre à jour
     * @param text Le texte à afficher
     * @param hasExobio Si l'icône exobio doit être affichée
     * @param hasMapped Si l'icône mapped doit être affichée
     * @param exobioImage L'image exobio (peut être null)
     * @param mappedImage L'image mapped (peut être null)
     */
    public static void updateLabelWithIcons(Label label, String text, 
                                           boolean hasExobio, boolean hasMapped,
                                           Image exobioImage, Image mappedImage) {
        if (label == null) {
            return;
        }
        
        // Mettre à jour le texte
        label.setText(text);
        
        // Si pas d'icônes à afficher, s'assurer que le label est directement dans son parent
        if (!hasExobio && !hasMapped) {
            removeIconsContainer(label);
            return;
        }
        
        // Vérifier si le label est déjà dans un HBox
        HBox container = getOrCreateIconContainer(label);
        
        // Retirer les anciennes icônes
        container.getChildren().removeIf(node -> node instanceof ImageView);
        
        // S'assurer que le label est le premier élément
        if (!container.getChildren().contains(label)) {
            container.getChildren().add(0, label);
        }
        
        // Ajouter les icônes
        if (hasExobio && exobioImage != null) {
            ImageView exobioIcon = createIcon(exobioImage);
            container.getChildren().add(exobioIcon);
        }
        
        if (hasMapped && mappedImage != null) {
            ImageView mappedIcon = createIcon(mappedImage);
            container.getChildren().add(mappedIcon);
        }
    }
    
    /**
     * Vérifie si une planète a exobio ou mapped
     */
    public static boolean[] checkPlanetIcons(ACelesteBody body) {
        boolean hasExobio = false;
        boolean hasMapped = false;
        
        if (body instanceof PlaneteDetail planet) {
            // Vérifier les espèces biologiques (bioSpecies) ou les espèces confirmées (confirmedSpecies)
            if ((planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) ||
                (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty())) {
                hasExobio = true;
            }
            if (planet.isMapped()) {
                hasMapped = true;
            }
        }
        
        return new boolean[]{hasExobio, hasMapped};
    }
    
    /**
     * Vérifie si un système a des planètes avec exobio ou mapped
     */
    public static boolean[] checkSystemIcons(java.util.Collection<ACelesteBody> bodies) {
        boolean hasExobio = false;
        boolean hasMapped = false;
        
        if (bodies != null) {
            for (ACelesteBody body : bodies) {
                if (body instanceof PlaneteDetail planet) {
                    if (!hasExobio && 
                        ((planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) ||
                         (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()))) {
                        hasExobio = true;
                    }
                    if (!hasMapped && planet.isMapped()) {
                        hasMapped = true;
                    }
                    if (hasExobio && hasMapped) {
                        break; // On a trouvé les deux, pas besoin de continuer
                    }
                }
            }
        }
        
        return new boolean[]{hasExobio, hasMapped};
    }
    
    /**
     * Obtient ou crée un conteneur HBox pour les icônes
     */
    private static HBox getOrCreateIconContainer(Label label) {
        if (label.getParent() instanceof HBox) {
            return (HBox) label.getParent();
        }
        
        // Créer un nouveau HBox
        HBox container = new HBox(ICON_SPACING);
        container.getStyleClass().add("exploration-label-icon-container");
        
        // Remplacer le label dans son parent par le HBox
        javafx.scene.Parent parent = label.getParent();
        if (parent instanceof Pane pane) {
            int index = pane.getChildren().indexOf(label);
            if (index >= 0) {
                pane.getChildren().remove(label);
                container.getChildren().add(label);
                pane.getChildren().add(index, container);
            }
        } else if (parent instanceof VBox vbox) {
            int index = vbox.getChildren().indexOf(label);
            if (index >= 0) {
                vbox.getChildren().remove(label);
                container.getChildren().add(label);
                vbox.getChildren().add(index, container);
            }
        }
        
        return container;
    }
    
    /**
     * Retire le conteneur d'icônes et remet le label directement dans son parent
     */
    private static void removeIconsContainer(Label label) {
        if (label.getParent() instanceof HBox container) {
            javafx.scene.Parent hboxParent = container.getParent();
            if (hboxParent instanceof Pane pane) {
                int index = pane.getChildren().indexOf(container);
                if (index >= 0) {
                    // Retirer le label du HBox
                    container.getChildren().remove(label);
                    // Retirer le HBox du parent
                    pane.getChildren().remove(container);
                    // Remettre le label directement dans le parent
                    pane.getChildren().add(index, label);
                }
            } else if (hboxParent instanceof VBox vbox) {
                int index = vbox.getChildren().indexOf(container);
                if (index >= 0) {
                    // Retirer le label du HBox
                    container.getChildren().remove(label);
                    // Retirer le HBox du parent
                    vbox.getChildren().remove(container);
                    // Remettre le label directement dans le parent
                    vbox.getChildren().add(index, label);
                }
            }
        }
    }
    
    /**
     * Crée une ImageView pour une icône
     */
    private static ImageView createIcon(Image image) {
        ImageView icon = new ImageView(image);
        icon.setFitWidth(ICON_SIZE);
        icon.setFitHeight(ICON_SIZE);
        icon.setPreserveRatio(true);
        return icon;
    }
}

