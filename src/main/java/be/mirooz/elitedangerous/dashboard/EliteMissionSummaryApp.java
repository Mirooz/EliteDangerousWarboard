package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.service.JournalService;

/**
 * Application pour afficher le résumé des missions Elite Dangerous
 */
public class EliteMissionSummaryApp {
    static String journalFolder = System.getProperty("journal.folder");
    public static void main(String[] args) {
        JournalService journalService = JournalService.getInstance();
        
        System.out.println("=== RÉSUMÉ DES MISSIONS ELITE DANGEROUS ===");
        System.out.println("Lecture des journaux depuis: " + journalFolder);
        System.out.println();
        
        try {
            String summary = journalService.generateMissionSummary();
            System.out.println(summary);
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du résumé: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Fermeture automatique
    }
}
