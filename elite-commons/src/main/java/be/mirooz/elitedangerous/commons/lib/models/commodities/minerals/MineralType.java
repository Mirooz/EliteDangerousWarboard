package be.mirooz.elitedangerous.commons.lib.models.commodities.minerals;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.*;

public enum MineralType implements Mineral {

    LOW_TEMPERATURE_DIAMONDS(
            "144",
            "Low Temperature Diamonds",
            "LowTemperatureDiamond",
            "lowtemperaturediamond",
            "LowTemperatureDiamond",
            "$lowtemperaturediamond_name;"
            , MiningMethod.LASER

    ),

    MONAZITE(
            "10245",
            "Monazite",
            "Monazite",
            "monazite",
            "Monazite",
            "$monazite_name;"
            , MiningMethod.CORE
    ),

    MUSGRAVITE(
            "10246",
            "Musgravite",
            "Musgravite",
            "musgravite",
            "Musgravite",
            "$musgravite_name;"
            , MiningMethod.CORE
    ),

    BENITOITE(
            "10247",
            "Benitoite",
            "Benitoite",
            "benitoite",
            "Benitoite",
            "$benitoite_name;"
            , MiningMethod.CORE
    ),

    GRANDIDIERITE(
            "10248",
            "Grandidierite",
            "Grandidierite",
            "grandidierite",
            "Grandidierite",
            "$grandidierite_name;"
            , MiningMethod.CORE
    ),

    ALEXANDRITE(
            "10249",
            "Alexandrite",
            "Alexandrite",
            "alexandrite",
            "Alexandrite",
            "$alexandrite_name;"
            , MiningMethod.CORE
    ),

    VOID_OPAL(
            "10250",
            "Void Opal",
            "Opal",
            "opal",
            "Opal",
            "$opal_name;"
            , MiningMethod.CORE
    ),

    RHODPLUMSITE(
            "10243",
            "Rhodplumsite",
            "Rhodplumsite",
            "rhodplumsite",
            "Rhodplumsite",
            "$rhodplumsite_name;"
            , MiningMethod.CORE
    ),

    SERENDIBITE(
            "10244",
            "Serendibite",
            "Serendibite",
            "serendibite",
            "Serendibite",
            "$serendibite_name;"
            , MiningMethod.CORE
    ),

    PAINITE(
            "84",
            "Painite",
            "Painite",
            "painite",
            "Painite",
            "$painite_name;"
            , MiningMethod.LASER
    ),

    BROMELLITE(
            "148",
            "Bromellite",
            "Bromellite",
            "bromellite",
            "Bromellite",
            "$bromellite_name;"
            , MiningMethod.LASER
    ),
    TRITIUM(
            "10269",
            "Tritium",
            "Tritium",
            "tritium",
            "Tritium",
            "$tritium_name;"
            , MiningMethod.LASER
    ),
    PLATINUM(
            "81",
            "Platinum",
            "Platinum",
            "platinum",
            "Platinum",
            "$platinum_name;"
            , MiningMethod.LASER
    );

    private final String inaraId;
    private final String inaraName;
    private final String edToolName;
    private final String cargoJsonName;
    private final String coreMineralName;
    private final String miningRefinedName;
    private final MiningMethod miningMethod;

    private final IntegerProperty price = new SimpleIntegerProperty();

    MineralType(
            String inaraId,
            String inaraName,
            String edToolName,
            String cargoJsonName,
            String coreMineralName,
            String miningRefinedName,
            MiningMethod miningMethod
    ) {
        this.inaraId = inaraId;
        this.inaraName = inaraName;
        this.edToolName = edToolName;
        this.cargoJsonName = cargoJsonName;
        this.coreMineralName = coreMineralName;
        this.miningRefinedName = miningRefinedName;
        this.miningMethod = miningMethod;
    }

    @Override
    public String getInaraId() {
        return inaraId;
    }

    @Override
    public String getInaraName() {
        return inaraName;
    }

    @Override
    public String getEdToolName() {
        return edToolName;
    }

    @Override
    public String getCargoJsonName() {
        return cargoJsonName;
    }

    @Override
    public String getMiningRefinedName() {
        return miningRefinedName;
    }

    @Override
    public int getPrice() {
        return price.get();
    }

    @Override
    public IntegerProperty getPriceProperty(){
        return price;
    }
    @Override
    public void setPrice(int price) {
        this.price.set(price);
    }

    @Override
    public CommodityType getCommodityType() {
        return CommodityType.MINERAL;
    }

    @Override
    public MiningMethod getMiningMethod() {
        return miningMethod;
    }

    @Override
    public String toString() {
        return getVisibleName();
    }

    public static Optional<MineralType> fromCargoJsonName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();

        for (be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType type : values()) {
            if (type.cargoJsonName.equalsIgnoreCase(name)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static Optional<MineralType> fromCoreMineralName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();

        for (be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType type : values()) {
            if (type.coreMineralName.equalsIgnoreCase(name)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static List<MineralType> all() {
        return Arrays.asList(values());
    }
}
