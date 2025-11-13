package be.mirooz.elitedangerous.dashboard.model.exploration;

import be.mirooz.elitedangerous.biologic.*;
import be.mirooz.elitedangerous.service.BioSpeciesService;
import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Modèle représentant les détails d'une planète scannée dans Elite Dangerous.
 * Stocke toutes les informations utiles extraites de l'événement Scan.
 */
@Data
@Builder
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
    @Builder.Default
    private List<Scan> bioSpecies = new ArrayList<>();
    @Builder.Default
    private List<BioSpecies> confirmedSpecies = new ArrayList<>();

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
     * Calcule les informations biologiques pour cette planète (niveau 1 - FSSBodySignals).
     * Cette méthode est appelée lorsqu'un signal biologique est détecté
     * et que la planète est disponible dans le registre.
     */
    public void calculBioFirstScan(Integer count) {
        calculBioScan(count, 1, null);
    }

    /**
     * Calcule les informations biologiques pour cette planète.
     * Cette méthode est appelée lorsqu'un signal biologique est détecté
     * et que la planète est disponible dans le registre.
     *
     * @param count   Le nombre de signaux biologiques
     * @param level   Le niveau du scan (1 pour FSSBodySignals, 2 pour SAASignalsFound)
     * @param genuses La liste des genuses détectés (null pour level 1)
     */
    public void calculBioScan(Integer count, int level, List<String> genuses) {
        // Vérification des espèces biologiques possibles sur cette planète
        try {
            List<BioSpecies> allSpecies = BioSpeciesService.getInstance().getSpecies();
            List<Map.Entry<BioSpecies, Double>> matchingSpecies = allSpecies.stream()
                    .filter(species -> BioSpeciesMatcher.matches(this, species))
                    .map(species -> Map.entry(species, BioSpeciesMatcher.probability(this, species)))
                    .sorted(Comparator.comparingDouble(Map.Entry<BioSpecies, Double>::getValue).reversed())
                    .toList();

            if (!matchingSpecies.isEmpty()) {
                // Filtrage selon le niveau
                matchingSpecies = matchingSpecies
                        .stream().filter(
                                species -> (species.getKey().getVariantMethod().equals(VariantMethods.SURFACE_MATERIALS)
                                        && this.getMaterials() != null
                                        && this.getMaterials().containsKey(species.getKey().getColorConditionName().toLowerCase()))
                                        || species.getKey().getColorConditionName().equals("K") //TODO
                        )
                        .toList();
                if (level == 2 && genuses != null && !genuses.isEmpty()) {
                    // Niveau 2 : filtre par genuses détectés
                    matchingSpecies = matchingSpecies
                            .stream()
                            .filter(species -> {
                                String speciesName = species.getKey().getName();
                                return genuses.stream()
                                        .anyMatch(genus -> genus.toLowerCase().contains(speciesName.toLowerCase()));

                            })
                            .toList();
                }

                double probaCount = matchingSpecies.stream()
                        .mapToDouble(Map.Entry::getValue)
                        .sum();

                System.out.printf("   🌱 Espèces biologiques possibles (niveau %d, %d espèces):%n", level, matchingSpecies.size());
                System.out.println(probaCount);
                matchingSpecies.forEach(species ->
                        {
                            System.out.printf("      - %s - %d - proba : %f %% %n", species.getKey().getFullName(), species.getKey().getBaseValue(), species.getValue());

                        }
                );
                List<SpeciesProbability> probabilities = matchingSpecies.stream()
                        .map(e -> new SpeciesProbability(e.getKey(), (100.0 / probaCount) * e.getValue()))
                        .toList();
                Scan scan = new Scan(level, probabilities);
                this.getBioSpecies().add(scan);
            }
        } catch (URISyntaxException | IOException e) {
            System.err.println("❌ Erreur lors du chargement des espèces biologiques: " + e.getMessage());
        }

    }

    /**
     * Ajoute ou met à jour une espèce confirmée selon le type de scan.
     *
     * @param scanOrganicData Les données du scan organique
     */
    public void addConfirmedSpecies(ScanOrganicData scanOrganicData) {
        try {
            ScanTypeBio scanTypeBio = ScanTypeBio.fromString(scanOrganicData.getScanType());
            if (scanTypeBio == null) {
                System.err.println("❌ Type de scan inconnu: " + scanOrganicData.getScanType());
                return;
            }

            // Chercher l'espèce correspondante dans la liste des espèces possibles
            BioSpecies matchingSpecies = findMatchingSpecies(scanOrganicData);

            if (matchingSpecies == null) {
                System.err.println("❌ Espèce non trouvée pour: " + scanOrganicData.getSpeciesLocalised());
                return;
            }
            // Chercher si l'espèce existe déjà dans confirmedSpecies
            BioSpecies existingSpecies = this.confirmedSpecies.stream()
                    .filter(s -> s.getId().equalsIgnoreCase(matchingSpecies.getId()))
                    .findFirst()
                    .orElse(null);

            //Déja présente dans confirmedSpecies
            if (existingSpecies != null) {
                existingSpecies.addScanType(scanTypeBio);
                System.out.printf("   📝 %s ajouté pour: %s%n", scanTypeBio, scanOrganicData.getSpeciesLocalised());
            } else {
                // Créer une copie de l'espèce avec les informations du scan
                BioSpecies confirmedSpecies = BioSpecies.builder()
                        .name(matchingSpecies.getName())
                        .specieName(matchingSpecies.getSpecieName())
                        .color(matchingSpecies.getColor())
                        .count(matchingSpecies.getCount())
                        .fdevname(matchingSpecies.getFdevname())
                        .baseValue(matchingSpecies.getBaseValue())
                        .firstLoggedValue(matchingSpecies.getFirstLoggedValue())
                        .colonyRangeMeters(matchingSpecies.getColonyRangeMeters())
                        .variantMethod(matchingSpecies.getVariantMethod())
                        .colorConditionName(matchingSpecies.getColorConditionName())
                        .id(matchingSpecies.getId())
                        .histogramData(matchingSpecies.getHistogramData())
                        .genus(scanOrganicData.getGenus())
                        .variantLocalised(scanOrganicData.getVariantLocalised())
                        .wasLogged(scanOrganicData.isWasLogged())
                        .collected(false)
                        .build();
                confirmedSpecies.addScanType(ScanTypeBio.LOG);
                this.confirmedSpecies.add(confirmedSpecies);
                System.out.printf("   📋 Nouvelle espèce loggée: %s%n", scanOrganicData.getSpeciesLocalised());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ajout de l'espèce confirmée: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Trouve l'espèce correspondante dans la liste des espèces possibles.
     */
    private BioSpecies findMatchingSpecies(ScanOrganicData scanOrganicData) {
        try {
            List<BioSpecies> allSpecies = BioSpeciesService.getInstance().getSpecies();
            String variant = scanOrganicData.getVariant();
            return allSpecies.stream().filter(
                    species -> {
                      return   species.getFdevname().equalsIgnoreCase(variant);
                    })
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la recherche de l'espèce: " + e.getMessage());
            return null;
        }
    }
}

