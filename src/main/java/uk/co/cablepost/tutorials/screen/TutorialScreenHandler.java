package uk.co.cablepost.tutorials.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import org.jetbrains.annotations.Nullable;

public class TutorialScreenHandler extends ScreenHandler {
    protected TutorialScreenHandler(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    TutorialScreenHandler(){
        this(null, 0);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return false;
    }
}
