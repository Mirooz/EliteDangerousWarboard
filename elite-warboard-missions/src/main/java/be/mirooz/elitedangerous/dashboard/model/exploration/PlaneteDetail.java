package be.mirooz.elitedangerous.dashboard.model.exploration;

import be.mirooz.elitedangerous.biologic.*;
import be.mirooz.elitedangerous.service.BioSpeciesService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static lombok.Builder.*;

/**
 * Modèle représentant les détails d'une planète scannée dans Elite Dangerous.
 * Stocke toutes les informations utiles extraites de l'événement Scan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaneteDetail {
    private String timestamp;
    // Informations de base
    private String bodyName;
    private String starSystem;
    private long systemAddress;
    private int bodyID;
    private BodyType planetClass;

    // Propriétés physiques
    private Double temperature; // En Kelvin
    private Double pressureAtm; // Pression en atmosphères (convertie depuis Pascal)
    private Double gravityG; // Gravité en G (convertie depuis m/s²)
    private boolean landable;

    // Atmosphère et volcanisme
    private AtmosphereType atmosphere;
    private VolcanismType volcanism;

    // Matériaux de surface (nom -> pourcentage)
    private Map<String, Double> materials;

    // Statut de découverte
    private boolean wasMapped;
    private boolean wasFootfalled;
    private boolean wasDiscovered;
    @Default
    private List<Scan> bioSpecies = new ArrayList<>();

    /**
     * Convertit la pression de Pascal vers atmosphères.
     * 1 atm = 101325 Pascal
     */
    public static double pascalToAtm(double pascal) {
        return pascal / 101325.0;
    }

    /**
     * Convertit la gravité de m/s² vers G.
     * 1 G = 9.80665 m/s²
     */
    public static double ms2ToG(double ms2) {
        return ms2 / 9.80665;
    }

    /**
     * Calcule les informations biologiques pour cette planète.
     * Cette méthode est appelée lorsqu'un signal biologique est détecté
     * et que la planète est disponible dans le registre.
     */
    public void calculBioFirstScan(Integer count) {
        // Vérification des espèces biologiques possibles sur cette planète
        try {
            List<BioSpecies> allSpecies = BioSpeciesService.getInstance().getSpecies();
            List<Map.Entry<BioSpecies, Double>> matchingSpecies = allSpecies.stream()
                    .filter(species -> BioSpeciesMatcher.matches(this, species))
                    .map(species -> Map.entry(species, BioSpeciesMatcher.probability(this, species)))
                    .sorted(Comparator.comparingDouble(Map.Entry<BioSpecies, Double>::getValue).reversed())
                    .toList();


            if (!matchingSpecies.isEmpty()) {
                matchingSpecies = matchingSpecies
                        .stream().filter(
                                species -> (species.getKey().getVariantMethod().equals(VariantMethods.SURFACE_MATERIALS)
                                        && this.getMaterials().containsKey(species.getKey().getColorConditionName()))
                                        || species.getKey().getColorConditionName().equals("K")
                        )
                        .toList();
                double probaCount = matchingSpecies.stream()
                        .mapToDouble(Map.Entry::getValue)
                        .sum();

                System.out.printf("   🌱 Espèces biologiques possibles (%d):%n", matchingSpecies.size());
                System.out.println(probaCount);
                matchingSpecies.forEach(species ->
                        {
                            System.out.printf("      - %s - %d - proba : %f %% %n", species.getKey().getFullName(), species.getKey().getBaseValue(), species.getValue());

                        }
                );
                List<SpeciesProbability> probabilities = matchingSpecies.stream()
                        .map(e -> new SpeciesProbability(e.getKey(), (100.0 / probaCount) * e.getValue()))
                        .toList();
                Scan scan = new Scan(1, probabilities);
                this.getBioSpecies().add(scan);
            }
        } catch (URISyntaxException | IOException e) {
            System.err.println("❌ Erreur lors du chargement des espèces biologiques: " + e.getMessage());
        }

    }
}

