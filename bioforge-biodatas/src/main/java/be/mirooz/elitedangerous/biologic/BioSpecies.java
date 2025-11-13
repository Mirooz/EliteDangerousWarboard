package be.mirooz.elitedangerous.biologic;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
     * BioSpecies implementation that stores histogram data and ID
     */
    @Data
    @Builder
    public class BioSpecies {

        String name;
        String specieName;
        String color;
        String fdevname;
        int count;
        long baseValue;
        long firstLoggedValue;
        double colonyRangeMeters;
        VariantMethods variantMethod;
        String colorConditionName;
        String id;
        BioSpeciesFactory.HistogramData histogramData;

        //Scan
        boolean collected;
        boolean wasLogged;
        String genus;
        String variantLocalised;
        @Builder.Default
        List<ScanTypeBio> scanType = new ArrayList<>();
        int sampleNumber;
        public String getFullName(){
            return name + " " + specieName + " " + color;
        }

        public void addScanType(ScanTypeBio scanTypeBio){
            this.scanType.add(scanTypeBio);
            if (scanTypeBio.equals(ScanTypeBio.SAMPLE) || scanTypeBio.equals(ScanTypeBio.ANALYSE))
                sampleNumber++;
            if (scanTypeBio.equals(ScanTypeBio.ANALYSE))
                collected = true;
        }
    }