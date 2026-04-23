package be.mirooz.elitedangerous.dashboard.service.mapping;

import be.mirooz.elitedangerous.backend.generated.model.AtmosphereComposition;
import be.mirooz.elitedangerous.backend.generated.model.BodyResult;
import be.mirooz.elitedangerous.backend.generated.model.Genus;
import be.mirooz.elitedangerous.backend.generated.model.Material;
import be.mirooz.elitedangerous.backend.generated.model.Parent;
import be.mirooz.elitedangerous.backend.generated.model.Ring;
import be.mirooz.elitedangerous.backend.generated.model.Signal;
import be.mirooz.elitedangerous.backend.generated.model.SpanshSearchResponse;
import be.mirooz.elitedangerous.backend.generated.model.SpanshSearchResponseDTO;
import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.biologic.BodyType;
import be.mirooz.elitedangerous.biologic.ScanTypeBio;
import be.mirooz.elitedangerous.biologic.StarType;
import be.mirooz.elitedangerous.dashboard.model.exploration.*;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import be.mirooz.elitedangerous.service.BioSpeciesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convertit une charge utile Spansh bodies (DTO {@code /api/spansh/bodies/search}) vers {@link SystemVisited}.
 */
@Slf4j
public final class SpanshSystemVisitedMapper {

    /** Rayon solaire (m), cohérent avec les scans journal (champ {@code Radius} en mètres). */
    private static final double SOLAR_RADIUS_METERS = 6.957e8;

    private static final Pattern SUBCLASS_AFTER_SPECTRAL =
            Pattern.compile("(?i)[OBAFGKMNST](\\d)");

