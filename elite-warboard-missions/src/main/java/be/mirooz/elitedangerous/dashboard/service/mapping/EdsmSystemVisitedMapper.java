package be.mirooz.elitedangerous.dashboard.service.mapping;

import be.mirooz.elitedangerous.backend.generated.model.EdsmBodiesResponse;
import be.mirooz.elitedangerous.backend.generated.model.EdsmBodyDto;
import be.mirooz.elitedangerous.backend.generated.model.EdsmParentRefDto;
import be.mirooz.elitedangerous.biologic.BodyType;
import be.mirooz.elitedangerous.biologic.StarType;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.ParentBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.StarDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Convertit une réponse EDSM vers le modèle interne SystemVisited.
 */
public final class EdsmSystemVisitedMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EdsmSystemVisitedMapper() {
    }

    public static SystemVisited toSystemVisited(EdsmBodiesResponse response, String fallbackSystemName) {
        String systemName = fallbackIfBlank(response != null ? response.getName() : null, fallbackSystemName, "Unknown");
        List<EdsmBodyDto> bodies = response != null && response.getBodies() != null ? response.getBodies() : List.of();
        String visitTimestamp = OffsetDateTime.now().toString();

        return SystemVisited.builder()
                .systemName(systemName)
                .firstDiscover(false)
                .firstVisitedTime(visitTimestamp)
                .lastVisitedTime(visitTimestamp)
                .numberVisited(1)
                .sold(false)
                .celesteBodies(toCelesteBodies(systemName, bodies))
                .build();
    }

    private static Collection<ACelesteBody> toCelesteBodies(String systemName, List<EdsmBodyDto> bodies) {
        Collection<ACelesteBody> mapped = new ArrayList<>();
        for (EdsmBodyDto body : bodies) {
            if (body == null) {
                continue;
            }
            mapped.add(isStar(body) ? toStar(systemName, body) : toPlanet(systemName, body));
        }
        return mapped;
    }

    private static boolean isStar(EdsmBodyDto body) {
        return body.getType() != null && "Star".equalsIgnoreCase(body.getType());
    }

    private static StarDetail toStar(String systemName, EdsmBodyDto body) {
        String starType = fallbackIfBlank(body.getSubType(), "Unknown", "Unknown");
        return StarDetail.builder()
                .timestamp(body.getUpdateTime())
                .jsonNode(toJsonNode(body))
                .bodyName(fallbackIfBlank(body.getName(), "Unknown body", "Unknown body"))
                .starSystem(systemName)
                .lsDistance(body.getDistanceToArrival())
                .systemAddress(valueOrZero(body.getId64()))
                .bodyID(valueOrZero(body.getBodyId()))
                .parents(mapParents(body))
                .wasDiscovered(true)
                .mapped(false)
                .rings(body.getRings() != null && !body.getRings().isEmpty())
                .starTypeString(starType)
                .starType(StarType.fromString(starType))
                .stellarMass(valueOrZero(body.getSolarMasses()))
                .build();
    }

    private static PlaneteDetail toPlanet(String systemName, EdsmBodyDto body) {
        return PlaneteDetail.builder()
                .timestamp(body.getUpdateTime())
                .jsonNode(toJsonNode(body))
                .bodyName(fallbackIfBlank(body.getName(), "Unknown body", "Unknown body"))
                .starSystem(systemName)
                .lsDistance(body.getDistanceToArrival())
                .systemAddress(valueOrZero(body.getId64()))
                .bodyID(valueOrZero(body.getBodyId()))
                .parents(mapParents(body))
                .wasDiscovered(true)
                .mapped(false)
                .rings(body.getRings() != null && !body.getRings().isEmpty())
                .planetClass(BodyType.fromString(body.getSubType()))
                .massEM(body.getEarthMasses())
                .temperature(body.getSurfaceTemperature())
                .pressureAtm(body.getSurfacePressure())
                .gravityG(body.getGravity())
                .landable(Boolean.TRUE.equals(body.getIsLandable()))
                .radius(valueOrZero(body.getRadius()))
                .terraformable(isTerraformable(body.getTerraformingState()))
                .build();
    }

    private static List<ParentBody> mapParents(EdsmBodyDto body) {
        List<ParentBody> mapped = new ArrayList<>();
        if (body == null || body.getParents() == null) {
            return mapped;
        }
        for (EdsmParentRefDto parentRef : body.getParents()) {
            if (parentRef == null || parentRef.getRefs() == null) {
                continue;
            }
            for (Map.Entry<String, Integer> ref : parentRef.getRefs().entrySet()) {
                if (ref.getKey() == null || ref.getKey().isBlank()) {
                    continue;
                }
                mapped.add(ParentBody.builder()
                        .type(ref.getKey())
                        .bodyID(valueOrZero(ref.getValue()))
                        .build());
            }
        }
        return mapped;
    }

    private static JsonNode toJsonNode(EdsmBodyDto body) {
        JsonNode node = MAPPER.valueToTree(body);
        if (node instanceof ObjectNode objectNode) {
            objectNode.put("_source", "EDSM");
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
        // EDSM peut renvoyer des libellés comme "Terraformable" ou "Candidate for terraforming".
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
}
