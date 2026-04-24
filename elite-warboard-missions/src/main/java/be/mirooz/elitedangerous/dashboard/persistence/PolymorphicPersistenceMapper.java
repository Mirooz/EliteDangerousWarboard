package be.mirooz.elitedangerous.dashboard.persistence;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * {@link ObjectMapper} pré-configuré pour les registries lourds de la Phase 3.
 *
 * <p>Activation du default-typing NON_FINAL : chaque objet sérialisé reçoit un {@code @class}
 * avec le FQN de son runtime type. Ça permet de persister et restaurer des champs typés
 * via une interface ({@code Mineral}, {@code ICommodity}) ou une classe abstraite
 * ({@code ACelesteBody}) sans annoter chaque POJO domaine.</p>
 *
 * <p>Le validator de polymorphisme est limité aux classes du projet
 * ({@code be.mirooz.elitedangerous}) + {@code java.util} pour couvrir les collections,
 * afin d'éviter les dangers classiques de default-typing global sur du JSON non-trust.</p>
 */
public final class PolymorphicPersistenceMapper {

    private PolymorphicPersistenceMapper() {}

    public static ObjectMapper create() {
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("be.mirooz.elitedangerous.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .build();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                // Enregistre les noms de paramètres de constructeurs → Jackson utilise
                // directement les constructeurs all-args de @Value / @AllArgsConstructor
                // (ColonisationConstruction, ConstructionResource, Scan, etc.).
                .registerModule(new ParameterNamesModule())
                // (De)sérialiseurs pour les clés de map "exotiques" (cf. VolcanicBodyType
                // dans BioSpeciesFactory.HistogramData). Requis même si le champ porteur est
                // @JsonIgnore, car le default-typing NON_FINAL résout les deserializers
                // eagerly pour tout le graphe de types.
                .registerModule(new DashboardPersistenceJacksonModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
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
        mapper.activateDefaultTyping(validator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    /**
     * Mapper "simple" pour stores qui ne nécessitent pas de default-typing polymorphique
     * (Mission, ShipTarget, etc.) mais partagent les mêmes besoins robustesse :
     * tolérance aux champs inconnus (ex. getters calculés Lombok {@code isActive()} →
     * propriété {@code "active"}), support {@code java.time}, indentation.
     */
    public static ObjectMapper createSimple() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new DashboardPersistenceJacksonModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