    private static final ObjectMapper MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }

    private SpanshSystemVisitedMapper() {
    }

    public static SystemVisited toSystemVisited(SpanshSearchResponseDTO dto, String fallbackSystemName) throws IOException {
        if (dto == null || dto.getSpanshResponse() == null) {
            throw new IOException("Spansh bodies search: spanshResponse is null");
        }
        SpanshSearchResponse sr = dto.getSpanshResponse();
        String visitTimestamp = OffsetDateTime.now().toString();
        Collection<ACelesteBody> bodies = mapBodyResults(fallbackSystemName, sr.getResults());
        SystemVisited visited = SystemVisited.builder()
                .systemName(fallbackSystemName)
                .firstDiscover(false)
                .firstVisitedTime(visitTimestamp)
                .lastVisitedTime(visitTimestamp)
                .numberVisited(1)
                .sold(false)
                .celesteBodies(bodies)
                .build();
        return visited;
    }




    private static Collection<ACelesteBody> mapBodyResults(String systemName, List<BodyResult> results) {
        Collection<ACelesteBody> out = new ArrayList<>();
        if (results == null) {
            return out;
        }
        Map<Long, Integer> id64ToBodyId = buildId64ToBodyId(results);
        Map<String, BodyResult> nameIndex = buildNameIndex(results);
        for (BodyResult br : results) {
            if (br == null) {
                continue;
            }
            out.add(isStar(br) ? toStar(systemName, br, id64ToBodyId, nameIndex)
                    : toPlanet(systemName, br, id64ToBodyId, nameIndex));
        }
        return out;
    }

    /** Permet de résoudre les parents Spansh ({@link Parent#getId64()}) vers un  journal. */
    private static Map<Long, Integer> buildId64ToBodyId(List<BodyResult> results) {
        Map<Long, Integer> map = new HashMap<>();
        for (BodyResult r : results) {
            if (r == null || r.getId64() == null || r.getBodyId() == null) {
                continue;
            }
            map.put(r.getId64(), r.getBodyId());
        }
        return map;
    }

    /**
     * Index des corps par nom complet (insensible à la casse) pour résoudre les parents Spansh {@code type: "Null"}.
     */
    private static Map<String, BodyResult> buildNameIndex(List<BodyResult> results) {
        Map<String, BodyResult> map = new HashMap<>();
        for (BodyResult r : results) {
            if (r == null || r.getName() == null || r.getName().isBlank()) {
                continue;
            }
            map.put(normalizeBodyKey(r.getName()), r);
        }
        return map;
    }

    private static String normalizeBodyKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static BodyResult findBodyByFullName(Map<String, BodyResult> nameIndex, String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        return nameIndex.get(normalizeBodyKey(fullName));
    }

    private static String inferParentFullNameByRemovingLastToken(String childFullName) {
        if (childFullName == null) {
            return null;
        }
        String child = childFullName.trim();
        if (child.isEmpty()) {
            return null;
        }
        int lastSpace = child.lastIndexOf(' ');
        if (lastSpace <= 0) {
            return null;
        }
        return child.substring(0, lastSpace).trim();
    }

    private static List<ParentBody> mapSpanshParents(
            BodyResult br,
            String systemName,
            Map<Long, Integer> id64ToBodyId,
            Map<String, BodyResult> nameIndex) {
        if (br.getParents() == null || br.getParents().isEmpty()) {
            return List.of();
        }
        String childName = br.getName();
        List<ParentBody> mapped = new ArrayList<>();
        for (Parent p : br.getParents()) {
            if (p == null) {
                continue;
            }
            String type = p.getType();
            if (type == null || type.isBlank()) {
                type = "Null";
            }
            int bodyId = 0;
            if (p.getId64() != null) {
                bodyId = id64ToBodyId.getOrDefault(p.getId64(), 0);
            }
            String resolvedType = type;
            if ("Null".equalsIgnoreCase(type)) {
                ParentBody inferred = resolveNullParentFromBodyName(childName, nameIndex);
                if (inferred != null) {
                    bodyId = inferred.getBodyID();
                    resolvedType = inferred.getType();
                }
            }
            mapped.add(ParentBody.builder().type(resolvedType).bodyID(bodyId).build());
        }
        return mapped;
    }

    private static ParentBody resolveNullParentFromBodyName(
            String childFullName,
            Map<String, BodyResult> nameIndex) {
        String parentFull = inferParentFullNameByRemovingLastToken(childFullName);
        if (parentFull == null) {
            return null;
        }
        BodyResult parentBr = findBodyByFullName(nameIndex, parentFull);
        if (parentBr == null || parentBr.getBodyId() == null) {
            return null;
        }
        String parentType = isStar(parentBr) ? "Star" : "Planet";
        return ParentBody.builder().type(parentType).bodyID(parentBr.getBodyId()).build();
    }

    private static boolean isStar(BodyResult br) {
        String t = br.getType();
        return t != null && "Star".equalsIgnoreCase(t.trim());
    }

    private static StarDetail toStar(
            String systemName, BodyResult br, Map<Long, Integer> id64ToBodyId, Map<String, BodyResult> nameIndex) {
        String starType = fallbackIfBlank(br.getSubtype(), br.getSpectralClass(), "Unknown");
        List<ParentBody> parents = mapSpanshParents(br, systemName, id64ToBodyId, nameIndex);
        long systemAddress = br.getSystemId64() != null ? br.getSystemId64() : valueOrZero(br.getId64());
        String resolvedSystem = fallbackIfBlank(br.getSystemName(), systemName, "Unknown");
        return StarDetail.builder()
                .timestamp(timestampOf(br))
                .jsonNode(starJournalJsonFromSpansh(br, resolvedSystem, parents))
                .bodyName(fallbackIfBlank(br.getName(), "Unknown body", "Unknown body"))
                .starSystem(resolvedSystem)
                .lsDistance(br.getDistanceToArrival())
                .systemAddress(systemAddress)
                .bodyID(valueOrZero(br.getBodyId()))
                .parents(parents)
                .wasDiscovered(true)
                .mapped(false)
                .rings(br.getRings() != null && !br.getRings().isEmpty())
                .starTypeString(starType)
                .starType(StarType.getStarType(starType))
                .stellarMass(valueOrZero(br.getSolarMasses()))
                .build();
    }

    private static PlaneteDetail toPlanet(
            String systemName, BodyResult br, Map<Long, Integer> id64ToBodyId, Map<String, BodyResult> nameIndex) {
        String planetClassStr = fallbackIfBlank(br.getSubtype(), "Unknown", "Unknown");
        List<ParentBody> parents = mapSpanshParents(br, systemName, id64ToBodyId, nameIndex);
        String resolvedSystem = fallbackIfBlank(br.getSystemName(), systemName, "Unknown");
        long systemAddress = br.getSystemId64() != null ? br.getSystemId64() : valueOrZero(br.getId64());
        Double pressurePa = br.getSurfacePressure();
        Double pressureAtm = pressurePa != null ? PlaneteDetail.pascalToAtm(pressurePa) : null;
        Double gravityG = br.getGravity() != null ? PlaneteDetail.ms2ToG(br.getGravity()) : null;
        PlaneteDetail planete = PlaneteDetail.builder()
                .timestamp(timestampOf(br))
                .bodyName(fallbackIfBlank(br.getName(), "Unknown body", "Unknown body"))
                .starSystem(resolvedSystem)
                .lsDistance(br.getDistanceToArrival())
                .systemAddress(systemAddress)
                .bodyID(valueOrZero(br.getBodyId()))
                .parents(parents)
                .wasDiscovered(true)
                .wasMapped(true)
                .mapped(false)
                .rings(br.getRings() != null && !br.getRings().isEmpty())
                .planetClass(BodyType.fromString(planetClassStr))
                .massEM(br.getEarthMasses())
                .temperature(br.getSurfaceTemperature())
                .pressureAtm(pressureAtm)
                .gravityG(gravityG)
                .landable(Boolean.TRUE.equals(br.getIsLandable()))
                .radius(valueOrZero(br.getRadius()))
                .terraformable(isTerraformable(br.getTerraformingState()))
                .materials(mapSpanshMaterials(br.getMaterials()))
                .jsonNode(MAPPER.createObjectNode())
                .build();
        injectSpanshExobio(planete, br);
        planete.setJsonNode(planetJournalJsonFromSpansh(
                br,
                resolvedSystem,
                parents,
                planete.isWasDiscovered(),
                planete.isWasMapped(),
                planete.isWasFootfalled()));
        return planete;
    }

    /** Matériaux Spansh : clés en minuscules (aligné sur les conditions bio surface). */
    private static Map<String, Double> mapSpanshMaterials(List<Material> materials) {
        if (materials == null || materials.isEmpty()) {
            return null;
        }
        Map<String, Double> out = new HashMap<>();
        for (Material m : materials) {
            if (m == null || m.getName() == null || m.getName().isBlank()) {
                continue;
            }
            String key = m.getName().trim().toLowerCase(Locale.ROOT);
            double share = m.getShare() != null ? m.getShare() : 0d;
            out.put(key, share);
        }
        return out.isEmpty() ? null : out;
    }

    /**
     * Applique signaux biologiques Spansh (FSS + SAA) sur la planète et notifie le {@link BiologicalSignalProcessor}
     * pour les corps déjà présents dans le registre.
     */
    private static void injectSpanshExobio(PlaneteDetail planete, BodyResult br)  {
        List<Genus> genuses = br.getGenuses();
        if (genuses == null || genuses.isEmpty()) {
            return;
        }
        for (Genus g : genuses) {
            if (g == null) {
                continue;
            }
            Optional<String> genusCodexOpt = spanshGenusToJournalCodex(g);
            if (genusCodexOpt.isEmpty()) {
                continue;
            }
            String genusCodex = genusCodexOpt.get();
            if (!isSaaGenusCodex(genusCodex)) {
                continue;
            }
            if (g.getLocalisedName() == null || g.getLocalisedName().isBlank()){
                continue;
            }
            try {
                BioSpecies specie;

                if ("Brain".equals(g.getSpecies())) {
                    specie = BioSpecies.brainTree();
                } else {
                    specie = BioSpeciesService.getInstance().getSpecies().stream()
                            .filter(s -> s.getName().equalsIgnoreCase(g.getLocalisedName()))
                            .filter(s -> s.getSpecieName().equalsIgnoreCase(g.getSpecies()))
                            .filter(s -> s.getColor().equalsIgnoreCase(g.getVariant()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                    "No matching Spansh species found for system: " + planete.getStarSystem() +
                                            ", name: " + g.getLocalisedName() +
                                            ", species: " + g.getSpecies() +
                                            ", variant: " + g.getVariant()
                            ));
                }
                planete.setWasFootfalled(true);
                planete.getConfirmedSpecies().stream()
                        .filter(s -> s.getId().equalsIgnoreCase(specie.getId()))
                        .findFirst()
                        .orElseGet(() -> planete.createNewSpecies(specie, genusCodex, specie.getFdevname(), true));
                SpeciesProbability speciesProbability = new SpeciesProbability(specie, 100);
                planete.getBioSpecies().add(new Scan(1, List.of(speciesProbability)));
                planete.setNumSpeciesDetected(
                        planete.getNumSpeciesDetected() == null
                                ? 1
                                : planete.getNumSpeciesDetected() + 1
                );
            }
            catch (Exception e){
                log.error("Error while processing spansh exobio", e);
            }
        }
    }

    private static int inferBioCountFromGenuses(List<Genus> genuses) {
        int sum = 0;
        for (Genus g : genuses) {
            if (g != null && g.getValue() != null) {
                sum += g.getValue();
            }
        }
        if (sum > 0) {
            return sum;
        }
        return Math.max(1, genuses.size());
    }

    private static boolean speciesExistsWithFdev(String fdev) {
        if (fdev == null || fdev.isBlank()) {
            return false;
        }
        try {
            return BioSpeciesService.getInstance().getSpecies().stream()
                    .anyMatch(s -> s.getFdevname() != null && fdev.equalsIgnoreCase(s.getFdevname()));
        } catch (URISyntaxException | IOException e) {
            return false;
        }
    }

    /**
     * Codex genre au format journal / SAASignalsFound : {@code $Codex_Ent_<Famille>_Genus_Name;}.
     */
    private static Optional<String> spanshGenusToJournalCodex(Genus g) {
        String raw = g.getName();
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String t = raw.trim();
        if (t.startsWith("$Codex_")) {
            return Optional.of(normalizeCodexSemicolon(t));
        }
        String token = toCodexFamilyToken(t);
        if (token.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("$Codex_Ent_" + token + "_Genus_Name;");
    }

    private static String normalizeCodexSemicolon(String codex) {
        String c = codex.trim();
        return c.endsWith(";") ? c : c + ";";
    }

    private static String toCodexFamilyToken(String raw) {
        String[] words = raw.trim().split("[\\s\\-_]+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) {
                sb.append(w.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    private static boolean isSaaGenusCodex(String genusCodex) {
        String[] parts = genusCodex.split("_");
        return parts.length > 3 && "Genus".equalsIgnoreCase(parts[3]);
    }






    private static String timestampOf(BodyResult br) {
        if (br.getUpdatedAt() != null) {
            return br.getUpdatedAt().toString();
        }
        return OffsetDateTime.now().toString();
    }

    /**
     * JSON façon événement journal {@code Scan} pour une étoile, à partir des champs Spansh disponibles.
     * Les clés absentes de Spansh (ex. {@code AbsoluteMagnitude}, {@code AscendingNode}) ne sont pas ajoutées.
     */
    private static JsonNode starJournalJsonFromSpansh(
            BodyResult br, String fallbackSystemName, List<ParentBody> parents) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("event", "Scan");
        root.put("ScanType", "Detailed");
        root.put("timestamp", timestampOf(br));
        root.put("BodyName", fallbackIfBlank(br.getName(), "Unknown body", "Unknown body"));
        root.put("BodyID", valueOrZero(br.getBodyId()));
        String starSystem = fallbackIfBlank(br.getSystemName(), fallbackSystemName, "Unknown");
        root.put("StarSystem", starSystem);
        long systemAddress = br.getSystemId64() != null ? br.getSystemId64() : valueOrZero(br.getId64());
        root.put("SystemAddress", systemAddress);
        putOptionalDouble(root, "DistanceFromArrivalLS", br.getDistanceToArrival());

        String spectral = br.getSpectralClass() != null ? br.getSpectralClass().trim() : "";
        String subtype = br.getSubtype() != null ? br.getSubtype().trim() : "";
        root.put("StarType", journalStarTypeFromSpansh(spectral, subtype));
        root.put("Subclass", inferStarSubclass(spectral, subtype));
        putOptionalDouble(root, "StellarMass", br.getSolarMasses());
        putOptionalDouble(root, "Radius", starRadiusMetersFromSpansh(br));
        putOptionalDouble(root, "Age_MY", br.getAge());
        putOptionalDouble(root, "SurfaceTemperature", br.getSurfaceTemperature());
        if (br.getLuminosityClass() != null && !br.getLuminosityClass().isBlank()) {
            root.put("Luminosity", br.getLuminosityClass().trim());
        }
        putOptionalDouble(root, "SemiMajorAxis", br.getSemiMajorAxis());
        putOptionalDouble(root, "Eccentricity", br.getOrbitalEccentricity());
        putOptionalDouble(root, "OrbitalInclination", br.getOrbitalInclination());
        putOptionalDouble(root, "Periapsis", br.getArgOfPeriapsis());
        putOptionalDouble(root, "OrbitalPeriod", br.getOrbitalPeriod());
        putOptionalDouble(root, "RotationPeriod", br.getRotationalPeriod());
        putOptionalDouble(root, "AxialTilt", br.getAxisTilt());

        root.put("WasDiscovered", true);
        root.put("WasMapped", false);
        root.put("WasFootfalled", false);
        root.put("_source", "SPANSH");

        putJournalParentsArray(root, parents);
        putJournalRingsFromBody(root, br);

        return root;
    }

    /**
     * JSON façon événement journal {@code Scan} pour une planète / lune, à partir des champs Spansh disponibles.
     */
    private static JsonNode planetJournalJsonFromSpansh(
            BodyResult br,
            String resolvedSystem,
            List<ParentBody> parents,
            boolean wasDiscovered,
            boolean wasMapped,
            boolean wasFootfalled) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("event", "Scan");
        root.put("ScanType", "Detailed");
        root.put("timestamp", timestampOf(br));
        root.put("BodyName", fallbackIfBlank(br.getName(), "Unknown body", "Unknown body"));
        root.put("BodyID", valueOrZero(br.getBodyId()));
        root.put("StarSystem", resolvedSystem);
        long systemAddress = br.getSystemId64() != null ? br.getSystemId64() : valueOrZero(br.getId64());
        root.put("SystemAddress", systemAddress);
        putOptionalDouble(root, "DistanceFromArrivalLS", br.getDistanceToArrival());

        root.put("TidalLock", Boolean.TRUE.equals(br.getIsRotationalPeriodTidallyLocked()));
        root.put("TerraformState", br.getTerraformingState() != null ? br.getTerraformingState() : "");
        root.put("PlanetClass", fallbackIfBlank(br.getSubtype(), "Unknown", "Unknown"));
        root.put("Atmosphere", br.getAtmosphere() != null ? br.getAtmosphere() : "");
        root.put("Volcanism", br.getVolcanismType() != null ? br.getVolcanismType() : "");

        putOptionalDouble(root, "MassEM", br.getEarthMasses());
        putOptionalDouble(root, "Radius", br.getRadius());
        putOptionalDouble(root, "SurfaceGravity", br.getGravity());
        putOptionalDouble(root, "SurfaceTemperature", br.getSurfaceTemperature());
        putOptionalDouble(root, "SurfacePressure", br.getSurfacePressure());
        root.put("Landable", Boolean.TRUE.equals(br.getIsLandable()));

        putOptionalDouble(root, "SemiMajorAxis", br.getSemiMajorAxis());
        putOptionalDouble(root, "Eccentricity", br.getOrbitalEccentricity());
        putOptionalDouble(root, "OrbitalInclination", br.getOrbitalInclination());
        putOptionalDouble(root, "Periapsis", br.getArgOfPeriapsis());
        putOptionalDouble(root, "OrbitalPeriod", br.getOrbitalPeriod());
        putOptionalDouble(root, "RotationPeriod", br.getRotationalPeriod());
        putOptionalDouble(root, "AxialTilt", br.getAxisTilt());

        if (br.getReserveLevel() != null && !br.getReserveLevel().isBlank()) {
            root.put("ReserveLevel", br.getReserveLevel().trim());
        }

        if (br.getAtmosphereComposition() != null && !br.getAtmosphereComposition().isEmpty()) {
            ArrayNode arr = MAPPER.createArrayNode();
            for (AtmosphereComposition ac : br.getAtmosphereComposition()) {
                if (ac == null || ac.getName() == null || ac.getName().isBlank()) {
                    continue;
                }
                ObjectNode o = MAPPER.createObjectNode();
                o.put("Name", ac.getName().trim());
                o.put("Percent", shareToJournalPercent(ac.getShare()));
                arr.add(o);
            }
            if (!arr.isEmpty()) {
                root.set("AtmosphereComposition", arr);
            }
        }

        if (br.getMaterials() != null && !br.getMaterials().isEmpty()) {
            ArrayNode marr = MAPPER.createArrayNode();
            for (Material m : br.getMaterials()) {
                if (m == null || m.getName() == null || m.getName().isBlank()) {
                    continue;
                }
                ObjectNode o = MAPPER.createObjectNode();
                o.put("Name", m.getName().trim());
                o.put("Percent", shareToJournalPercent(m.getShare()));
                marr.add(o);
            }
            if (!marr.isEmpty()) {
                root.set("Materials", marr);
            }
        }

        putJournalParentsArray(root, parents);
        putJournalRingsFromBody(root, br);

        root.put("WasDiscovered", wasDiscovered);
        root.put("WasMapped", wasMapped);
        root.put("WasFootfalled", wasFootfalled);
        root.put("_source", "SPANSH");

        return root;
    }

    private static void putJournalParentsArray(ObjectNode root, List<ParentBody> parents) {
        if (parents == null || parents.isEmpty()) {
            return;
        }
        ArrayNode parr = MAPPER.createArrayNode();
        for (ParentBody pb : parents) {
            if (pb == null) {
                continue;
            }
            ObjectNode p = MAPPER.createObjectNode();
            String t = pb.getType();
            int id = pb.getBodyID();
            if (t != null && t.equalsIgnoreCase("Planet")) {
                p.put("Planet", id);
            } else if (t != null && t.equalsIgnoreCase("Star")) {
                p.put("Star", id);
            } else {
                p.put("Null", id);
            }
            parr.add(p);
        }
        if (!parr.isEmpty()) {
            root.set("Parents", parr);
        }
    }

    private static void putJournalRingsFromBody(ObjectNode root, BodyResult br) {
        if (br.getRings() == null || br.getRings().isEmpty()) {
            return;
        }
        ArrayNode rings = MAPPER.createArrayNode();
        for (Ring ring : br.getRings()) {
            if (ring == null) {
                continue;
            }
            ObjectNode rn = MAPPER.createObjectNode();
            if (ring.getName() != null) {
                rn.put("Name", ring.getName());
            }
            if (ring.getType() != null) {
                rn.put("RingClass", ring.getType());
            }
            putOptionalDouble(rn, "MassMT", ring.getMass());
            putOptionalDouble(rn, "InnerRad", ring.getInnerRadius());
            putOptionalDouble(rn, "OuterRad", ring.getOuterRadius());
            rings.add(rn);
        }
        if (!rings.isEmpty()) {
            root.set("Rings", rings);
        }
    }

    /** Spansh {@code share} est souvent une fraction 0–1 ; le journal utilise un pourcentage. */
    private static double shareToJournalPercent(Double share) {
        if (share == null || share.isNaN() || share.isInfinite()) {
            return 0d;
        }
        if (share >= 0d && share <= 1.0001d) {
            return share * 100d;
        }
        return share;
    }

    private static String journalStarTypeFromSpansh(String spectral, String subtype) {
        if (spectral != null && !spectral.isBlank()) {
            String s = spectral.trim();
            if (s.length() <= 4) {
                return s;
            }
            return s.substring(0, Math.min(4, s.length()));
        }
        if (subtype != null && !subtype.isBlank()) {
            String first = subtype.trim().split("\\s+")[0];
            return first.replaceAll("\\d", "").trim();
        }
        return "Unknown";
    }

    private static int inferStarSubclass(String spectral, String subtype) {
        String combined = (spectral != null ? spectral : "") + " " + (subtype != null ? subtype : "");
        Matcher m = SUBCLASS_AFTER_SPECTRAL.matcher(combined);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static Double starRadiusMetersFromSpansh(BodyResult br) {
        if (br.getRadius() != null && br.getRadius() > 0 && !br.getRadius().isNaN()) {
            return br.getRadius();
        }
        if (br.getSolarRadius() != null && br.getSolarRadius() > 0 && !br.getSolarRadius().isNaN()) {
            return br.getSolarRadius() * SOLAR_RADIUS_METERS;
        }
        return null;
    }

    private static void putOptionalDouble(ObjectNode node, String key, Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return;
        }
        node.put(key, value);
    }

    private static boolean isTerraformable(String terraformingState) {
        if (terraformingState == null) {
            return false;
        }
        String normalized = terraformingState.trim().toLowerCase();
        if (normalized.isBlank()) {
            return false;
        }
        return !normalized.contains("not");
    }

    private static String fallbackIfBlank(String value, String fallback, String defaultValue) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return defaultValue;
    }

    private static int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }

    private static long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private static double valueOrZero(Double value) {
        return value != null ? value : 0d;
    }

}
