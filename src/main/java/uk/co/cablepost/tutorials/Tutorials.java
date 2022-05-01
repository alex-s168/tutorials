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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Tutorials implements ModInitializer {

    public static final String MOD_ID = "tutorials";

    public static Map<Identifier, Map<String, Tutorial>> tutorials = new HashMap<>();

    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public void reload(ResourceManager manager) {
                        Gson GSON = new GsonBuilder().create();
                        tutorials.clear();

                        for(Identifier id : manager.findResources("tutorials", path -> path.endsWith(".json"))) {
                            try(InputStream stream = manager.getResource(id).getInputStream()) {
                                String str = IOUtils.toString(stream, StandardCharsets.UTF_8);
                                Tutorial tut = GSON.fromJson(str, Tutorial.class);

                                String[] pathParts = id.getPath().replace(".json", "").split("/");

                                Identifier identifier = new Identifier(pathParts[1], pathParts[2]);

                                if(!tutorials.containsKey(identifier)){
                                    tutorials.put(identifier, new HashMap<>());
                                }

                                Map<String, Tutorial> itemTuts = tutorials.get(identifier);

                                itemTuts.put(pathParts[3], tut);
                            } catch(Exception e) {
                                //TUTORIAL_LOG.error("Error occurred while loading resource json " + id.toString(), e);
                                //TODO - set tutorial to error
                            }
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
