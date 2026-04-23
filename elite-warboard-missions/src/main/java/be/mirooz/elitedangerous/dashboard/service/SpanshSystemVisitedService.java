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

    public SpanshSearchResponseDTO fetchSpanshBodiesSearch(String systemName) throws IOException {
        if (systemName == null || systemName.isBlank()) {
            throw new IOException("System name is blank");
        }
        String key = systemName.trim();
        try {
            return spanshFacade.searchSpanshBodiesBySystem(key);
        } catch (Exception e) {
            throw new IOException("Spansh bodies/search failed for system: " + key, e);
        }
    }

    public SystemVisited fetchSystemVisited(String systemName) throws IOException {
        if (systemName == null || systemName.isBlank()) {
            throw new IOException("System name is blank");
        }
        String key = systemName.trim();
        SystemVisited cached = systemVisitedByName.get(key);
        if (cached != null) {
            LOG.info("Spansh: pas d'appel réseau (cache mémoire) pour le système « {} »", key);
            return cached;
        }
        SpanshSearchResponseDTO dto = fetchSpanshBodiesSearch(key);
        SystemVisited fresh = SpanshSystemVisitedMapper.toSystemVisited(dto, key);
        SystemVisited previous = systemVisitedByName.putIfAbsent(key, fresh);
        return previous != null ? previous : fresh;
    }
}
