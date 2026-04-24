package be.mirooz.elitedangerous.eddn;

/**
 * Références de schémas publiés sur EDDN (https://eddn.edcd.io/schemas/).
 * <p>La production utilise les URLs sans suffixe, le banc de test ajoute {@code /test}
 * (non utilisé ici : on publie directement sur le gateway de prod).
 */
public final class EddnSchemas {

    private EddnSchemas() {}

    private static final String BASE = "https://eddn.edcd.io/schemas/";

    /** {@code Docked}, {@code FSDJump}, {@code Location}, {@code CarrierJump}, {@code Scan}, {@code SAASignalsFound}. */
    public static final String JOURNAL_V1              = BASE + "journal/1";
    public static final String APPROACH_SETTLEMENT_V1  = BASE + "approachsettlement/1";
    public static final String CODEX_ENTRY_V1          = BASE + "codexentry/1";
    public static final String COMMODITY_V3            = BASE + "commodity/3";
    public static final String DOCKING_DENIED_V1       = BASE + "dockingdenied/1";
    public static final String DOCKING_GRANTED_V1      = BASE + "dockinggranted/1";
    public static final String FC_MATERIALS_CAPI_V1    = BASE + "fcmaterials_capi/1";
    public static final String FC_MATERIALS_JOURNAL_V1 = BASE + "fcmaterials_journal/1";
    public static final String FSS_ALL_BODIES_FOUND_V1 = BASE + "fssallbodiesfound/1";
    public static final String FSS_BODY_SIGNALS_V1     = BASE + "fssbodysignals/1";
    public static final String FSS_DISCOVERY_SCAN_V1   = BASE + "fssdiscoveryscan/1";
    public static final String FSS_SIGNAL_DISCOVERED_V1 = BASE + "fsssignaldiscovered/1";
    public static final String NAV_BEACON_SCAN_V1      = BASE + "navbeaconscan/1";
    public static final String NAV_ROUTE_V1            = BASE + "navroute/1";
    public static final String OUTFITTING_V2           = BASE + "outfitting/2";
    public static final String SCAN_BARY_CENTRE_V1     = BASE + "scanbarycentre/1";
    public static final String SHIPYARD_V2             = BASE + "shipyard/2";
}
