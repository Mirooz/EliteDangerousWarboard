package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Scan {
        private int scanNumber; // 1, 2 ou 3
        private List<SpeciesProbability> speciesProbabilities = new ArrayList<>();
    }