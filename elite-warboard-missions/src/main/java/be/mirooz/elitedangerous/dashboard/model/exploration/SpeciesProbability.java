package be.mirooz.elitedangerous.dashboard.model.exploration;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpeciesProbability {
        private BioSpecies bioSpecies;
        private double probability; // ou BigDecimal
    }