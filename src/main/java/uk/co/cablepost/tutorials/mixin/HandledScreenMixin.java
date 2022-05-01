package uk.co.cablepost.tutorials.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.cablepost.tutorials.client.TutorialsClient;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Shadow @Nullable protected Slot focusedSlot;

    @Inject(at = @At("RETURN"), method = "render")
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        /*
        if(focusedSlot == null ){
            TutorialsClient.mouseOverItem = null;
        }
        else {
            TutorialsClient.mouseOverItem = focusedSlot.getStack().getItem();
        }
        */
    }
}
