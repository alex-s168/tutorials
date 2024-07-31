package uk.co.cablepost.tutorials.rend;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.cablepost.tutorials.KeyframeData;
import uk.co.cablepost.tutorials.TutorialObjectRender;

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
