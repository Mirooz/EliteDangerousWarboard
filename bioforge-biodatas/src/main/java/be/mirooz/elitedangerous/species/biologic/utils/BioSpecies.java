package be.mirooz.elitedangerous.species.biologic.utils;

import lombok.Data;

import java.util.List;

@Data
public abstract class BioSpecies {

    protected final String name;
    protected final long baseValue;
    protected final long firstLoggedValue;
    protected final double colonyRangeMeters;
    protected final VariantMethods variantMethod;
    protected final List<Variant> variants;
    
    protected BioSpecies(String name, long baseValue, long firstLoggedValue, double colonyRangeMeters, VariantMethods variantMethod, List<Variant> variants) {
        this.name = name;
        this.baseValue = baseValue;
        this.firstLoggedValue = firstLoggedValue;
        this.colonyRangeMeters = colonyRangeMeters;
        this.variantMethod = variantMethod;
        this.variants = variants;
    }
}
