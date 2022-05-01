package uk.co.cablepost.tutorials.mixin;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.cablepost.tutorials.Tutorials;
import uk.co.cablepost.tutorials.client.TutorialsClient;

import java.util.List;
import java.util.Objects;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract Item getItem();

    @Shadow public abstract boolean isEmpty();

    @Inject(at = @At("RETURN"), method = "getTooltip")
    public void getTooltip(@Nullable PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        if(player == null){
            return;
        }

        if(isEmpty()){
            return;
        }

        TutorialsClient.mouseOverItem = getItem();
        TutorialsClient.mouseOverItemLastTime = player.age;

        Identifier id = Registry.ITEM.getId(getItem());
        if(!Tutorials.tutorials.containsKey(id)){
            return;
        }

        if(TutorialsClient.keyBinding.isUnbound()){
            cir.getReturnValue().add(new LiteralText("You need to bind the key: '" + new TranslatableText("key.tutorials.open_tutorial").getString() + "' to view tutorial").formatted(Formatting.GOLD));
        }
        else{
            String keyName;

            String keyNameTranslationKey = TutorialsClient.keyBinding.getBoundKeyTranslationKey();
            keyName = new TranslatableText(keyNameTranslationKey).getString();
            if(Objects.equals(keyNameTranslationKey, keyName)){
                String keyName2 = TutorialsClient.keyBinding.getBoundKeyLocalizedText().asString();
                if(!Objects.equals(keyName2, "")){
                    keyName = keyName2;
                }
            }

            cir.getReturnValue().add(new LiteralText("Press [" + keyName + "] to view tutorial").formatted(Formatting.GOLD));
        }
    }
}
