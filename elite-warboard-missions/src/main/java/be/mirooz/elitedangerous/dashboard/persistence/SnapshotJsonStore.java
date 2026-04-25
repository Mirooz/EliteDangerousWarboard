package be.mirooz.elitedangerous.dashboard.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Store JSON générique basé sur un snapshot DTO dédié.
 *
 * <p>Le snapshot capture un état sérialisable stable, tandis que {@code loader}
 * applique explicitement la restauration sur le runtime registry.</p>
 *
 * <p>Utilise soit {@link Class}{@code <T>} (racine JSON = objet), soit
 * {@link TypeReference} pour des racines paramétrées (ex. {@code Map}, {@code List}).</p>
 */
public class SnapshotJsonStore<T> implements RegistryStore {

    private final String storeName;
    private final Path file;
    private final ObjectMapper mapper;
    private final Class<T> snapshotType;
    private final TypeReference<T> typeRef;
    private final Supplier<T> snapshotSupplier;
    private final Consumer<T> loader;

    public SnapshotJsonStore(String storeName,
                             Path file,
                             ObjectMapper mapper,
                             Class<T> snapshotType,
                             Supplier<T> snapshotSupplier,
                             Consumer<T> loader) {
        this(storeName, file, mapper, snapshotType, null, snapshotSupplier, loader);
    }

    public SnapshotJsonStore(String storeName,
                             Path file,
                             ObjectMapper mapper,
                             TypeReference<T> typeRef,
                             Supplier<T> snapshotSupplier,
                             Consumer<T> loader) {
        this(storeName, file, mapper, null, typeRef, snapshotSupplier, loader);
    }

    private SnapshotJsonStore(String storeName,
                              Path file,
                              ObjectMapper mapper,
                              Class<T> snapshotType,
                              TypeReference<T> typeRef,
                              Supplier<T> snapshotSupplier,
                              Consumer<T> loader) {
        if (snapshotType == null && typeRef == null) {
            throw new IllegalArgumentException("Either snapshotType or typeRef must be set");
        }
        if (snapshotType != null && typeRef != null) {
            throw new IllegalArgumentException("Use either snapshotType or typeRef, not both");
        }
        this.storeName = storeName;
        this.file = file;
        this.mapper = mapper;
        this.snapshotType = snapshotType;
        this.typeRef = typeRef;
        this.snapshotSupplier = snapshotSupplier;
        this.loader = loader;
    }

    @Override
    public String name() {
        return storeName;
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            mapper.writeValue(file.toFile(), snapshotSupplier.get());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save " + storeName + " to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            T snapshot = typeRef != null
                    ? mapper.readValue(file.toFile(), typeRef)
                    : mapper.readValue(file.toFile(), snapshotType);
            loader.accept(snapshot);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + storeName + " from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete " + storeName + " file " + file, e);
        }
    }
}
