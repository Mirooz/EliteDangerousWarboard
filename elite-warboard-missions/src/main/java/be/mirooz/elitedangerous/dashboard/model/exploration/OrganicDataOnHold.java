package be.mirooz.elitedangerous.dashboard.model.exploration;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganicDataOnHold {
    @Builder.Default
    private List<BioSpecies> bioData = new ArrayList<>();

    private long totalValue; // Somme de toutes les valeurs
    private long totalBonus; // Somme de tous les bonus
}



