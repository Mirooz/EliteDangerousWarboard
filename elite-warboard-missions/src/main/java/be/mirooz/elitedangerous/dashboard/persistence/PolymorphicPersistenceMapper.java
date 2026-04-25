package be.mirooz.elitedangerous.dashboard.persistence;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * {@link ObjectMapper} pré-configuré pour les registries lourds de la Phase 3.
 *
 * <p>Polymorphisme explicite par annotations {@code @JsonTypeInfo(use = NAME)} et mixins
 * ({@link PersistencePolymorphicMixins}) au lieu du default-typing global.</p>
 */
public final class PolymorphicPersistenceMapper {

    private PolymorphicPersistenceMapper() {}

    public static ObjectMapper create() {
        ObjectMapper mapper = baseMapper()
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // Accès direct aux champs (y compris privés/finals) : requis pour nos POJOs Lombok
        // dont certains champs sont finaux sans setter explicite (ex. ColonisationArchitectSystem
        // #sitesByMarketId).
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        // Visibilité des constructeurs : Lombok @SuperBuilder génère un constructeur no-arg
        // en accès "package-private"/protected (cf. ACelesteBody / StarDetail / PlaneteDetail).
        // Sans ANY, Jackson ne le voit pas et lève
        // "Cannot construct instance of X (no Creators, like default constructor, exist)".
        // ColonisationArchitectSystem n'a qu'un constructeur (String starSystem) : combiné au
        // flag -parameters du compilateur, Jackson peut le détecter comme creator property-based.
        mapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        PersistencePolymorphicMixins.registerOn(mapper);
        return mapper;
    }

    private static ObjectMapper baseMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new DashboardPersistenceJacksonModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
