package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteTargetRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NavRouteTargetStore implements RegistryStore {

    private final Path file;
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.createSimple();

    public NavRouteTargetStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "nav-route-target";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            mapper.writeValue(file.toFile(),
                    new Payload(NavRouteTargetRegistry.getInstance().getRemainingJumpsInRoute()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save nav-route target to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Payload payload = mapper.readValue(file.toFile(), Payload.class);
            if (payload != null) {
                NavRouteTargetRegistry.getInstance().setRemainingJumpsInRoute(payload.remainingJumpsInRoute);
            }
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load nav-route target from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete nav-route target file " + file, e);
        }
    }

    private static class Payload {
        @JsonProperty
        public int remainingJumpsInRoute;

        public Payload() {}
        public Payload(int value) { this.remainingJumpsInRoute = value; }
    }
}
