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
    ),

    // -- Additional minerals (default set as trash value) --
    // Laser-mined common minerals
    BAUXITE(
            "51",
            "Bauxite",
            "Bauxite",
            "bauxite",
            "Bauxite",
            "$bauxite_name;",
            MiningMethod.LASER
    ),
    BERTRANDITE(
            "52",
            "Bertrandite",
            "Bertrandite",
            "bertrandite",
            "Bertrandite",
            "$bertrandite_name;",
            MiningMethod.LASER
    ),
    COLTAN(
            "55",
            "Coltan",
            "Coltan",
            "coltan",
            "Coltan",
            "$coltan_name;",
            MiningMethod.LASER
    ),
    CRYOLITE(
            "110",
            "Cryolite",
            "Cryolite",
            "cryolite",
            "Cryolite",
            "$cryolite_name;",
            MiningMethod.LASER
    ),
    GALLITE(
            "56",
            "Gallite",
            "Gallite",
            "gallite",
            "Gallite",
            "$gallite_name;",
            MiningMethod.LASER
    ),
    GOSLARITE(
            "111",
            "Goslarite",
            "Goslarite",
            "goslarite",
            "Goslarite",
            "$goslarite_name;",
            MiningMethod.LASER
    ),
    INDITE(
            "57",
            "Indite",
            "Indite",
            "indite",
            "Indite",
            "$indite_name;",
            MiningMethod.LASER
    ),
    JADEITE(
            "168",
            "Jadeite",
            "Jadeite",
            "jadeite",
            "Jadeite",
            "$jadeite_name;",
            MiningMethod.LASER
    ),
    LEPIDOLITE(
            "58",
            "Lepidolite",
            "Lepidolite",
            "lepidolite",
            "Lepidolite",
            "$lepidolite_name;",
            MiningMethod.LASER
    ),
    LITHIUM_HYDROXIDE(
            "147",
            "Lithium Hydroxide",
            "LithiumHydroxide",
            "lithiumhydroxide",
            "LithiumHydroxide",
            "$lithiumhydroxide_name;",
            MiningMethod.LASER
    ),
    METHANE_CLATHRATE(
            "145",
            "Methane Clathrate",
            "MethaneClathrate",
            "methaneclathrate",
            "MethaneClathrate",
            "$methaneclathrate_name;",
            MiningMethod.LASER
    ),
    METHANOL_MONOHYDRATE_CRYSTALS(
            "146",
            "Methanol Monohydrate Crystals",
            "MethanolMonohydrateCrystals",
            "methanolmonohydratecrystals",
            "MethanolMonohydrateCrystals",
            "$methanolmonohydratecrystals_name;",
            MiningMethod.LASER
    ),
    MOISSANITE(
            "116",
            "Moissanite",
            "Moissanite",
            "moissanite",
            "Moissanite",
            "$moissanite_name;",
            MiningMethod.LASER
    ),
    PYROPHYLLITE(
            "112",
            "Pyrophyllite",
            "Pyrophyllite",
            "pyrophyllite",
            "Pyrophyllite",
            "$pyrophyllite_name;",
            MiningMethod.LASER
    ),
    RUTILE(
            "59",
            "Rutile",
            "Rutile",
            "rutile",
            "Rutile",
            "$rutile_name;",
            MiningMethod.LASER
    ),
    TAAFFEITE(
            "120",
            "Taaffeite",
            "Taaffeite",
            "taaffeite",
            "Taaffeite",
            "$taaffeite_name;",
            MiningMethod.LASER
    ),
    URANINITE(
            "60",
            "Uraninite",
            "Uraninite",
            "uraninite",
            "Uraninite",
            "$uraninite_name;",
            MiningMethod.LASER
    ),

    // Precious/metals (commodities often mined/refined)
    GOLD(
            "42",
            "Gold",
            "Gold",
            "gold",
            "Gold",
            "$gold_name;",
            MiningMethod.LASER
    ),
    SILVER(
            "46",
            "Silver",
            "Silver",
            "silver",
            "Silver",
            "$silver_name;",
            MiningMethod.LASER
    ),
    PALLADIUM(
            "45",
            "Palladium",
            "Palladium",
            "palladium",
            "Palladium",
            "$palladium_name;",
            MiningMethod.LASER
    ),
    OSMIUM(
            "72",
            "Osmium",
            "Osmium",
            "osmium",
            "Osmium",
            "$osmium_name;",
            MiningMethod.LASER
    ),
    COBALT(
            "39",
            "Cobalt",
            "Cobalt",
            "cobalt",
            "Cobalt",
            "$cobalt_name;",
            MiningMethod.LASER
    ),

    // Chemicals (treated here for display; mined via sub-surface/laser in gameplay)
    WATER(
            "139",
            "Water",
            "Water",
            "water",
            "Water",
            "$water_name;",
            MiningMethod.LASER
    ),
    LIQUID_OXYGEN(
            "137",
            "Liquid Oxygen",
            "LiquidOxygen",
            "liquidoxygen",
            "LiquidOxygen",
            "$liquidoxygen_name;",
            MiningMethod.LASER
    ),
    HYDROGEN_PEROXIDE(
            "138",
            "Hydrogen Peroxide",
            "HydrogenPeroxide",
            "hydrogenperoxide",
            "HydrogenPeroxide",
            "$hydrogenperoxide_name;",
            MiningMethod.LASER
    ),
    HAEMATITE(
            "10486",
            "Haematite",
            "haematite",
            "haematite",
            "haematite",
            "$haematite_name;",
            MiningMethod.LASER
    );

    private final String inaraId;
    private final String inaraName;
    private final String edToolName;
    private final String cargoJsonName;
    private final String coreMineralName;
    private final String miningRefinedName;
    private final MiningMethod miningMethod;

    private final IntegerProperty price = new SimpleIntegerProperty();
    private boolean trashValue = false;

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

    // Set trash value true for common/low-value minerals we added
    static {
        setTrashItems();
        setInitPriceForTrash();


    }

    private static void setInitPriceForTrash() {
        BAUXITE.setPrice(2882);
        BERTRANDITE.setPrice(18418);
        COLTAN.setPrice(6131);
        CRYOLITE.setPrice(16489);
        GALLITE.setPrice(12152);
        GOSLARITE.setPrice(8596);
        INDITE.setPrice(11222);
        JADEITE.setPrice(43134);
        LEPIDOLITE.setPrice(2050);
        LITHIUM_HYDROXIDE.setPrice(5649);
        METHANE_CLATHRATE.setPrice(1594);
        METHANOL_MONOHYDRATE_CRYSTALS.setPrice(2554);
        MOISSANITE.setPrice(30194);
        PYROPHYLLITE.setPrice(15940);
        RUTILE.setPrice(3631);
        TAAFFEITE.setPrice(53071);
        URANINITE.setPrice(3002);
        WATER.setPrice(1910);
        LIQUID_OXYGEN.setPrice(4267);
        HYDROGEN_PEROXIDE.setPrice(3301);
        GOLD.setPrice(48302);
        SILVER.setPrice(38345);
        PALLADIUM.setPrice(52368);
        OSMIUM.setPrice(66991);
        COBALT.setPrice(5400);
        HAEMATITE.setPrice(10044);
    }

    private static void setTrashItems() {
        BAUXITE.setTrashValue(true);
        BERTRANDITE.setTrashValue(true);
        COLTAN.setTrashValue(true);
        CRYOLITE.setTrashValue(true);
        GALLITE.setTrashValue(true);
        GOSLARITE.setTrashValue(true);
        INDITE.setTrashValue(true);
        JADEITE.setTrashValue(true);
        LEPIDOLITE.setTrashValue(true);
        LITHIUM_HYDROXIDE.setTrashValue(true);
        METHANE_CLATHRATE.setTrashValue(true);
        METHANOL_MONOHYDRATE_CRYSTALS.setTrashValue(true);
        MOISSANITE.setTrashValue(true);
        PYROPHYLLITE.setTrashValue(true);
        RUTILE.setTrashValue(true);
        TAAFFEITE.setTrashValue(true);
        URANINITE.setTrashValue(true);
        WATER.setTrashValue(true);
        LIQUID_OXYGEN.setTrashValue(true);
        HYDROGEN_PEROXIDE.setTrashValue(true);
        // Precious/metals
        GOLD.setTrashValue(true);
        SILVER.setTrashValue(true);
        PALLADIUM.setTrashValue(true);
        OSMIUM.setTrashValue(true);
        COBALT.setTrashValue(true);
        HAEMATITE.setTrashValue(true);
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
    public boolean isTrashValue() {
        return trashValue;
    }

    @Override
    public void setTrashValue(boolean trash) {
        this.trashValue = trash;
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

    public static Optional<MineralType> fromInaraId(String inaraId) {
        if (inaraId == null || inaraId.isBlank()) return Optional.empty();

        for (be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType type : values()) {
            if (inaraId.equals(type.inaraId)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static List<MineralType> all() {
        return Arrays.asList(values());
    }
}
