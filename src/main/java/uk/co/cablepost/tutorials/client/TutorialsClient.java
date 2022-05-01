package uk.co.cablepost.tutorials.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import uk.co.cablepost.tutorials.client.screen.TutorialScreen;

@Environment(EnvType.CLIENT)
public class TutorialsClient implements ClientModInitializer {

    private static KeyBinding keyBinding;

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tutorials.open_tutorial", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_LEFT_ALT, // The keycode of the key
                "category.tutorials" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                //client.player.sendMessage(new LiteralText("Key 1 was pressed!"), false);
                //client.currentScreen.getTooltipFromItem()
                TutorialScreen screen = new TutorialScreen(client.player);
                client.setScreen(screen);
                try {
                    screen.setTutorials(new Identifier("minecraft", "obsidian"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
