package uk.co.cablepost.tutorials.rend;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import me.alex_s168.tutorials.api.KeyframeData;
import me.alex_s168.tutorials.api.TutorialObjectRender;

public abstract class EmptyKeyframeRender implements TutorialObjectRender {
    @Override
    public @NotNull KeyframeData defaultKeyframe() {
        return new KeyframeData.Empty();
    }

    @Override
    public @NotNull KeyframeData parseKeyframe(@Nullable KeyframeData last, JsonElement json) throws Exception {
        return new KeyframeData.Empty();
    }
}
