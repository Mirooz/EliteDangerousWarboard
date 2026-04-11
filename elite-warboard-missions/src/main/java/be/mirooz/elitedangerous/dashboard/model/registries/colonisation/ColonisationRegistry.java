package be.mirooz.elitedangerous.dashboard.model.registries.colonisation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Synchronized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dernière état connu des événements de colonisation (revendication, balise, dépôt de construction).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ColonisationRegistry {

    private static final ColonisationRegistry INSTANCE = new ColonisationRegistry();

    private volatile String claimedStarSystem;
    private volatile long claimedSystemAddress;
    private volatile String beaconDeployedTimestamp;

    private volatile long constructionMarketId;
    private volatile double constructionProgress;
    private volatile ConstructionStatus constructionStatus = ConstructionStatus.IN_PROGRESS;

    @Getter(AccessLevel.NONE)
    private volatile List<ConstructionResource> constructionResources = List.of();

    public static ColonisationRegistry getInstance() {
        return INSTANCE;
    }

    @Synchronized
    public void recordClaim(String starSystem, long systemAddress) {
        this.claimedStarSystem = starSystem;
        this.claimedSystemAddress = systemAddress;
    }

    @Synchronized
    public void recordBeaconDeployed(String timestamp) {
        this.beaconDeployedTimestamp = timestamp;
    }

    @Synchronized
    public void updateConstructionDepot(long marketId, double progress, ConstructionStatus status,
                                        List<ConstructionResource> resources) {
        this.constructionMarketId = marketId;
        this.constructionProgress = progress;
        this.constructionStatus = status != null ? status : ConstructionStatus.IN_PROGRESS;
        this.constructionResources = resources == null ? List.of() : List.copyOf(resources);
    }

    @Synchronized
    public List<ConstructionResource> getConstructionResources() {
        return Collections.unmodifiableList(new ArrayList<>(constructionResources));
    }
}
