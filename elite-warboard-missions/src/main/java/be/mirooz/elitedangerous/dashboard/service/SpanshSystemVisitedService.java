package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.backend.generated.model.SpanshSearchResponseDTO;
import be.mirooz.elitedangerous.backend.spansh.SpanshFacade;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.service.mapping.SpanshSystemVisitedMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service d'accès Spansh ({@code GET /api/spansh/bodies/search}) avec mapping vers SystemVisited.
 */
public final class SpanshSystemVisitedService {

    private static final Logger LOG = LoggerFactory.getLogger(SpanshSystemVisitedService.class);

    private static final SpanshSystemVisitedService INSTANCE = new SpanshSystemVisitedService();

    private final SpanshFacade spanshFacade = SpanshFacade.getInstance();
    private final ConcurrentHashMap<String, SystemVisited> systemVisitedByName = new ConcurrentHashMap<>();

    private SpanshSystemVisitedService() {
    }

    public static SpanshSystemVisitedService getInstance() {
        return INSTANCE;
    }


    public SpanshSearchResponseDTO fetchSpanshBodiesSearch(String systemName, Long systemId64) throws IOException {
        boolean hasName = systemName != null && !systemName.isBlank();
        boolean hasId = systemId64 != null && systemId64 != 0L;
        if (!hasName && !hasId) {
            throw new IOException("System name and id are blank");
        }
        String key = hasName ? systemName.trim() : null;
        try {
            return spanshFacade.searchSpanshBodiesBySystem(key, hasId ? systemId64 : null);
        } catch (Exception e) {
            throw new IOException("Spansh bodies/search failed for system: " + key + " (id64=" + systemId64 + ")", e);
        }
    }

    public SystemVisited fetchSystemVisited(String systemName, Long systemId64) throws IOException {
        boolean hasName = systemName != null && !systemName.isBlank();
        boolean hasId = systemId64 != null && systemId64 != 0L;
        if (!hasName && !hasId) {
            throw new IOException("System name and id are blank");
        }
        String key = hasName ? systemName.trim() : ("#" + systemId64);
        SystemVisited cached = systemVisitedByName.get(key);
        if (cached != null) {
            return cached;
        }
        SpanshSearchResponseDTO dto = fetchSpanshBodiesSearch(hasName ? systemName : null, hasId ? systemId64 : null);
        SystemVisited fresh = SpanshSystemVisitedMapper.toSystemVisited(dto, hasName ? key : null);
        SystemVisited previous = systemVisitedByName.putIfAbsent(key, fresh);
        return previous != null ? previous : fresh;
    }
}
