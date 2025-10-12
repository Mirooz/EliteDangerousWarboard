package be.mirooz.elitedangerous.lib.inara.service;

import be.mirooz.elitedangerous.lib.inara.client.InaraClient;
import be.mirooz.elitedangerous.lib.inara.model.Commodity;
import be.mirooz.elitedangerous.lib.inara.model.CoreMineralRegistry;
import be.mirooz.elitedangerous.lib.inara.model.minerals.CoreMineral;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service pour récupérer les données de marché de tous les minéraux de core mining
 */
public class InaraService {
    
    private final InaraClient client;
    private final ExecutorService executorService;
    
    public InaraService() {
        this.client = new InaraClient();
        this.executorService = Executors.newFixedThreadPool(10); // Pool de 10 threads
    }
    
    /**
     * Récupère le marché de tous les minéraux de core mining de manière asynchrone
     * 
     * @param sourceSystem Système source pour la recherche (ex: "belanit")
     * @param distance Distance maximale du système en années-lumière (ex: 100)
     * @param supplyDemand Offre/demande minimale (ex: 500)
     * @param largePad Utiliser uniquement les grands pads d'atterrissage (ex: false)
     * @return Liste complète de toutes les commodités trouvées
     * @throws IOException en cas d'erreur de connexion
     */
    public List<Commodity> fetchAllMinerMarkets(String sourceSystem, int distance, 
                                               int supplyDemand, boolean largePad) throws IOException {
        
        // Récupérer tous les minéraux de core mining
        List<CoreMineral> allMinerals = new ArrayList<>(CoreMineralRegistry.getAllMinerals().values());
        
        // Créer des tâches asynchrones pour chaque minéral
        List<CompletableFuture<List<Commodity>>> futures = new ArrayList<>();
        
        for (CoreMineral mineral : allMinerals) {
            CompletableFuture<List<Commodity>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("🔍 Recherche du marché pour: " + mineral.getInaraName());
                    return client.fetchMinerMarket(mineral, sourceSystem, distance, supplyDemand, largePad);
                } catch (IOException e) {
                    System.err.println("❌ Erreur lors de la recherche pour " + mineral.getInaraName() + ": " + e.getMessage());
                    return new ArrayList<>(); // Retourner une liste vide en cas d'erreur
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // Attendre que toutes les tâches se terminent et combiner les résultats
        List<Commodity> allCommodities = new ArrayList<>();
        
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            // Attendre la fin de toutes les tâches
            allFutures.get();
            
            // Collecter tous les résultats
            for (CompletableFuture<List<Commodity>> future : futures) {
                List<Commodity> commodities = future.get();
                allCommodities.addAll(commodities);
            }
            
            System.out.println("✅ Recherche terminée. Total de " + allCommodities.size() + " commodités trouvées.");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'exécution des tâches asynchrones: " + e.getMessage());
            throw new IOException("Erreur lors de la récupération des marchés", e);
        }
        
        return allCommodities;
    }
    
    /**
     * Méthode simplifiée avec des paramètres par défaut
     * 
     * @param sourceSystem Système source pour la recherche
     * @return Liste complète de toutes les commodités trouvées
     * @throws IOException en cas d'erreur de connexion
     */
    public List<Commodity> fetchAllMinerMarkets(String sourceSystem) throws IOException {
        return fetchAllMinerMarkets(sourceSystem, 100, 500, false);
    }
    
    /**
     * Ferme le service et libère les ressources
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
