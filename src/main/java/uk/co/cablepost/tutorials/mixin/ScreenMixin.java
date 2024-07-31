package uk.co.cablepost.tutorials.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.cablepost.tutorials.client.TutorialsClient;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(at = @At("HEAD"), method = "keyPressed")
    public void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if(TutorialsClient.keyBinding.matchesKey(keyCode, scanCode)){
            TutorialsClient.onTutorialKey();
        }
    }
}
