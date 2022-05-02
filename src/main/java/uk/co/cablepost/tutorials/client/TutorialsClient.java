package uk.co.cablepost.tutorials.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.lwjgl.glfw.GLFW;
import uk.co.cablepost.tutorials.Tutorials;
import uk.co.cablepost.tutorials.client.screen.TutorialScreen;

@Environment(EnvType.CLIENT)
public class TutorialsClient implements ClientModInitializer {

    public static KeyBinding keyBinding;
    public static Item mouseOverItem;
    public static int timeSinceMouseOverItem;
    public static int timeSinceMouseTextInputFocus;

    @Override
    public void onInitializeClient() {
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
        if(!Tutorials.tutorials.containsKey(id)){
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 1.0f);
            return;
        }

        TutorialScreen screen = new TutorialScreen(client.player);
        client.setScreen(screen);
        screen.setTutorials(id);
    }
}
