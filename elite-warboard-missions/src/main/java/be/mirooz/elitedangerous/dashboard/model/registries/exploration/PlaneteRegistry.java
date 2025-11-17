package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.BiologicalSignalProcessor;
import be.mirooz.elitedangerous.dashboard.model.exploration.ParentBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.StarDetail;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Singleton pour stocker les détails des planètes scannées, observable par la UI.
 * Utilise le bodyID comme clé unique.
 */
@Data
public class PlaneteRegistry {

    private static final PlaneteRegistry INSTANCE = new PlaneteRegistry();

    private final ObservableMap<Integer, ACelesteBody> planetesMap =
            FXCollections.observableHashMap();

    private PlaneteRegistry() {
    }
    private String currentStarSystem;

    public static PlaneteRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Ajoute ou met à jour une planète dans le registre.
     * Utilise le bodyID comme clé unique.
     */
    public void addOrUpdateBody(ACelesteBody body) {
        ACelesteBody existing = planetesMap.get(body.getBodyID());

        if (existing instanceof PlaneteDetail oldP
                && body instanceof PlaneteDetail newP) {
            // Au lieu de remplacer l'objet, on met juste à jour les champs
            oldP.updateFrom(newP);
            return;
        }

        planetesMap.put(body.getBodyID(), body);
        getSortedBodiesForOrrery();
    }


    /**
     * Récupère une planète par son bodyID.
     */
    public Optional<ACelesteBody> getByBodyID(int bodyID) {
        return Optional.ofNullable(planetesMap.get(bodyID));
    }

    /**
     * Récupère une planète par son nom et système stellaire.
     */
    public Optional<ACelesteBody> getPlaneteByName(String bodyName, String starSystem) {
        return planetesMap.values().stream()
                .filter(p -> p.getBodyName().equals(bodyName) && p.getStarSystem().equals(starSystem))
                .findFirst();
    }

    /**
     * Récupère toutes les planètes d'un système stellaire.
     */
    public java.util.List<ACelesteBody> getPlanetesBySystem(String starSystem) {
        return planetesMap.values().stream()
                .filter(p -> p.getStarSystem().equals(starSystem))
                .toList();
    }

    /**
     * Ajoute un listener pour les changements du registre.
     */
    public void addPlaneteMapListener(Runnable action) {
        planetesMap.addListener((MapChangeListener<Integer, ACelesteBody>) change -> {
            if (!DashboardContext.getInstance().isBatchLoading()) {
                Platform.runLater(action);
            }
        });
    }

    /**
     * Vide le registre.
     */
    public void clear() {
        planetesMap.clear();
        BiologicalSignalProcessor.getInstance().clear();
        currentStarSystem = null;
    }

    /**
     * Retourne le nombre de planètes dans le registre.
     */
    public int size() {
        return planetesMap.size();
    }

    /**
     * Retourne toutes les planètes.
     */
    public Collection<ACelesteBody> getAllPlanetes() {
        return planetesMap.values();
    }

    public void setAllPlanetes(Collection<ACelesteBody> planetes) {
        clear();
        planetes.forEach( planete ->
                planetesMap.put(planete.getBodyID(), planete)
        );
    }

