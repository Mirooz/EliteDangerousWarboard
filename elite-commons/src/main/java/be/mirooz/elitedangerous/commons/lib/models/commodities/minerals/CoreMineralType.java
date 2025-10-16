package be.mirooz.elitedangerous.commons.lib.models.commodities.minerals;

import org.reflections.Reflections;

import java.util.*;

public enum CoreMineralType implements Mineral {

    LOW_TEMPERATURE_DIAMONDS(
        "144",
        "Low Temperature Diamonds",
        "LowTemperatureDiamond",
        "lowtemperaturediamond",
        "LowTemperatureDiamond",
        "$lowtemperaturediamond_name;"
    ),

    MONAZITE(
        "10245",
        "Monazite",
        "Monazite",
        "monazite",
        "Monazite",
        "$monazite_name;"
    ),

    MUSGRAVITE(
        "10246",
        "Musgravite",
        "Musgravite",
        "musgravite",
        "Musgravite",
        "$musgravite_name;"
    ),

    BENITOITE(
        "10247",
        "Benitoite",
        "Benitoite",
        "benitoite",
        "Benitoite",
        "$benitoite_name;"
    ),

    GRANDIDIERITE(
        "10248",
        "Grandidierite",
        "Grandidierite",
        "grandidierite",
        "Grandidierite",
        "$grandidierite_name;"
    ),

    ALEXANDRITE(
        "10249",
        "Alexandrite",
        "Alexandrite",
        "alexandrite",
        "Alexandrite",
        "$alexandrite_name;"
    ),

    VOID_OPAL(
        "10250",
        "Void Opal",
        "Opal",
        "opal",
        "Opal",
        "$opal_name;"
    ),

    RHODPLUMSITE(
        "10243",
        "Rhodplumsite",
        "Rhodplumsite",
        "rhodplumsite",
        "Rhodplumsite",
        "$rhodplumsite_name;"
    ),

    SERENDIBITE(
        "10244",
        "Serendibite",
        "Serendibite",
        "serendibite",
        "Serendibite",
        "$serendibite_name;"
    ),

    PAINITE(
        "84",
        "Painite",
        "Painite",
        "painite",
        "Painite",
        "$painite_name;"
    ),

    BROMELLITE(
        "148",
        "Bromellite",
        "Bromellite",
        "bromellite",
        "Bromellite",
        "$bromellite_name;"
    );

    private final String inaraId;
    private final String inaraName;
    private final String edToolName;
    private final String cargoJsonName;
    private final String coreMineralName;
    private final String miningRefinedName;

    CoreMineralType(
        String inaraId,
        String inaraName,
        String edToolName,
        String cargoJsonName,
        String coreMineralName,
        String miningRefinedName
    ) {
        this.inaraId = inaraId;
        this.inaraName = inaraName;
        this.edToolName = edToolName;
        this.cargoJsonName = cargoJsonName;
        this.coreMineralName = coreMineralName;
        this.miningRefinedName = miningRefinedName;
    }

    @Override public String getInaraId() { return inaraId; }
    @Override public String getInaraName() { return inaraName; }
    @Override public String getEdToolName() { return edToolName; }
    @Override public String getCargoJsonName() { return cargoJsonName; }
    @Override public String getMiningRefinedName() { return miningRefinedName; }
    @Override public CommodityType getCommodityType() { return CommodityType.MINERAL; }
    @Override public MineralType getMineralType() { return MineralType.CORE_MINERAL; }

    @Override
    public String toString() {
        return getInaraName() + " (" + getInaraId() + ")";
    }

    public static Optional<CoreMineralType> fromCargoJsonName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        
        for (CoreMineralType type : values()) {
            if (type.cargoJsonName.equalsIgnoreCase(name)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static Optional<CoreMineralType> fromCoreMineralName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        
        for (CoreMineralType type : values()) {
            if (type.coreMineralName.equalsIgnoreCase(name)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static List<CoreMineralType> all() {
        return Arrays.asList(values());
    }
}
