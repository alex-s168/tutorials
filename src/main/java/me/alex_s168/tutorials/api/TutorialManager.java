package me.alex_s168.tutorials.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.cablepost.tutorials.TutorialObjectDeserializer;
import uk.co.cablepost.tutorials.TutorialsClient;

import java.util.*;

final public class TutorialManager {
    private static Gson GSON = new GsonBuilder()
            .registerTypeAdapter(TutorialObject.class, new TutorialObjectDeserializer())
            .registerTypeAdapter(TutorialInstruction.class, new TutorialInstruction.Deserializer())
            .create();

    private static Map<Identifier, TutorialObjectRenderKind> tutorialRenderers = new HashMap<>();

    public static void registerRender(Identifier id, TutorialObjectRenderKind kind) {
        tutorialRenderers.put(id, kind);
    }

    // will be cleaned
    private static Map<Identifier, Tutorial> tutorials = new HashMap<>();
    private static Map<Identifier, List<Tutorial.Parsed>> tutorialsByItems = new HashMap<>();

    public static Optional<TutorialObjectRenderKind> getRender(Identifier name) {
        return Optional.ofNullable(tutorialRenderers.get(name));
    }

    public static Optional<Tutorial> byName(Identifier tut) {
        return Optional.ofNullable(tutorials.get(tut));
    }

    public static List<Tutorial.Parsed> byItem(Identifier item) {
        return tutorialsByItems.getOrDefault(item, new ArrayList<>());
    }

    public static List<Tutorial.Parsed> byItem(Item item) {
        return byItem(Registry.ITEM.getId(item));
    }

    static {
        TutorialsClient.registerRenders(tutorialRenderers);
    }

    private static void registerTutorial(Identifier name, Tutorial tut) throws Exception {
        tutorials.put(name, tut);

        var p = tut.finishParse();
        for (String item : tut.show_for_items) {
            Identifier itemId = new Identifier(item);
            List<Tutorial.Parsed> itemTuts = tutorialsByItems.computeIfAbsent(itemId, (ignored) ->
                    new ArrayList<>());
            itemTuts.add(p);
        }
    }

    private static void registerTutorial(Identifier name, String json) {
        try {
            Tutorial tut = GSON.fromJson(json, Tutorial.class);
            registerTutorial(name, tut);
        } catch (Exception e) {
            String msg = "Error while loading tutorial '" + name + "': " + e;
            TutorialsClient.LOGGER.error(msg);
        }
    }

    private static List<TutorialProvider> providers = new ArrayList<>();

    public static void addSource(TutorialProvider prov) {
        providers.add(prov);
    }

    public static void reload(@NotNull ResourceManager resourceManager, @Nullable TutorialProvider thisReload) {
        TutorialsClient.LOGGER.info("Reloading Tutorials");

        TutorialsClient.lastResourceManager = resourceManager;

        tutorials.clear();
        tutorialsByItems.clear();

        var reg = new TutorialProvider.TutorialRegister() {
            @Override
            public void register(Identifier ident, Tutorial tutorial) {
                try {
                    registerTutorial(ident, tutorial);
                } catch (Exception e) {
                    String msg = "Error while loading tutorial '" + ident + "': " + e;
                    TutorialsClient.LOGGER.error(msg);
                }
            }

            @Override
            public void register(Identifier ident, String tutorial) {
                registerTutorial(ident, tutorial);
            }
        };

        for (TutorialProvider prov : providers) {
            prov.getTutorials(reg);
        }

        if (thisReload != null) {
            thisReload.getTutorials(reg);
        }

        TutorialsClient.lastResourceManager = null;
    }

    public static void reload(@NotNull ResourceManager resourceManager) {
        reload(resourceManager, null);
    }
}
