package be.mirooz.elitedangerous.dashboard.service.mapping;

import be.mirooz.elitedangerous.backend.generated.model.BodyResult;
import be.mirooz.elitedangerous.backend.generated.model.Genus;
import be.mirooz.elitedangerous.backend.generated.model.Material;
import be.mirooz.elitedangerous.backend.generated.model.Parent;
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

/**
 * Convertit une charge utile Spansh bodies (DTO {@code /api/spansh/bodies/search}) vers {@link SystemVisited}.
 */
@Slf4j
public final class SpanshSystemVisitedMapper {

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

    public static SystemVisited toSystemVisited(String spanshBodiesJson, String fallbackSystemName) throws IOException {
        if (spanshBodiesJson == null || spanshBodiesJson.isBlank()) {
            throw new IOException("Spansh bodies payload is empty");
        }
        JsonNode root = MAPPER.readTree(spanshBodiesJson);
        if (root.has("spanshResponse") && root.get("spanshResponse").isObject()) {
            SpanshSearchResponseDTO dto = MAPPER.treeToValue(root, SpanshSearchResponseDTO.class);
            return toSystemVisited(dto, fallbackSystemName);
        }
        if (root.has("results") && root.get("results").isArray()) {
            SpanshSearchResponse sr = MAPPER.treeToValue(root, SpanshSearchResponse.class);
            SpanshSearchResponseDTO dto = new SpanshSearchResponseDTO();
            dto.setSpanshResponse(sr);
            return toSystemVisited(dto, fallbackSystemName);
        }
        throw new IOException("Unsupported Spansh bodies JSON: expected spanshResponse object or results array at root");
    }

    private static String resolveSystemName(SpanshSearchResponse sr, String fallback) {
        if (sr.getReference() != null && sr.getReference().getName() != null && !sr.getReference().getName().isBlank()) {
            return sr.getReference().getName().trim();
        }
        if (sr.getSearch() != null && sr.getSearch().getReferenceSystem() != null
                && !sr.getSearch().getReferenceSystem().isBlank()) {
            return sr.getSearch().getReferenceSystem().trim();
        }
        if (sr.getResults() != null) {
            for (BodyResult br : sr.getResults()) {
                if (br != null && br.getSystemName() != null && !br.getSystemName().isBlank()) {
                    return br.getSystemName().trim();
                }
            }
        }
        return fallback != null && !fallback.isBlank() ? fallback.trim() : "Unknown";
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
        return StarDetail.builder()
                .timestamp(timestampOf(br))
                .jsonNode(toJsonNode(br))
                .bodyName(fallbackIfBlank(br.getName(), "Unknown body", "Unknown body"))
                .starSystem(systemName)
                .lsDistance(br.getDistanceToArrival())
                .systemAddress(valueOrZero(br.getId64()))
                .bodyID(valueOrZero(br.getBodyId()))
                .parents(mapSpanshParents(br, systemName, id64ToBodyId, nameIndex))
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
        PlaneteDetail planete = PlaneteDetail.builder()
                .timestamp(timestampOf(br))
                .jsonNode(toJsonNode(br))
                .bodyName(fallbackIfBlank(br.getName(), "Unknown body", "Unknown body"))
                .starSystem(systemName)
                .lsDistance(br.getDistanceToArrival())
                .systemAddress(valueOrZero(br.getId64()))
                .bodyID(valueOrZero(br.getBodyId()))
                .parents(mapSpanshParents(br, systemName, id64ToBodyId, nameIndex))
                .wasDiscovered(true)
                .wasMapped(true)
                .mapped(false)
                .rings(br.getRings() != null && !br.getRings().isEmpty())
                .planetClass(BodyType.fromString(planetClassStr))
                .massEM(br.getEarthMasses())
                .temperature(br.getSurfaceTemperature())
                .pressureAtm(br.getSurfacePressure())
                .gravityG(br.getGravity())
                .landable(Boolean.TRUE.equals(br.getIsLandable()))
                .radius(valueOrZero(br.getRadius()))
                .terraformable(isTerraformable(br.getTerraformingState()))
                .materials(mapSpanshMaterials(br.getMaterials()))
                .build();
        injectSpanshExobio(planete, br);
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
                List<BioSpecies> matchingSpecies = BioSpeciesService.getInstance().getSpecies();
                BioSpecies specie = matchingSpecies.stream()
                        .filter(s -> s.getName().equalsIgnoreCase(g.getLocalisedName()))
                        .filter(s -> s.getSpecieName().equalsIgnoreCase(g.getSpecies()))
                        .filter(s -> s.getColor().equalsIgnoreCase(g.getVariant()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No matching Spansh species found for system: " + planete.getStarSystem() + ", name: " + g.getLocalisedName() +
                                ", species: " + g.getSpecies() + ", variant: " + g.getVariant()));
                planete.setWasFootfalled(true);
                planete.getConfirmedSpecies().stream()
                        .filter(s -> s.getId().equalsIgnoreCase(specie.getId()))
                        .findFirst()
                        .orElseGet(() -> {
                            assert specie != null;
                            return planete.createNewSpecies(specie, genusCodex,specie.getFdevname(),true);
                        });

                SpeciesProbability speciesProbability = new SpeciesProbability(specie,100);
                planete.getBioSpecies().add(new Scan(1, new ArrayList<>(List.of(speciesProbability))));
                if (planete.getNumSpeciesDetected() == null){
                    planete.setNumSpeciesDetected(1);
                }
                else {
                    planete.setNumSpeciesDetected(planete.getNumSpeciesDetected() + 1);
                }
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

    private static JsonNode toJsonNode(BodyResult br) {
        JsonNode node = MAPPER.valueToTree(br);
        if (node instanceof ObjectNode objectNode) {
            objectNode.put("_source", "SPANSH");
        }
        return node;
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
