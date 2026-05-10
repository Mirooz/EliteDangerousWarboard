package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataOnHold;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Data;

/**
 * Registry pour stocker les ventes de données d'exploration.
 * Singleton observable pour la UI.
 */
@Data
public class ExplorationDataSaleRegistry {

    private static final ExplorationDataSaleRegistry INSTANCE = new ExplorationDataSaleRegistry();

    @JsonProperty("sales")
    private List<ExplorationDataSale> sales = FXCollections.observableArrayList();
    
    // Vente en cours (accumule les MultiSellExplorationData jusqu'à Undocked)
    private ExplorationDataSale currentSale = null;

    private ExplorationDataOnHold explorationDataOnHold = null;

    /** Analyse biologique en cours : nom du corps et id d'espèce (persistés avec le registry). */
    @JsonProperty("currentAnalysisBodyName")
    private String currentAnalysisBodyName;
    @JsonProperty("currentAnalysisSpeciesId")
    private String currentAnalysisSpeciesId;

    /** Filtre liste corps (exobio) : un seul {@code bodyID}, ou null. */
    @JsonProperty("filteredBodyID")
    private Integer filteredBodyID;

    private ExplorationDataSaleRegistry() {
    }

    public static ExplorationDataSaleRegistry getInstance() {
        return INSTANCE;
    }

    public void addToOnHold(SystemVisited systemVisited) {
        if (explorationDataOnHold == null) {
            explorationDataOnHold = ExplorationDataOnHold.builder()
                    .startTimeStamp(systemVisited.getLastVisitedTime())
                    .build();
        }
        ExplorationDataOnHold.ensureSystemsVisitedMapIsConcurrent(explorationDataOnHold);
        explorationDataOnHold.getSystemsVisitedMap().put(systemVisited.getSystemName(), systemVisited);
    }
    /**
     * Ajoute ou met à jour la vente en cours avec de nouvelles données.
     */
    public void addToCurrentSale(List<SystemVisited> discoveredSystems,
                                 long baseValue, long bonus, long totalEarnings, String timestamp) {
        if (currentSale == null) {
            // Récupérer le endTimestamp du précédent dans le registry (null si pas de précédent)
            String startTimeStamp = null;
            if (!sales.isEmpty()) {
                ExplorationDataSale previousSale = sales.get(sales.size() - 1);
                startTimeStamp = previousSale.getEndTimeStamp();
            }
            
            // Créer une nouvelle vente en cours
            currentSale = ExplorationDataSale.builder()
                    .startTimeStamp(startTimeStamp)
                    .endTimeStamp(timestamp)
                    .systemsVisited(new java.util.ArrayList<>())
                    .baseValue(0)
                    .bonus(0)
                    .totalEarnings(0)
                    .build();
            sales.add(currentSale);
        }
        
        // Ajouter les systèmes découverts
        currentSale.getSystemsVisited().addAll(discoveredSystems);
        
        // Accumuler les valeurs
        currentSale.setBaseValue(currentSale.getBaseValue() + baseValue);
        currentSale.setBonus(currentSale.getBonus() + bonus);
        currentSale.setTotalEarnings(currentSale.getTotalEarnings() + totalEarnings);
        currentSale.setEndTimeStamp(timestamp);
    }

    /**
     * Finalise la vente en cours et l'ajoute à la liste des ventes.
     * Appelé lors de l'événement Undocked.
     */
    public void finalizeCurrentSale(String timestamp) {
        if (currentSale != null) {
            currentSale.setEndTimeStamp(timestamp);
            currentSale = null;
            //addToOnHold(SystemVisitedRegistry.getInstance().getSystem(CommanderStatus.getInstance().getCurrentStarSystem()));

        }
          }

    /**
     * Récupère toutes les ventes.
     */
    public List<ExplorationDataSale> getAllSales() {
        return sales;
    }


    public void clearOnHold() {
        explorationDataOnHold = null;
    }

