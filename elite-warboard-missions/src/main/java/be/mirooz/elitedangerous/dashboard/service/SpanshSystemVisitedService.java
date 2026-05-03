package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.backend.generated.model.SpanshSearchResponseDTO;
import be.mirooz.elitedangerous.backend.spansh.SpanshFacade;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.service.mapping.SpanshSystemVisitedMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Service d'accès Spansh ({@code GET /api/spansh/bodies/search}) avec mapping vers SystemVisited.
 */
public final class SpanshSystemVisitedService {

    private static final Logger LOG = LoggerFactory.getLogger(SpanshSystemVisitedService.class);

    private static final SpanshSystemVisitedService INSTANCE = new SpanshSystemVisitedService();

    private final SpanshFacade spanshFacade = SpanshFacade.getInstance();
    private final ConcurrentHashMap<String, SystemVisited> systemVisitedByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<SystemVisited>> inFlightLoadsByName = new ConcurrentHashMap<>();

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
        CompletableFuture<SystemVisited> currentLoad = new CompletableFuture<>();
        CompletableFuture<SystemVisited> existingLoad = inFlightLoadsByName.putIfAbsent(key, currentLoad);
        if (existingLoad != null) {
            return waitInFlight(existingLoad, key, systemId64);
        }
        try {
            SpanshSearchResponseDTO dto = fetchSpanshBodiesSearch(hasName ? systemName : null, hasId ? systemId64 : null);
            SystemVisited fresh = SpanshSystemVisitedMapper.toSystemVisited(dto, hasName ? key : null);
            SystemVisited previous = systemVisitedByName.putIfAbsent(key, fresh);
            SystemVisited resolved = previous != null ? previous : fresh;
            currentLoad.complete(resolved);
            return resolved;
        } catch (Exception e) {
            currentLoad.completeExceptionally(e);
            if (e instanceof IOException io) {
                throw io;
            }
            throw new IOException("Spansh fetch failed for system: " + key + " (id64=" + systemId64 + ")", e);
        } finally {
            inFlightLoadsByName.remove(key, currentLoad);
        }
    }

    private static SystemVisited waitInFlight(CompletableFuture<SystemVisited> inFlight, String key, Long systemId64)
            throws IOException {
        try {
            return inFlight.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting Spansh fetch for system: " + key + " (id64=" + systemId64 + ")", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            throw new IOException("Spansh fetch failed for system: " + key + " (id64=" + systemId64 + ")", cause);
        }
    }
}
