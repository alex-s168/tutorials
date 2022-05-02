package uk.co.cablepost.tutorials;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Tutorials implements ModInitializer {

    public static final String MOD_ID = "tutorials";

    public static Map<Identifier, Map<String, Tutorial>> tutorials = new HashMap<>();

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public void reload(ResourceManager manager) {
                        Gson GSON = new GsonBuilder().create();
                        tutorials.clear();

                        for(Identifier id : manager.findResources("tutorials", path -> path.endsWith(".json"))) {
                            String[] pathParts = id.getPath().replace(".json", "").split("/");
                            Identifier identifier = new Identifier(pathParts[1], pathParts[2]);

                            Tutorial tut;

                            try(InputStream stream = manager.getResource(id).getInputStream()) {
                                String str = IOUtils.toString(stream, StandardCharsets.UTF_8);
                                tut = GSON.fromJson(str, Tutorial.class);
                            } catch(Exception e) {
                                String msg = "Error while loading tutorial '" + id.getPath() + "': " + e;
                                LOGGER.error(msg);
                                tut = new Tutorial();
                                tut.display_name = "Tutorial that failed to load :(";
                                tut.error_message = msg;
                            }

                            if(!tutorials.containsKey(identifier)){
                                tutorials.put(identifier, new HashMap<>());
                            }

                            Map<String, Tutorial> itemTuts = tutorials.get(identifier);
                            itemTuts.put(pathParts[3], tut);
                        }
                    }

                    @Override
                    public Identifier getFabricId() {
                        return new Identifier(MOD_ID, "tutorials");
                    }
                }
        );
    }
}
