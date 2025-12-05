package be.mirooz.elitedangerous.dashboard.model.exploration;

import be.mirooz.elitedangerous.biologic.*;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.OrganicDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import be.mirooz.elitedangerous.dashboard.service.ExplorationService;
import be.mirooz.elitedangerous.service.BioSpeciesService;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Modèle représentant les détails d'une planète scannée dans Elite Dangerous.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class PlaneteDetail extends ACelesteBody {

    private final ExplorationService explorationService = ExplorationService.getInstance();
    // Propriétés spécifiques à une planète
    private BodyType planetClass;
    private Double massEM;
    // Propriétés physiques
    private Double temperature;     // Kelvin
    private Double pressureAtm;     // Atmosphères
    private Double gravityG;        // En G
    private boolean landable;

    private double radius;
    private boolean terraformable;
    // Atmosphère & volcanisme
    private AtmosphereType atmosphere;
    private VolcanismType volcanism;
    // Matériaux de surface
    private Map<String, Double> materials;

    // Liste des scans biologiques (FSS/SAASignals)
    @Builder.Default
    private List<Scan> bioSpecies = new ArrayList<>();
    private Integer numSpeciesDetected;

    // Liste des espèces confirmées (ScanOrganic)
    @Builder.Default
    private List<BioSpecies> confirmedSpecies = new ArrayList<>();


    /**
     * Conversion Pascal → Atmosphères
     */
    public static double pascalToAtm(double pascal) {
        return pascal / 101325.0;
    }

    /**
     * Conversion m/s² → G
     */
    public static double ms2ToG(double ms2) {
        return ms2 / 9.80665;
    }

    /**
     * Calcul complet des espèces biologiques possibles pour cette planète.
     */
    public void calculBioScan(Integer count, int level, List<String> genuses) {
        this.numSpeciesDetected = count;
        if (this.bioSpecies != null && !this.bioSpecies.isEmpty()) {
            for (Scan scan : this.bioSpecies) {
                //Scan level déja fait
                if (scan.getScanNumber() == level) {
                    return;
                }
            }
        }
        try {
            List<BioSpecies> allSpecies = BioSpeciesService.getInstance().getSpecies();

            List<Map.Entry<BioSpecies, Double>> matchingSpecies =
                    allSpecies.stream()
                            .filter(species -> BioSpeciesMatcher.matches(this, species))
                            .map(species -> Map.entry(species, BioSpeciesMatcher.probability(this, species)))
                            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                            .toList();

            if (matchingSpecies.isEmpty()) {
                SpeciesProbability brainTreeProbability = new SpeciesProbability(BioSpecies.brainTree(), 100.0);
                this.bioSpecies.add(new Scan(level, new ArrayList<>(List.of(brainTreeProbability))));
                return;
            }

            matchingSpecies = matchingSpecies.stream()
                    .filter(entry -> {
                        BioSpecies species = entry.getKey();
                        VariantMethods method = species.getVariantMethod();
                        String colorCond = species.getColorConditionName().toLowerCase();
                        // --- CAS 1 : SURFACE MATERIALS ---
                        boolean surfaceMatch =
                                method == VariantMethods.SURFACE_MATERIALS
                                        && this.materials != null
                                        && this.materials.containsKey(colorCond);

                        // CAS 2 : RADIANT STAR
                        boolean radiantMatch = false;

                        if (method == VariantMethods.RADIANT_STAR && parents != null) {

                            // Récupère toutes les étoiles du système
                            List<StarDetail> allStars = PlaneteRegistry.getInstance()
                                    .getAllPlanetes().stream()
                                    .filter(body -> body instanceof StarDetail)
                                    .map(body -> (StarDetail) body)
                                    .toList();

                            // Vérifie si un parent est une étoile
                            Optional<ParentBody> parentStar = parents.stream()
                                    .filter(p -> "Star".equalsIgnoreCase(p.getType()))
                                    .findFirst();

                            StarDetail starToUse;

                            if (parentStar.isPresent()) {
                                // On utilise l'étoile correspondante au parent
                                int parentStarId = parentStar.get().getBodyID();
                                starToUse = allStars.stream()
                                        .filter(star -> star.getBodyID() == parentStarId)
                                        .findFirst()
                                        .orElse(null);
                            } else {
                                // Aucun parent Star → on prend la première étoile du système
                                starToUse = allStars.stream().findFirst().orElse(null);
                            }

                            // Si on a une étoile (par parent ou fallback)
                            if (starToUse != null) {
                                radiantMatch = starToUse.getStarTypeString()
                                        .equalsIgnoreCase(species.getColorConditionName());
                            }
                        }

                        return surfaceMatch || radiantMatch;

                    })
                    .toList();


            // Niveau 2 : filtre par genuses
            if (level == 2) {

                if (genuses != null && !genuses.isEmpty()) {
                    matchingSpecies = matchingSpecies.stream()
                            .filter(species ->
                                    genuses.stream().anyMatch(
                                            genus -> genus.split("_")[2].equals(species.getKey().getFdevname().split("_")[2])
                                    )
                            )
                            .toList();
                }
                else{
                    SpeciesProbability brainTreeProbability = new SpeciesProbability(BioSpecies.brainTree(), 100.0);
                    this.bioSpecies.add(new Scan(level, new ArrayList<>(List.of(brainTreeProbability))));
                    return;
                }
            }

            List<SpeciesProbability> probabilities = computeProbabilities(matchingSpecies, count);
            System.out.println("Bio pour body :" + this.bodyName + " level " + level + " count " + count + (genuses != null ? " genus " + genuses : ""));
            for (SpeciesProbability probability : probabilities) {
                System.out.println("    " + probability.getBioSpecies().getFullName() + " : " + probability.getProbability());
            }
            System.out.println("    ");
            this.bioSpecies.add(new Scan(level, probabilities));

        } catch (Exception e) {
            System.err.println("❌ Erreur calculBioScan: " + e.getMessage());
        }
    }

    private List<SpeciesProbability> computeProbabilities(
            List<Map.Entry<BioSpecies, Double>> matchingSpecies,
            int count
    ) {

        if (matchingSpecies == null || matchingSpecies.isEmpty()) {
            return List.of();
        }

        // -- 1) Total occurrences brutes --
        double totalOccurrences = matchingSpecies.stream()
                .mapToDouble(Map.Entry::getValue)
                .sum();

        if (totalOccurrences <= 0) {
            return List.of();
        }

        // -- 2) Calcul des probabilités cumulées (1 - (1 - f)^count) --
        List<SpeciesProbability> rawList = matchingSpecies.stream()
                .map(e -> {
                    double f = e.getValue() / totalOccurrences;
                    double p = 1 - Math.pow(1 - f, count);
                    return new SpeciesProbability(e.getKey(), p * 100);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // -- 3) Filtrer les espèces < 1% --
        rawList.removeIf(sp -> sp.getProbability() < 1.0);
        if (rawList.isEmpty()) {
            return List.of();
        }

        // -- 4) Regrouper par nom (= genus ou name) et par genre et garder la plus probable --
        Map<String, SpeciesProbability> bestByName =
                rawList.stream()
                        .collect(Collectors.toMap(
                                sp -> sp.getBioSpecies().getName() + " " + sp.getBioSpecies().getSpecieName(), // clé de regroupement
                                sp -> sp,
                                (p1, p2) -> p1.getProbability() >= p2.getProbability() ? p1 : p2
                        ));

        List<SpeciesProbability> finalList = new ArrayList<>(bestByName.values());

        // -- 6) Trier par probabilité décroissante --
        finalList.sort(Comparator.comparingDouble(SpeciesProbability::getProbability).reversed());
        // -- 5) SI count == nb d’espèces après regroupement → TOUTES = 100% --
        if (finalList.size() <= count) {
            finalList.forEach(sp -> sp.setProbability(100.0));
        }


        return finalList;
    }


    /**
     * Ajoute ou met à jour une espèce confirmée suite à un ScanOrganic.
     */
    public void addConfirmedSpecies(ScanOrganicData scanOrganicData) {
        try {
            ScanTypeBio scanTypeBio = ScanTypeBio.fromString(scanOrganicData.getScanType());
            if (scanTypeBio == null) return;

            BioSpecies matchingSpecies = findMatchingSpecies(scanOrganicData);
            if (matchingSpecies == null) return;

            BioSpecies specie = confirmedSpecies.stream()
                    .filter(s -> s.getId().equalsIgnoreCase(matchingSpecies.getId()))
                    .findFirst()
                    .orElseGet(() -> createNewSpecies(matchingSpecies, scanOrganicData));


            // Actions selon le type
            handleScanTypeActions(scanTypeBio, specie, matchingSpecies);
            // Ajoute le scan type
            specie.addScanType(scanTypeBio);

        } catch (Exception e) {
            System.err.println("❌ Erreur addConfirmedSpecies: " + e.getMessage());
        }
    }

    /**
     * Retrouve l'espèce correspondante dans la bibliothèque bio
     */
    private BioSpecies findMatchingSpecies(ScanOrganicData scanOrganicData) {
        try {
            return BioSpeciesService.getInstance()
                    .getSpecies()
                    .stream()
                    .filter(species -> species.getFdevname()
                            .equalsIgnoreCase(scanOrganicData.getVariant()))
                    .findFirst()
                    .orElse(BioSpecies.brainTree());
        } catch (Exception e) {
            return null;
        }
    }

    private BioSpecies createNewSpecies(BioSpecies base, ScanOrganicData scanData) {
        BioSpecies newSpecie = BioSpecies.builder()
                .name(base.getName())
                .specieName(base.getSpecieName())
                .color(base.getColor())
                .count(base.getCount())
                .baseValue(base.getBaseValue())
                .bonusValue(base.getBonusValue())
                .colonyRangeMeters(base.getColonyRangeMeters())
                .variantMethod(base.getVariantMethod())
                .colorConditionName(base.getColorConditionName())
                .id(base.getId())
                .histogramData(base.getHistogramData())
                .genus(scanData.getGenus())
                .variantLocalised(scanData.getVariantLocalised())
                .wasLogged(scanData.isWasLogged())
                .collected(false)
                .build();

        confirmedSpecies.add(newSpecie);
        return newSpecie;
    }

    private void handleScanTypeActions(ScanTypeBio scanTypeBio, BioSpecies specie, BioSpecies matchingSpecies) {

        switch (scanTypeBio) {
            case ANALYSE -> {
                OrganicDataSaleRegistry.getInstance()
                        .addAnalyzedOrganicData(specie, this.isWasFootfalled());
            }

            case SAMPLE, LOG -> {
                explorationService.setCurrentBiologicalAnalysis(this, matchingSpecies);
            }

            default -> {
                // Rien à faire pour les autres types
            }
        }
    }

    public void updateFrom(PlaneteDetail src) {
        this.planetClass = src.planetClass;
        this.temperature = src.temperature;
        this.pressureAtm = src.pressureAtm;
        this.gravityG = src.gravityG;
        this.massEM = src.massEM;
        this.terraformable = src.terraformable;
        this.landable = src.landable;
        this.atmosphere = src.atmosphere;
        this.volcanism = src.volcanism;
        this.materials = src.materials;
        this.rings = src.rings;
        this.jsonNode = src.jsonNode;

        // flags comme avant :
        this.wasMapped |= src.wasMapped;
        this.wasDiscovered |= src.wasDiscovered;
        this.wasFootfalled |= src.wasFootfalled;
    }

    @Override
    public long computeBodyValue() {
        boolean firstDiscover = !wasDiscovered;
        boolean firstMapped = !wasMapped;
        boolean isFleetCarrierSale = false;
        boolean isOdyssey = true;
        boolean isEfficiencyBonus = efficiencyTargetMap;
        double kValue = this.terraformable ? planetClass.getTerraformableK() : planetClass.getBaseK();
        final double q = 0.56591828;

        double mappingMultiplier = 1.0;

        if (mapped) {
            if (firstDiscover && firstMapped) {
                mappingMultiplier = 3.699622554;
            } else if (firstMapped) {
                mappingMultiplier = 8.0956;
            } else {
                mappingMultiplier = 3.3333333333;
            }
        }

        double value = (kValue + kValue * q * Math.pow(massEM, 0.2)) * mappingMultiplier;

        if (mapped) {
            if (isOdyssey) {
                double bonus = value * 0.3;
                value += Math.max(bonus, 555);
            }

            if (isEfficiencyBonus) {
                value *= 1.25;
            }
        }

        value = Math.max(500, value);

        if (firstDiscover) {
            value *= 2.6;
        }

        if (isFleetCarrierSale) {
            value *= 0.75;
        }

        return (long) Math.round(value);
    }
}
