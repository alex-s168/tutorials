package uk.co.cablepost.tutorials.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.cablepost.tutorials.Tutorial;
import uk.co.cablepost.tutorials.client.screen.TutorialScreen;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class TutorialsClient implements ClientModInitializer {

    public static final String MOD_ID = "tutorials";

    public static Map<Identifier, Map<String, Tutorial>> tutorials = new HashMap<>();

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyBinding keyBinding;
    public static Item mouseOverItem;
    public static int timeSinceMouseOverItem;
    public static int timeSinceMouseTextInputFocus;

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public void reload(ResourceManager manager) {
                        Gson GSON = new GsonBuilder().create();
                        tutorials.clear();

                        for(Identifier id : manager.findResources("tutorials", path -> path.endsWith(".json"))) {
                            String[] pathParts = id.getPath().replace(".json", "").split("/");
                            Identifier identifier = new Identifier(id.getNamespace(), pathParts[1]);

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
                            itemTuts.put(pathParts[2], tut);
                        }
                    }

                    @Override
                    public Identifier getFabricId() {
                        return new Identifier(MOD_ID, "tutorials");
                    }
                }
        );

        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tutorials.open_tutorial", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_LEFT_ALT, // The keycode of the key
                "category.tutorials" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(timeSinceMouseOverItem < 10) {
                timeSinceMouseOverItem++;
            }
            if(timeSinceMouseTextInputFocus < 10) {
                timeSinceMouseTextInputFocus++;
            }

            while (keyBinding.wasPressed()) {
                try {
                    onTutorialKey();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static void onTutorialKey() {
        MinecraftClient client = MinecraftClient.getInstance();

        if(client.player == null){
            return;
        }

        if(timeSinceMouseTextInputFocus < 2){
            return;
        }

        if(client.currentScreen == null){
            ItemStack mainHand = client.player.getStackInHand(Hand.MAIN_HAND);
            if(mainHand != null && !mainHand.isEmpty()){
                openScreenFor(mainHand.getItem(), client);
                return;
            }

            ItemStack offHand = client.player.getStackInHand(Hand.OFF_HAND);
            if(offHand != null && !offHand.isEmpty()){
                openScreenFor(offHand.getItem(), client);
                return;
            }

            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 1.0f);
            return;
        }

        if(timeSinceMouseOverItem > 2){
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 1.0f);
            return;
        }

        openScreenFor(TutorialsClient.mouseOverItem, client);
    }

    private static void openScreenFor(Item item, MinecraftClient client) {
        if(client.player == null){
            return;
        }

        Identifier id = Registry.ITEM.getId(item);
        if(!TutorialsClient.tutorials.containsKey(id)){
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 1.0f);
            return;
        }

        TutorialScreen screen = new TutorialScreen(client.player);
        client.setScreen(screen);
        screen.setTutorials(id);
    }
}
