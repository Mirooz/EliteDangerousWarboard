package be.mirooz.elitedangerous.dashboard.model.exploration;

import be.mirooz.elitedangerous.biologic.*;

import java.util.List;
import java.util.Map;

/**
 * Utilitaire pour vérifier si une planète correspond aux conditions requises
 * pour qu'une espèce biologique puisse y être trouvée.
 */
public class BioSpeciesMatcher {

    public static double probability(PlaneteDetail planete, BioSpecies species){
        try {
            BioSpeciesFactory.HistogramData histogramData = species.getHistogramData();
            matchesBodyType(planete.getPlanetClass(), histogramData.bodyTypes);
            Double bodyValue = histogramData.bodyTypes.get(planete.getPlanetClass());
            Double atmosValue = histogramData.atmosTypes.get(planete.getAtmosphere());
            Double volcanismValue = histogramData.volcanicBodyTypes.get(new BioSpeciesFactory.VolcanicBodyType(planete.getPlanetClass(), planete.getVolcanism()));

            Double temperatureValue = histogramData.temperature.stream().filter(bin ->
                            bin.min != null && bin.max != null &&
                                    planete.getTemperature() >= bin.min && planete.getTemperature() <= bin.max)
                    .findFirst().map(bin -> bin.value).orElse(0.0);
            Double gravityValue = histogramData.gravity.stream().filter(bin ->
                            bin.min != null && bin.max != null &&
                                    planete.getGravityG() >= bin.min && planete.getGravityG() <= bin.max)
                    .findFirst().map(bin -> bin.value).orElse(0.0);
            Double pressureValue = histogramData.pressure.stream().filter(bin ->
                            bin.min != null && bin.max != null &&
                                    planete.getPressureAtm() >= bin.min && planete.getPressureAtm() <= bin.max)
                    .findFirst().map(bin -> bin.value).orElse(0.0);
            return Math.min(bodyValue, Math.min(atmosValue, Math.min(volcanismValue, Math.min(temperatureValue, Math.min(gravityValue, pressureValue)))));
        }
        catch (Exception e){
            System.out.println("Error in probability");
            return 0.0;
        }
        }
    /**
     * Vérifie si une planète correspond aux conditions d'une espèce biologique.
     * 
     * @param planete La planète à vérifier
     * @param species L'espèce biologique à vérifier
     * @return true si la planète correspond aux conditions, false sinon
     */
    public static boolean matches(PlaneteDetail planete, BioSpecies species) {

        if (planete == null || species == null || species.getHistogramData() == null) {
            return false;
        }

        BioSpeciesFactory.HistogramData histogramData = species.getHistogramData();

        // 1. Vérifier le type de corps (BodyType)
        if (!matchesBodyType(planete.getPlanetClass(), histogramData.bodyTypes)) {
            return false;
        }

        // 2. Vérifier l'atmosphère
        if (!matchesAtmosphere(planete.getAtmosphere(), histogramData.atmosTypes)) {
            return false;
        }

        // 3. Vérifier le volcanisme
        if (!matchesVolcanism(planete.getPlanetClass(), planete.getVolcanism(), histogramData.volcanicBodyTypes)) {
            return false;
        }

        // 4. Vérifier la température
        if (planete.getTemperature() != null && !matchesTemperature(planete.getTemperature(), histogramData.temperature)) {
            return false;
        }

        // 5. Vérifier la gravité
        if (planete.getGravityG() != null && !matchesGravity(planete.getGravityG(), histogramData.gravity)) {
            return false;
        }

        // 6. Vérifier la pression
        if (planete.getPressureAtm() != null && !matchesPressure(planete.getPressureAtm(), histogramData.pressure)) {
            return false;
        }
        if (species.getVariantMethod().equals(VariantMethods.SURFACE_MATERIALS)){
            return planete.getMaterials().containsKey(species.getColorConditionName().toLowerCase());
        }
        else{
            //CAS STAR
        }



        return true;
    }

