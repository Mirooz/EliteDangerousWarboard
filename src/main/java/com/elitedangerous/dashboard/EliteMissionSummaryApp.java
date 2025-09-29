package com.elitedangerous.dashboard;

import com.elitedangerous.dashboard.service.JournalService;

/**
 * Application pour afficher le résumé des missions Elite Dangerous
 */
public class EliteMissionSummaryApp {
    
    public static void main(String[] args) {
        JournalService journalService = new JournalService();
        
        System.out.println("=== RÉSUMÉ DES MISSIONS ELITE DANGEROUS ===");
        System.out.println("Lecture des journaux depuis: C:\\Users\\ewen_\\Saved Games\\Frontier Developments\\Elite Dangerous");
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
