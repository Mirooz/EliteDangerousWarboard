package be.mirooz.elitedangerous.biologic;

import lombok.Builder;
import lombok.Data;

import javax.swing.text.Position;
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
    long bonusValue;
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
    private List<Position> positions = new ArrayList<>();
    public String getFullName() {
        return name + " " + specieName + " " + color;
    }

    public void addScanType(ScanTypeBio scanTypeBio) {
        this.scanType.add(scanTypeBio);
        if (scanTypeBio.equals(ScanTypeBio.SAMPLE) || scanTypeBio.equals(ScanTypeBio.LOG)) {
            sampleNumber++;
        }
        if (scanTypeBio.equals(ScanTypeBio.ANALYSE)) {
            collected = true;
        }
    }

    public void removeAllSamples() {
        this.scanType.removeIf(type -> type == ScanTypeBio.SAMPLE);
        this.scanType.removeIf(type -> type == ScanTypeBio.LOG);
        sampleNumber = 0;
    }

    public static BioSpecies brainTree(){
        return builder()
                .id("Brain Tree")
                .name("Brain Tree")
                .specieName("")
                .variantLocalised("")
                .colonyRangeMeters(100)
                .baseValue(1593700)
                .bonusValue(1593700*4)
                .color("")
                .build();
    }
}