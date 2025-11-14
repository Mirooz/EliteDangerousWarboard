package be.mirooz.elitedangerous.dashboard.model.exploration;

import be.mirooz.elitedangerous.biologic.*;
import be.mirooz.elitedangerous.dashboard.model.registries.PlaneteRegistry;
import be.mirooz.elitedangerous.service.BioSpeciesService;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Modèle représentant les détails d'une planète scannée dans Elite Dangerous.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class PlaneteDetail extends AbstractCelesteBody {

    // Propriétés spécifiques à une planète
    private BodyType planetClass;

    // Propriétés physiques
    private Double temperature;     // Kelvin
    private Double pressureAtm;     // Atmosphères
    private Double gravityG;        // En G
    private boolean landable;

    // Atmosphère & volcanisme
    private AtmosphereType atmosphere;
    private VolcanismType volcanism;

    // Matériaux de surface
    private Map<String, Double> materials;

    // Liste des scans biologiques (FSS/SAASignals)
    @Builder.Default
    private List<Scan> bioSpecies = new ArrayList<>();

    // Liste des espèces confirmées (ScanOrganic)
    @Builder.Default
    private List<BioSpecies> confirmedSpecies = new ArrayList<>();


    /** Conversion Pascal → Atmosphères */
    public static double pascalToAtm(double pascal) {
        return pascal / 101325.0;
    }

    /** Conversion m/s² → G */
    public static double ms2ToG(double ms2) {
        return ms2 / 9.80665;
    }

    /**
     * Calcul niveau 1 : signaux FSS (nombre de bio possibles)
     */
    public void calculBioFirstScan(Integer count) {
        calculBioScan(count, 1, null);
    }

    /**
     * Calcul complet des espèces biologiques possibles pour cette planète.
     */
    public void calculBioScan(Integer count, int level, List<String> genuses) {
        try {
            List<BioSpecies> allSpecies = BioSpeciesService.getInstance().getSpecies();

            List<Map.Entry<BioSpecies, Double>> matchingSpecies =
                    allSpecies.stream()
                            .filter(species -> BioSpeciesMatcher.matches(this, species))
                            .map(species -> Map.entry(species, BioSpeciesMatcher.probability(this, species)))
                            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                            .toList();

            if (matchingSpecies.isEmpty()) return;

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
                        // --- CAS 2 : RADIANT STAR ---
                        boolean radiantMatch =
                                method == VariantMethods.RADIANT_STAR
                                        && parents != null
                                        && parents.stream().anyMatch(parent ->
                                        "Star".equalsIgnoreCase(parent.getType())
                                                &&
                                                PlaneteRegistry.getInstance()
                                                        .getAllPlanetes().stream()
                                                        .filter(body -> body instanceof StarDetail)
                                                        .map(body -> (StarDetail) body)
                                                        .anyMatch(star ->
                                                                star.getBodyID() == parent.getBodyID()
                                                                        && star.getStarType().equalsIgnoreCase(species.getColorConditionName())
                                                        )
                                );

                        return surfaceMatch || radiantMatch;
                    })
                    .toList();


            // Niveau 2 : filtre par genuses
            if (level == 2 && genuses != null && !genuses.isEmpty()) {
                matchingSpecies = matchingSpecies.stream()
                        .filter(species ->
                                genuses.stream().anyMatch(
                                        genus -> genus.split("_")[2].equals(species.getKey().getFdevname().split("_")[2])
                                )
                        )
                        .toList();
            }
//
//            double totalOccurrences = matchingSpecies.stream()
//                    .mapToDouble(Map.Entry::getValue)
//                    .sum();
//
//            List<SpeciesProbability> probabilities = matchingSpecies.stream()
//                    .map(e -> {
//                        double f = e.getValue() / totalOccurrences;  // fréquence brute
//                        double adjusted = 1 - Math.pow(1 - f, count); // proba cumulée
//                        return new SpeciesProbability(e.getKey(), adjusted * 100);
//                    })
//                    .collect(Collectors.toCollection(ArrayList::new));
            //
            List<SpeciesProbability> probabilities  =computeProbabilities(matchingSpecies, count);
            System.out.println("Bio pour body :" + this.bodyName + " level " + level + " count " + count + (genuses != null ? " genus " + genuses : ""));
            for (SpeciesProbability probability : probabilities) {
                System.out.println("    " +probability.getBioSpecies().getFullName() + " : " + probability.getProbability());
            }
            System.out.println("    " );
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

        // -- 4) Regrouper par nom (= genus ou name) et garder la plus probable --
        Map<String, SpeciesProbability> bestByName =
                rawList.stream()
                        .collect(Collectors.toMap(
                                sp -> sp.getBioSpecies().getName(), // clé de regroupement
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


    private void applyAccurateProbabilities(List<SpeciesProbability> probabilities, int count) {

        if (probabilities == null || probabilities.isEmpty()) {
            return;
        }

        // -- 0) Filtrer les valeurs trop faibles (< 1%) --
        List<SpeciesProbability> filtered = probabilities.stream()
                .filter(p -> p.getProbability() >= 1.0)
                .collect(Collectors.toCollection(ArrayList::new));

        if (filtered.isEmpty()) {
            probabilities.clear(); // plus rien
            return;
        }

        // -- 1) Grouper par nom et garder la plus probable --
        Map<String, SpeciesProbability> bestByName =
                filtered.stream()
                        .collect(Collectors.toMap(
                                p -> p.getBioSpecies().getName(), // ← ajuster : genus si nécessaire
                                p -> p,
                                (p1, p2) -> p1.getProbability() >= p2.getProbability() ? p1 : p2
                        ));

        // -- 2) Normaliser les probabilités --
        List<SpeciesProbability> normalized = new ArrayList<>(bestByName.values());

        double totalProba = normalized.stream()
                .mapToDouble(SpeciesProbability::getProbability)
                .sum();

        if (totalProba > 0) {
            normalized.forEach(sp ->
                    sp.setProbability((100.0 / totalProba) * sp.getProbability())
            );
        }

        // -- 3) Si le nombre d’espèces ≤ count → 100% --
        if (normalized.size() <= count) {
            normalized.forEach(sp -> sp.setProbability(100.0));
        }

        // -- 4) Trier par proba décroissante --
        normalized.sort(Comparator.comparingDouble(SpeciesProbability::getProbability).reversed());

        // -- 5) Réinjecter dans la liste d'origine --
        probabilities.clear();
        probabilities.addAll(normalized);
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

            BioSpecies existing = confirmedSpecies.stream()
                    .filter(s -> s.getId().equalsIgnoreCase(matchingSpecies.getId()))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                existing.addScanType(scanTypeBio);
                return;
            }

            BioSpecies confirmed = BioSpecies.builder()
                    .name(matchingSpecies.getName())
                    .specieName(matchingSpecies.getSpecieName())
                    .color(matchingSpecies.getColor())
                    .count(matchingSpecies.getCount())
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

            confirmed.addScanType(ScanTypeBio.LOG);
            confirmedSpecies.add(confirmed);

        } catch (Exception e) {
            System.err.println("❌ Erreur addConfirmedSpecies: " + e.getMessage());
        }
    }

    /** Retrouve l'espèce correspondante dans la bibliothèque bio */
    private BioSpecies findMatchingSpecies(ScanOrganicData scanOrganicData) {
        try {
            return BioSpeciesService.getInstance()
                    .getSpecies()
                    .stream()
                    .filter(species -> species.getFdevname()
                            .equalsIgnoreCase(scanOrganicData.getVariant()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