    /**
     * Retourne une liste triée des corps célestes organisée hiérarchiquement :
     * - Les soleils sont placés à gauche (en premier)
     * - Chaque soleil est suivi de ses orbites organisées récursivement
     * - Les liens parent-enfant sont respectés (planètes → lunes → lunes de lunes, etc.)
     * 
     * Exemple d'organisation :
     * - Soleil 1
     *   - Planète A (orbite autour du Soleil 1)
     *     - Lune A1 (orbite autour de la Planète A)
     *       - Lune A1a (orbite autour de la Lune A1)
     *     - Lune A2 (orbite autour de la Planète A)
     *   - Planète B (orbite autour du Soleil 1)
     * - Soleil 2
     *   - Planète C (orbite autour du Soleil 2)
     * 
     * @return Liste triée avec les soleils en premier, suivis de leurs orbites hiérarchiques
     */
    public List<ACelesteBody> getSortedBodiesForOrrery() {
        List<ACelesteBody> result = new ArrayList<>();
        Set<Integer> processed = new HashSet<>();
        
        // Étape 1 : Identifier tous les soleils (StarDetail sans parent ou avec parent "Null")
        List<ACelesteBody> soleils = planetesMap.values().stream()
                .filter(body -> body instanceof StarDetail)
                .filter(body -> {
                    // Un soleil est un corps sans parent ou avec un parent de type "Null"
                    List<ParentBody> parents = body.getParents();
                    return parents == null || parents.isEmpty() || 
                           parents.stream().anyMatch(p -> "Null".equalsIgnoreCase(p.getType()));
                })
                .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                .collect(Collectors.toList());
        
        // Étape 2 : Pour chaque soleil, ajouter le soleil puis ses orbites récursivement
        for (ACelesteBody soleil : soleils) {
            if (!processed.contains(soleil.getBodyID())) {
                result.add(soleil);
                processed.add(soleil.getBodyID());
                
                // Ajouter récursivement tous les enfants de ce soleil
                // Cela inclut : planètes → lunes → lunes de lunes, etc.
                addChildrenRecursively(soleil, result, processed);
            }
        }
        
        // Étape 3 : Ajouter les corps célestes qui n'ont pas été traités (orphelins)
        // Ces corps n'ont pas de parent soleil identifié, mais peuvent avoir leurs propres enfants
        planetesMap.values().stream()
                .filter(body -> !processed.contains(body.getBodyID()))
                .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                .forEach(body -> {
                    result.add(body);
                    processed.add(body.getBodyID());
                    // Même pour les orphelins, on ajoute récursivement leurs enfants
                    addChildrenRecursively(body, result, processed);
                });
        
        return result;
    }
    
    /**
     * Ajoute récursivement tous les enfants d'un corps céleste parent.
     * Cette méthode gère la hiérarchie complète :
     * - Planètes autour d'un soleil
     * - Lunes autour d'une planète
     * - Lunes de lunes (sub-lunes) autour d'une lune
     * - Et ainsi de suite récursivement
     * 
     * Les enfants sont triés par bodyID pour un ordre cohérent.
     * 
     * @param parent Le corps céleste parent (soleil, planète, lune, etc.)
     * @param result La liste résultat où ajouter les enfants
     * @param processed L'ensemble des bodyID déjà traités pour éviter les doublons
     */
    private void addChildrenRecursively(ACelesteBody parent, List<ACelesteBody> result, Set<Integer> processed) {
        // Trouver tous les corps qui ont ce parent dans leur liste de parents
        // Dans Elite Dangerous, un corps peut avoir plusieurs parents dans sa chaîne
        // (ex: une lune a [Planet, Star] dans sa liste de parents)
        // On cherche si le parent donné est présent dans cette chaîne
        List<ACelesteBody> children = planetesMap.values().stream()
                .filter(body -> !processed.contains(body.getBodyID()))
                .filter(body -> {
                    List<ParentBody> parents = body.getParents();
                    if (parents == null || parents.isEmpty()) {
                        return false;
                    }
                    // Vérifier si ce corps a le parent dans sa chaîne de parents
                    // Cela fonctionne pour : Soleil → Planète, Planète → Lune, Lune → Sub-lune, etc.
                    return parents.stream().anyMatch(p -> p.getBodyID() == parent.getBodyID());
                })
                .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                .collect(Collectors.toList());
        
        // Ajouter chaque enfant et récursivement ses propres enfants
        // Exemple : Si parent = Planète A, on trouve ses lunes, puis pour chaque lune,
        // on cherche récursivement ses propres lunes (sub-lunes)
        for (ACelesteBody child : children) {
            if (!processed.contains(child.getBodyID())) {
                result.add(child);
                processed.add(child.getBodyID());
                // Récursion : si cet enfant a lui-même des enfants (ex: lune avec sub-lunes),
                // ils seront aussi ajoutés à la suite
                addChildrenRecursively(child, result, processed);
            }
        }
    }
}

