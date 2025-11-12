package be.mirooz.elitedangerous.biologic;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
     * BioSpecies implementation that stores histogram data and ID
     */
    @Data
    @AllArgsConstructor
    public class BioSpecies {

        String name;
        String specieName;
        String color;
        int count;
        long baseValue;
        long firstLoggedValue;
        double colonyRangeMeters;
        VariantMethods variantMethod;
        String colorConditionName;
        String id;
        BioSpeciesFactory.HistogramData histogramData;
        public String getFullName(){
            return name + " " + specieName + " " + color;
        }
    }