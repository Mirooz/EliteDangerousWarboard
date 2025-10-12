package be.mirooz.elitedangerous.lib.inara.model;

import be.mirooz.elitedangerous.lib.inara.model.minerals.CoreMineral;
import java.lang.reflect.Modifier;
import java.util.*;
import org.reflections.Reflections;

/**
 * Registre des minéraux de core mining disponibles
 */
public class CoreMineralRegistry {

    private static final Map<String, CoreMineral> MINERALS_BY_ID = new HashMap<>();
    private static final Map<String, CoreMineral> MINERALS_BY_NAME = new HashMap<>();

    static {
        initializeMinerals();
    }

    private static void initializeMinerals() {
        // 📌 Package contenant les classes de minéraux
        String basePackage = CoreMineral.class.getPackageName();

        Reflections reflections = new Reflections(basePackage);
        Set<Class<? extends CoreMineral>> mineralClasses = reflections.getSubTypesOf(CoreMineral.class);


        for (Class<? extends CoreMineral> clazz : mineralClasses) {
            // On ignore les classes abstraites ou interfaces
            if (Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
                continue;
            }

            try {
                CoreMineral mineral = clazz.getDeclaredConstructor().newInstance();
                MINERALS_BY_ID.put(mineral.getInaraId(), mineral);
                MINERALS_BY_NAME.put(mineral.getInaraName(), mineral);
            } catch (Exception e) {
                System.err.println("❌ Impossible d'initialiser le minéral : " + clazz.getName());
                e.printStackTrace();
            }
        }
    }

    public static Optional<CoreMineral> getById(String id) {
        return Optional.ofNullable(MINERALS_BY_ID.get(id));
    }

    public static Optional<CoreMineral> getByName(String name) {
        return Optional.ofNullable(MINERALS_BY_NAME.get(name));
    }

    public static String getNameById(String id) {
        return getById(id)
                .map(CoreMineral::getInaraName)
                .orElse(id);
    }

    public static boolean isCoreMiningMineral(String id) {
        return MINERALS_BY_ID.containsKey(id);
    }

    public static Map<String, CoreMineral> getAllMinerals() {
        return new HashMap<>(MINERALS_BY_ID);
    }
    
    /**
     * Retourne l'ID d'un minéral à partir de son nom
     * @param name Le nom du minéral
     * @return L'ID du minéral ou null si non trouvé
     */
    public static String getIdByName(String name) {
        return getByName(name)
                .map(CoreMineral::getInaraId)
                .orElse(null);
    }
}
