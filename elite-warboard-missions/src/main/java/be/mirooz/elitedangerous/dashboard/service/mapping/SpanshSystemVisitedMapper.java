package be.mirooz.elitedangerous.dashboard.service.mapping;

import be.mirooz.elitedangerous.backend.generated.model.BodyResult;
import be.mirooz.elitedangerous.backend.generated.model.EdsmBodiesResponse;
import be.mirooz.elitedangerous.backend.generated.model.Parent;
import be.mirooz.elitedangerous.backend.generated.model.SpanshSearchResponse;
import be.mirooz.elitedangerous.backend.generated.model.SpanshSearchResponseDTO;
import be.mirooz.elitedangerous.biologic.BodyType;
import be.mirooz.elitedangerous.biologic.StarType;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.ParentBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.StarDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convertit une charge utile Spansh bodies (JSON legacy EDSM-like ou DTO {@code /api/spansh/bodies/search}) vers {@link SystemVisited}.
 */
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
        markOnlineData(visited);
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
        ObjectNode normalized = normalize(root, fallbackSystemName);
        EdsmBodiesResponse response = MAPPER.treeToValue(normalized, EdsmBodiesResponse.class);
        return toSystemVisitedFromEdsm(response, fallbackSystemName);
    }

    private static SystemVisited toSystemVisitedFromEdsm(EdsmBodiesResponse response, String fallbackSystemName) {
        SystemVisited visited = EdsmSystemVisitedMapper.toSystemVisited(response, fallbackSystemName);
        markOnlineData(visited);
        return visited;
    }

    private static void markOnlineData(SystemVisited visited) {
        if (visited.getCelesteBodies() != null) {
            for (ACelesteBody b : visited.getCelesteBodies()) {
                if (b != null) {
                    b.setOnlineData(true);
                }
            }
        }
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

    /** Permet de résoudre les parents Spansh ({@link Parent#getId64()}) vers un {@link ParentBody#bodyID} journal. */
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
                .starType(StarType.fromString(starType))
                .stellarMass(valueOrZero(br.getSolarMasses()))
                .build();
    }

    private static PlaneteDetail toPlanet(
            String systemName, BodyResult br, Map<Long, Integer> id64ToBodyId, Map<String, BodyResult> nameIndex) {
        String planetClassStr = fallbackIfBlank(br.getSubtype(), "Unknown", "Unknown");
        return PlaneteDetail.builder()
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
                .planetClass(BodyType.fromString(planetClassStr))
                .massEM(br.getEarthMasses())
                .temperature(br.getSurfaceTemperature())
                .pressureAtm(br.getSurfacePressure())
                .gravityG(br.getGravity())
                .landable(Boolean.TRUE.equals(br.getIsLandable()))
                .radius(valueOrZero(br.getRadius()))
                .terraformable(isTerraformable(br.getTerraformingState()))
                .build();
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
        return normalized.contains("terraform");
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

    /**
     * Normalise plusieurs variantes de payload vers le shape attendu: { "name": "...", "bodies": [...] }.
     */
    private static ObjectNode normalize(JsonNode root, String fallbackSystemName) {
        ObjectNode out = MAPPER.createObjectNode();
        if (root == null || root.isNull()) {
            out.put("name", fallbackName(fallbackSystemName));
            out.set("bodies", MAPPER.createArrayNode());
            return out;
        }

        JsonNode directName = root.get("name");
        JsonNode directBodies = root.get("bodies");
        if (directName != null && directBodies != null && directBodies.isArray()) {
            out.put("name", directName.asText(fallbackName(fallbackSystemName)));
            out.set("bodies", directBodies);
            return out;
        }

        JsonNode systemNode = root.get("system");
        if (systemNode != null && systemNode.isObject()) {
            String name = textOrNull(systemNode.get("name"));
            if (name == null || name.isBlank()) {
                name = textOrNull(root.get("name"));
            }
            out.put("name", name != null && !name.isBlank() ? name : fallbackName(fallbackSystemName));
            JsonNode nestedBodies = systemNode.get("bodies");
            if (nestedBodies != null && nestedBodies.isArray()) {
                out.set("bodies", nestedBodies);
            } else if (directBodies != null && directBodies.isArray()) {
                out.set("bodies", directBodies);
            } else {
                out.set("bodies", MAPPER.createArrayNode());
            }
            return out;
        }

        out.put("name", textOrNull(directName) != null ? directName.asText() : fallbackName(fallbackSystemName));
        out.set("bodies", directBodies instanceof ArrayNode ? directBodies : MAPPER.createArrayNode());
        return out;
    }

    private static String fallbackName(String fallbackSystemName) {
        return fallbackSystemName != null && !fallbackSystemName.isBlank() ? fallbackSystemName : "Unknown";
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String v = node.asText(null);
        return v != null && !v.isBlank() ? v : null;
    }
}
