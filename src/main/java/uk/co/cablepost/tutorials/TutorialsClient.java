package uk.co.cablepost.tutorials;

import me.alex_s168.tutorials.api.*;
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
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.cablepost.tutorials.screen.TutorialScreen;
import uk.co.cablepost.tutorials.rend.*;
import uk.co.cablepost.tutorials.util.Resources;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class TutorialsClient implements ClientModInitializer {

    public static final String MOD_ID = "tutorials";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyBinding keyBinding;
    public static Item mouseOverItem;
    public static int timeSinceMouseOverItem;
    public static int timeSinceMouseTextInputFocus;

    public static ResourceManager lastResourceManager;

    public static void registerRenders(Map<Identifier, TutorialObjectRenderKind> dest) {
        dest.put(new Identifier("tutorials", "block"),
                new BlockTutorialObjectRender.Src());

        dest.put(new Identifier("tutorials", "item"),
                new ItemTutorialObjectRender.Src());

        dest.put(new Identifier("tutorials", "text"),
                new TextTutorialObjectRender.Src());

        dest.put(new Identifier("tutorials", "texture"),
                new TextureTutorialObjectRender.Src());

        dest.put(new Identifier("tutorials", "empty"),
                new EmptyRender.Src());
    }

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public void reload(ResourceManager manager) {
                        TutorialManager.reload(manager, (reg) -> {
                            for (Identifier id : manager.findResources("tutorials", path -> path.endsWith(".json"))) {
                                var identifier = Resources.mapPath(id, (p) ->
                                        Resources.removeSuffix(p, ".json")
                                                .substring("tutorials/".length()));

                                try (InputStream stream = manager.getResource(id).getInputStream()) {
                                    var str = IOUtils.toString(stream, StandardCharsets.UTF_8);
                                    reg.register(identifier, str);
                                } catch (Exception e) {
                                    String msg = "Error while loading tutorial '" + id.getPath() + "': " + e;
                                    LOGGER.error(msg);
                                }
                            }
                        });
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
                onTutorialKey();
            }
        });
    }

    public static void onTutorialKey() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null){
            return;
        }

        if (timeSinceMouseTextInputFocus < 2){
            return;
        }

        if (client.currentScreen == null){
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

            client.player.sendMessage(new TranslatableText("tutorials.no_item_in_screen"), true);
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 1.0f);
            return;
        }

        if (timeSinceMouseOverItem > 2) {
            client.player.sendMessage(new TranslatableText("tutorials.no_item_in_game"), true);
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 1.0f);
            return;
        }

        openScreenFor(TutorialsClient.mouseOverItem, client);
    }

    private static void openScreenFor(Item item, MinecraftClient client) {
        if (client.player == null){
            return;
        }

        Identifier id = Registry.ITEM.getId(item);

        if (TutorialManager.byItem(id).isEmpty()) {
            client.player.sendMessage(new TranslatableText("tutorials.no_tutorial"), true);
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 1.0f);
            return;
        }

        TutorialScreen screen = new TutorialScreen(client.player);
        client.setScreen(screen);
        screen.setTutorials(id);
    }
}
