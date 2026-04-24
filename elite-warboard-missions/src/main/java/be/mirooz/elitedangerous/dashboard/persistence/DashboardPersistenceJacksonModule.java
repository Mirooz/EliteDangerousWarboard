package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.biologic.BioSpeciesFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

/**
 * Module Jackson qui couvre les clés de map "exotiques" du graphe sérialisé.
 *
 * <p>Cas concret : {@link be.mirooz.elitedangerous.biologic.BioSpeciesFactory.HistogramData}
 * contient une {@code Map<VolcanicBodyType, Double>} où {@code VolcanicBodyType} est un POJO
 * composite (pas enum, pas String). Jackson ne sait ni la sérialiser ni la désérialiser comme
 * clé de map sans aide explicite.</p>
 *
 * <p>Même si le champ {@code histogramData} est marqué {@code @JsonIgnore} côté
 * {@link be.mirooz.elitedangerous.biologic.BioSpecies}, l'activation du default-typing
 * {@code NON_FINAL} dans {@link PolymorphicPersistenceMapper} force Jackson à résoudre de
 * manière eager les deserializers du graphe complet pendant la construction de son cache —
 * y compris les champs ignorés côté données. Sans ces (de)serializers, Jackson lève une
 * {@code InvalidDefinitionException: Cannot find a (Map) Key deserializer for type
 * VolcanicBodyType} avant même de commencer la lecture du JSON.</p>
 *
 * <p>Stratégie : (de)sérialisation "no-op" — on écrit une chaîne vide et on relit {@code null}.
 * Ces clés ne portent aucune info métier utile à la persistance.</p>
 */
public final class DashboardPersistenceJacksonModule extends SimpleModule {

    public DashboardPersistenceJacksonModule() {
        super("DashboardPersistenceJacksonModule");
        addKeyDeserializer(BioSpeciesFactory.VolcanicBodyType.class, new NoOpKeyDeserializer());
        addKeySerializer(BioSpeciesFactory.VolcanicBodyType.class, new NoOpKeySerializer());
    }

    private static final class NoOpKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return null;
        }
    }

    private static final class NoOpKeySerializer extends JsonSerializer<BioSpeciesFactory.VolcanicBodyType> {
        @Override
        public void serialize(BioSpeciesFactory.VolcanicBodyType value, JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            gen.writeFieldName("");
        }
    }
}