    /**
     * Vérifie si le type de corps correspond.
     */
    private static boolean matchesBodyType(BodyType planetClass, Map<BodyType, Double> allowedBodyTypes) {
        if (planetClass == null) {
            return false;
        }
        if (allowedBodyTypes == null || allowedBodyTypes.isEmpty()) {
            return true; // Pas de restriction
        }
        return allowedBodyTypes.containsKey(planetClass);
    }

    private static double probabilityBodyType(BodyType planetClass, Map<BodyType, Double> allowedBodyTypes){
        return allowedBodyTypes.get(planetClass);
    }

    /**
     * Vérifie si l'atmosphère correspond.
     */
    private static boolean matchesAtmosphere(AtmosphereType atmosphere, Map<AtmosphereType, Double> allowedAtmosTypes) {
        if (atmosphere == null) {
            // Si pas d'atmosphère, vérifier si "No atmosphere" est autorisé
            if (allowedAtmosTypes == null || allowedAtmosTypes.isEmpty()) {
                return true;
            }
            return allowedAtmosTypes.containsKey(AtmosphereType.NO_ATMOSPHERE);
        }
        if (allowedAtmosTypes == null || allowedAtmosTypes.isEmpty()) {
            return true; // Pas de restriction
        }
        return allowedAtmosTypes.containsKey(atmosphere);
    }

    /**
     * Vérifie si le volcanisme correspond.
     */
    private static boolean matchesVolcanism(BodyType planetClass, VolcanismType volcanism, 
                                           Map<BioSpeciesFactory.VolcanicBodyType, Double> volcanicBodyTypes) {
        if (volcanicBodyTypes == null || volcanicBodyTypes.isEmpty()) {
            return true; // Pas de restriction
        }

        if (planetClass == null || volcanism == null) {
            // Si pas de volcanisme, vérifier si "No volcanism" est autorisé pour ce type de corps
            if (volcanism == null && planetClass != null) {
                BioSpeciesFactory.VolcanicBodyType key = new BioSpeciesFactory.VolcanicBodyType(
                    planetClass, VolcanismType.NO_VOLCANISM);
                return volcanicBodyTypes.containsKey(key);
            }
            return false;
        }

        // Vérifier la combinaison exacte bodyType + volcanismType
        BioSpeciesFactory.VolcanicBodyType key = new BioSpeciesFactory.VolcanicBodyType(planetClass, volcanism);
        return volcanicBodyTypes.containsKey(key);
    }

    /**
     * Vérifie si la température est dans une des plages autorisées.
     */
    private static boolean matchesTemperature(double temperature, List<BioSpeciesFactory.Bin> temperatureBins) {
        if (temperatureBins == null || temperatureBins.isEmpty()) {
            return false;
        }

        return temperatureBins.stream().anyMatch(bin -> 
            bin.min != null && bin.max != null && 
            temperature >= bin.min && temperature <= bin.max
        );
    }

    /**
     * Vérifie si la gravité est dans une des plages autorisées.
     */
    private static boolean matchesGravity(double gravityG, List<BioSpeciesFactory.Bin> gravityBins) {
        if (gravityBins == null || gravityBins.isEmpty()) {
            return false; // Pas de restriction
        }

        return gravityBins.stream().anyMatch(bin -> 
            bin.min != null && bin.max != null && 
            gravityG >= bin.min && gravityG <= bin.max
        );
    }

    /**
     * Vérifie si la pression est dans une des plages autorisées.
     */
    private static boolean matchesPressure(double pressureAtm, List<BioSpeciesFactory.Bin> pressureBins) {
        if (pressureBins == null || pressureBins.isEmpty()) {
            return false; // Pas de restriction
        }

        return pressureBins.stream().anyMatch(bin -> 
            bin.min != null && bin.max != null && 
            pressureAtm >= bin.min && pressureAtm <= bin.max
        );
    }
}




