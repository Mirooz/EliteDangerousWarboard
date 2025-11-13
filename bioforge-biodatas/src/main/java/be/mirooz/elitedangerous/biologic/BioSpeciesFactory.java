package be.mirooz.elitedangerous.biologic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.InputStream;
import java.util.*;

/**
 * Factory utility for creating BioSpecies from JSON statistical data.
 * Reads JSON from resources and creates BioSpecies based on histogram data.
 */
public class BioSpeciesFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reads a JSON file from resources and creates BioSpecies variants from it.
     * Each entry in the JSON represents a variant with its histogram data.
     *
     * @param colonyRangeMeters Colony range in meters
     * @return List of BioSpecies instances created from the JSON data
     */
    public static List<BioSpecies> createFromJsonResource(
            InputStream inputStream,
            double colonyRangeMeters) {

        List<BioSpecies> speciesList = new ArrayList<>();

        try {


            JsonNode rootNode = objectMapper.readTree(inputStream);

            // Iterate through each variant entry in the JSON
            rootNode.fields().forEachRemaining(entry -> {
                String id = entry.getKey();
                JsonNode variantNode = entry.getValue();
                try {
                    BioSpecies species = createBioSpeciesFromJsonNode(
                            colonyRangeMeters,
                            variantNode,
                            id
                    );

                    if (species != null) {
                        speciesList.add(species);
                    }
                } catch (Exception e) {
                    System.err.println("Error creating species for id " + id + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        return speciesList;
    }

    /**
     * Creates a BioSpecies from a JSON node representing one variant.
     */
    private static BioSpecies createBioSpeciesFromJsonNode(
            double colonyRangeMeters,
            JsonNode node,
            String id) {

        // Extract variant name and material
        String baseName = extractBaseName(node);
        String specieName = extractSpecieName(node);
        String color = extractVariantName(node);
        String colorConditionName = extractSurfaceMaterial(node);
        String fdevname = node.get("fdevname").asText();
        VariantMethods variantMethod;
        if (colorConditionName.equals("Tin") || colorConditionName.length() >= 4) {
            variantMethod = VariantMethods.SURFACE_MATERIALS;
        } else {
            variantMethod = VariantMethods.RADIANT_STAR;
        }


        // Extract histogram data
        HistogramData histogramData = extractHistogramData(node);


        long reward = node.get("reward").asLong();
        int count = node.get("count").asInt();

        // Create BioSpecies instance with histogram data
        return BioSpecies.builder()
                .name(baseName)
                .specieName(specieName)
                .color(color)
                .count(count)
                .fdevname(fdevname)
                .baseValue(reward)
                .firstLoggedValue(reward * 5)
                .colonyRangeMeters(colonyRangeMeters)
                .variantMethod(variantMethod)
                .colorConditionName(colorConditionName)
                .id(id)
                .histogramData(histogramData)
                .build();

    }

    /**
     * Extracts variant name from JSON node (e.g., "Bacterium Tela - Orange" -> "Orange")
     */
    private static String extractBaseName(JsonNode node) {
        if (node.has("name")) {
            String fullName = node.get("name").asText();
            if (fullName.contains(" ")) {
                return fullName.split(" ")[0];
            }
            return fullName;
        }
        return "Unknown";
    }

    private static String extractSpecieName(JsonNode node) {
        if (node.has("name")) {
            String fullName = node.get("name").asText();
            if (fullName.contains(" ")) {
                return fullName.split(" ")[1];
            }
            return fullName;
        }
        return "Unknown";
    }

    private static String extractVariantName(JsonNode node) {
        if (node.has("name")) {
            String fullName = node.get("name").asText();
            if (fullName.contains(" - ")) {
                return fullName.substring(fullName.lastIndexOf(" - ") + 3);
            }
            return fullName;
        }
        return "Unknown";
    }

    /**
     * Extracts surface material from JSON node (from materials array)
     */
    private static String extractSurfaceMaterial(JsonNode node) {
        try {
            return node.get("fdevname").asText().split("_")[4];
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Extracts all histogram data from JSON node
     */
    private static HistogramData extractHistogramData(JsonNode node) {
        HistogramData data = new HistogramData();
        int totalCount = node.get("count").asInt();
        if (node.has("histograms") && node.get("histograms").isObject()) {
            JsonNode histograms = node.get("histograms");

            // atmos_types: Map<AtmosphereType, Integer>
            if (histograms.has("atmos_types") && histograms.get("atmos_types").isObject()) {
                data.atmosTypes = new HashMap<>();
                histograms.get("atmos_types").fields().forEachRemaining(entry -> {
                    AtmosphereType atmosType = AtmosphereType.fromString(entry.getKey());
                    if (atmosType != null) {
                        JsonNode valueNode = entry.getValue();
                        Double count = (valueNode != null && !valueNode.isNull()) ? valueNode.asDouble() : null;
                        if (count != null) {
                            data.atmosTypes.put(atmosType,  count);
                        }
                    } else {
                        throw new RuntimeException("Unknown atmosphere type: " + entry.getKey());
                    }
                });
            }

            // body_types: Map<BodyType, Integer>
            if (histograms.has("body_types") && histograms.get("body_types").isObject()) {
                data.bodyTypes = new HashMap<>();
                histograms.get("body_types").fields().forEachRemaining(entry -> {
                    BodyType bodyType = BodyType.fromString(entry.getKey());
                    if (bodyType != null) {
                        JsonNode valueNode = entry.getValue();
                        Double count = (valueNode != null && !valueNode.isNull()) ? valueNode.asDouble() : null;
                        if (count != null) {
                            data.bodyTypes.put(bodyType, count);

                        }
                    } else {
                        throw new RuntimeException("Unknown atmosphere type: " + entry.getKey());
                    }
                });
            }

            // gravity: List<Bin> (min, max, value)
            if (histograms.has("gravity") && histograms.get("gravity").isArray()) {
                data.gravity = new ArrayList<>();
                histograms.get("gravity").forEach(bin -> {
                    Bin gravityBin = new Bin();
                    gravityBin.min = getDoubleOrNull(bin, "min");
                    gravityBin.max = getDoubleOrNull(bin, "max");
                    gravityBin.value = getDoubleOrNull(bin,"value");
                    if (gravityBin.value != 0) {
                        data.gravity.add(gravityBin);
                    }
                });
            }

            // pressure: List<Bin>
            if (histograms.has("pressure") && histograms.get("pressure").isArray()) {
                data.pressure = new ArrayList<>();
                histograms.get("pressure").forEach(bin -> {
                    Bin pressureBin = new Bin();
                    pressureBin.min = getDoubleOrNull(bin, "min");
                    pressureBin.max = getDoubleOrNull(bin, "max");
                    pressureBin.value = getDoubleOrNull(bin,"value");
                    if (pressureBin.value != 0) {
                        data.pressure.add(pressureBin);
                    }
                });
            }
            // temperature: List<Bin>
            if (histograms.has("temperature") && histograms.get("temperature").isArray()) {
                data.temperature = new ArrayList<>();
                histograms.get("temperature").forEach(bin -> {
                    Bin tempBin = new Bin();
                    tempBin.min = getDoubleOrNull(bin, "min");
                    tempBin.max = getDoubleOrNull(bin, "max");
                    tempBin.value = getDoubleOrNull(bin,"value");
                    if (tempBin.value != 0 ) {
                        data.temperature.add(tempBin);
                    }
                });
            }

            // volcanic_body_types: Map<VolcanicBodyType, Integer>
            if (histograms.has("volcanic_body_types") && histograms.get("volcanic_body_types").isObject()) {
                data.volcanicBodyTypes = new HashMap<>();
                histograms.get("volcanic_body_types").fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    VolcanicBodyType volcanicBodyType = parseVolcanicBodyType(key);
                    if (volcanicBodyType != null) {
                        JsonNode valueNode = entry.getValue();
                        Double count = (valueNode != null && !valueNode.isNull()) ? valueNode.asDouble() : null;
                        if (count != null) {
                            data.volcanicBodyTypes.put(volcanicBodyType, count);
                        }
                    } else {
                        throw new RuntimeException("Unknown volcanic body type: " + key);
                    }
                });
            }
        }

        return data;
    }


    /**
     * Parses a volcanic body type string (format: "Body Type - Volcanism Type")
     * and returns a VolcanicBodyType object.
     */
    private static VolcanicBodyType parseVolcanicBodyType(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        if (str.contains(" - ")) {
            String[] parts = str.split(" - ", 2);
            BodyType bodyType = BodyType.fromString(parts[0].trim());
            VolcanismType volcanismType = VolcanismType.fromString(parts[1].trim());

            if (bodyType != null && volcanismType != null) {
                return new VolcanicBodyType(bodyType, volcanismType);
            }
        }

        return null;
    }

    // 🔧 Méthodes utilitaires pour gérer les null proprement
    private static Double getDoubleOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asDouble() : 0;
    }


    /**
     * Data structure for storing histogram data with values/quantities.
     * Contains only: atmos_types, body_types, gravity, pressure, temperature, volcanic_body_types
     */
    @Data
    public static class HistogramData {
        // atmos_types: Map of atmosphere type -> count
        public Map<AtmosphereType, Double> atmosTypes;

        // body_types: Map of body type -> count
        public Map<BodyType, Double> bodyTypes;

        // gravity: List of bins (min, max, count)
        public List<Bin> gravity;

        // pressure: List of bins (min, max, count)
        public List<Bin> pressure;

        // temperature: List of bins (min, max, count)
        public List<Bin> temperature;

        // volcanic_body_types: Map of (body type, volcanism type) -> count
        public Map<VolcanicBodyType, Double> volcanicBodyTypes;
    }

    /**
     * Represents a bin in a histogram with min, max, and value/count
     */
    public static class Bin {
        public Double min;
        public Double max;
        public Double value; // proportion
    }

    /**
     * Represents a combination of BodyType and VolcanismType.
     * Used as a key in the volcanicBodyTypes map.
     */
    @Data
    @AllArgsConstructor
    public static class VolcanicBodyType {
        private final BodyType bodyType;
        private final VolcanismType volcanismType;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VolcanicBodyType that = (VolcanicBodyType) o;
            return bodyType == that.bodyType && volcanismType == that.volcanismType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(bodyType, volcanismType);
        }
    }
}
