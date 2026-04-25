package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteTargetRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;

public class NavRouteTargetStore extends SnapshotJsonStore<NavRouteTargetStore.Payload> {

    public NavRouteTargetStore(Path file) {
        super(
                "nav-route-target",
                file,
                PolymorphicPersistenceMapper.createSimple(),
                Payload.class,
                () -> new Payload(NavRouteTargetRegistry.getInstance().getRemainingJumpsInRoute()),
                NavRouteTargetStore::restoreSnapshot
        );
    }

    private static void restoreSnapshot(Payload payload) {
        if (payload != null) {
            NavRouteTargetRegistry.getInstance().setRemainingJumpsInRoute(payload.remainingJumpsInRoute);
        }
    }

    static class Payload {
        @JsonProperty
        public int remainingJumpsInRoute;

        public Payload() {}

        public Payload(int value) {
            this.remainingJumpsInRoute = value;
        }
    }
}