    /**
     * Après une vente au marchand : le journal ne liste que les systèmes dont les données ont été
     * vendues, pas forcément le système d'amarrage (hub, bulle, etc.). On garde le système actuel
     * dans le groupe {@code currentSale} pour qu'il reste visible dans l'historique d'exploration.
     */
    public void appendCommanderStarSystemToCurrentSaleIfMissing(String journalTimestamp) {
        if (currentSale == null || currentSale.getSystemsVisited() == null) {
            return;
        }
        String name = CommanderStatus.getInstance().getCurrentStarSystem();
        if (name == null || name.isBlank()) {
            return;
        }
        boolean alreadyListed = currentSale.getSystemsVisited().stream()
                .anyMatch(s -> s != null && name.equals(s.getSystemName()));
        if (alreadyListed) {
            return;
        }
        SystemVisited fromRegistry = SystemVisitedRegistry.getInstance().getSystem(name);
        if (fromRegistry != null) {
            currentSale.getSystemsVisited().add(fromRegistry);
            return;
        }
        String ts = journalTimestamp != null && !journalTimestamp.isBlank() ? journalTimestamp : "";
        SystemVisited placeholder = SystemVisited.builder()
                .systemName(name)
                .firstVisitedTime(ts)
                .lastVisitedTime(ts)
                .sold(false)
                .build();
        currentSale.getSystemsVisited().add(placeholder);
    }

    /**
     * Remplace dans les ventes / on hold les entrées {@link SystemVisited} pour ce nom par l’instance
     * courante du {@link SystemVisitedRegistry} (après {@code addOrUpdateSystem}, merge Spansh, sync corps, etc.).
     */
    public void resyncSystemVisitedWithRegistry(String systemName) {
        if (systemName == null || systemName.isBlank()) {
            return;
        }
        String nameNorm = systemName.trim();
        SystemVisited fromReg = lookupRegistrySystem(nameNorm);
        if (fromReg == null) {
            return;
        }
        if (explorationDataOnHold != null && explorationDataOnHold.getSystemsVisitedMap() != null) {
            ExplorationDataOnHold.ensureSystemsVisitedMapIsConcurrent(explorationDataOnHold);
            var map = explorationDataOnHold.getSystemsVisitedMap();
            String keyFound = null;
            for (String k : map.keySet()) {
                if (k != null && k.equalsIgnoreCase(nameNorm)) {
                    keyFound = k;
                    break;
                }
            }
            if (keyFound != null) {
                map.put(keyFound, fromReg);
            }
        }
        for (ExplorationDataSale sale : sales) {
            if (sale == null || sale.getSystemsVisited() == null) {
                continue;
            }
            replaceSystemInSaleList(sale.getSystemsVisited(), nameNorm, fromReg);
        }
    }

    private static SystemVisited lookupRegistrySystem(String nameNorm) {
        SystemVisitedRegistry reg = SystemVisitedRegistry.getInstance();
        SystemVisited exact = reg.getSystem(nameNorm);
        if (exact != null) {
            return exact;
        }
        for (var e : reg.getSystems().entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(nameNorm)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static void replaceSystemInSaleList(List<SystemVisited> list, String nameNorm, SystemVisited fromReg) {
        for (int i = 0; i < list.size(); i++) {
            SystemVisited s = list.get(i);
            if (s != null && s.getSystemName() != null && s.getSystemName().trim().equalsIgnoreCase(nameNorm)) {
                list.set(i, fromReg);
            }
        }
    }

    /**
     * Après chargement persistance / batch : réaligne toutes les entrées d’historique sur le registre visité.
     */
    public void resyncAllExplorationSalesFromSystemRegistry() {
        Set<String> names = new LinkedHashSet<>();
        if (explorationDataOnHold != null && explorationDataOnHold.getSystemsVisitedMap() != null) {
            ExplorationDataOnHold.ensureSystemsVisitedMapIsConcurrent(explorationDataOnHold);
            for (var e : explorationDataOnHold.getSystemsVisitedMap().entrySet()) {
                if (e.getKey() != null && !e.getKey().isBlank()) {
                    names.add(e.getKey().trim());
                }
                if (e.getValue() != null && e.getValue().getSystemName() != null && !e.getValue().getSystemName().isBlank()) {
                    names.add(e.getValue().getSystemName().trim());
                }
            }
        }
        for (ExplorationDataSale sale : sales) {
            if (sale == null || sale.getSystemsVisited() == null) {
                continue;
            }
            for (SystemVisited sv : sale.getSystemsVisited()) {
                if (sv != null && sv.getSystemName() != null && !sv.getSystemName().isBlank()) {
                    names.add(sv.getSystemName().trim());
                }
            }
        }
        for (String n : names) {
            resyncSystemVisitedWithRegistry(n);
        }
    }

    public void clearAll(){
        clearOnHold();
        getAllSales().clear();
        currentAnalysisBodyName = null;
        currentAnalysisSpeciesId = null;
        filteredBodyID = null;
    }
    /**
     * Retourne le nombre de ventes dans le registry.
     */
    public int size() {
        return sales.size();
    }

}

