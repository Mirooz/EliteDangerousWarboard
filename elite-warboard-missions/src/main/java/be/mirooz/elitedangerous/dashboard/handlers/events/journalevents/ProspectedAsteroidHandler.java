package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ProspectedAsteroidHandler implements JournalEventHandler {
    private final MiningService miningService = MiningService.getInstance();

    @Override
    public String getEventType() {
        return "ProspectedAsteroid";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            ProspectedAsteroid prospectedAsteroid = parseProspectedAsteroid(jsonNode);
            System.out.println("ProspectedAsteroid parsed: core " + prospectedAsteroid.getMotherlodeMaterial() +
                             " (Content: " + prospectedAsteroid.getMaterials() + ", Remaining: " +
                             prospectedAsteroid.getRemaining() + "%)");
            Optional<Mineral> mineral = MineralFactory.fromCoreMineralName(prospectedAsteroid.getMotherlodeMaterial());
            mineral.ifPresent(m -> {
                if (m instanceof MineralType coreMineral) {
                    prospectedAsteroid.setCoreMineral(coreMineral);
                }
            });

            miningService.registerProspectedAsteroid(prospectedAsteroid);
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de ProspectedAsteroid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ProspectedAsteroid parseProspectedAsteroid(JsonNode jsonNode) {
        ProspectedAsteroid prospectedAsteroid = new ProspectedAsteroid();

        // Fonctions utilitaires locales
        Function<String, String> getText = field -> jsonNode.has(field) && !jsonNode.get(field).isNull() ? jsonNode.get(field).asText() : null;
        Function<String, Double> getDouble = field -> jsonNode.has(field) && !jsonNode.get(field).isNull() ? jsonNode.get(field).asDouble() : null;

        // Champs de base
        prospectedAsteroid.setTimestamp(getText.apply("timestamp"));
        prospectedAsteroid.setEvent(getText.apply("event"));
        prospectedAsteroid.setMotherlodeMaterial(getText.apply("MotherlodeMaterial"));
        prospectedAsteroid.setContent(getText.apply("Content"));
        prospectedAsteroid.setContentLocalised(getText.apply("Content_Localised"));
        if (getDouble.apply("Remaining") != null) prospectedAsteroid.setRemaining(getDouble.apply("Remaining"));

        // Parse materials
        if (jsonNode.has("Materials") && jsonNode.get("Materials").isArray()) {
            JsonNode materialsNode = jsonNode.get("Materials");
            List<ProspectedAsteroid.Material> materials = new ArrayList<>();

            for (JsonNode materialNode : materialsNode) {
                ProspectedAsteroid.Material material = parseMaterial(materialNode);
                materials.add(material);
            }

            prospectedAsteroid.setMaterials(materials);
        }

        return prospectedAsteroid;
    }

    private ProspectedAsteroid.Material parseMaterial(JsonNode materialNode) {
        ProspectedAsteroid.Material material = new ProspectedAsteroid.Material();

        // Fonctions utilitaires locales
        Function<String, String> getText = field -> materialNode.has(field) && !materialNode.get(field).isNull() ? materialNode.get(field).asText() : null;
        Function<String, Double> getDouble = field -> materialNode.has(field) && !materialNode.get(field).isNull() ? materialNode.get(field).asDouble() : null;

        // Champs
        String rawName = getText.apply("Name");

        if (rawName != null) {
            MineralFactory.fromCargoJsonName(rawName).ifPresent(
                    m -> {
                        if (m instanceof MineralType mt) {
                            material.setMineral(mt);
                        }
                    });
            if (material.getMineral() == null) {
                material.setUnregisteredName(rawName);
            }
        }

        material.setNameLocalised(getText.apply("Name_Localised"));
        if (getDouble.apply("Proportion") != null) material.setProportion(getDouble.apply("Proportion"));

        return material;
    }

}

