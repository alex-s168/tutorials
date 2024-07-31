package uk.co.cablepost.tutorials.rend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import uk.co.cablepost.tutorials.Interpolator;
import uk.co.cablepost.tutorials.KeyframeData;
import uk.co.cablepost.tutorials.TutorialObjectRender;
import uk.co.cablepost.tutorials.TutorialObjectRenderKind;

import java.util.Optional;

public class TextTutorialObjectRender implements TutorialObjectRender {
    public TextTutorialObjectRender(@NotNull String text) {
        this.text = text;
    }

    @NotNull
    String text;

    static class Keyframe implements KeyframeData {
        KeyframeData.Color foreground = new KeyframeData.Color();
        KeyframeData.Color background = new KeyframeData.Color();

        public Keyframe() {
            foreground.fromPacked(0xAAFFFFFF);

            background.setAlpha((int) (0.25f * 255));
        }

        @Override
        public KeyframeData interpolate(KeyframeData other, Interpolator interpolator) {
            Keyframe kother = (Keyframe) other;

            Keyframe res = new Keyframe();
            res.foreground = foreground;
            res.background = background;

            res.foreground.interpolate(kother.foreground, interpolator);
            res.background.interpolate(kother.background, interpolator);

            return res;
        }
    }

    @Override
    public @NotNull KeyframeData defaultKeyframe() {
        return new Keyframe();
    }

    @Override
    public @NotNull KeyframeData parseKeyframe(@Nullable KeyframeData last, JsonElement json) throws Exception {
        Keyframe k = new Keyframe();

        if (last != null) {
            Keyframe klast = (Keyframe) last;
            k.foreground = klast.foreground;
            k.background = klast.background;
        }

        JsonObject obj = json.getAsJsonObject();

        if (obj.has("fg")) {
            k.foreground.parse(obj.get("fg"));
        }

        if (obj.has("bg")) {
            k.background.parse(obj.get("bg"));
        }

        return k;
    }

    @Override
    public @NotNull Optional<String> render(Logger logger, MatrixStack matrices, VertexConsumerProvider.Immediate immediate, TextRenderer textRenderer, KeyframeData keyframe) {
        Keyframe k = (Keyframe) keyframe;

        matrices.push();
        matrices.scale(0.25f, 0.25f, 0.25f);

        textRenderer.draw(text,
                -textRenderer.getWidth(text) / 2f, 0f,
                k.foreground.getPackedARGB(), false,
                matrices.peek().getPositionMatrix(),
                immediate, false, k.background.getPackedARGB(), 255);

        matrices.pop();

        return Optional.empty();
    }

    public static class Src implements TutorialObjectRenderKind {

        @Override
        public TutorialObjectRender parse(JsonElement json, ResourceManager resourceManager) throws Exception {
            JsonObject obj = json.getAsJsonObject();

            String text = obj.get("text").getAsString();

            return new TextTutorialObjectRender(text);
        }
    }
}
