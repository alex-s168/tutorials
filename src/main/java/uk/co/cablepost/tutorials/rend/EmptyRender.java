package uk.co.cablepost.tutorials.rend;

import com.google.gson.JsonElement;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import me.alex_s168.tutorials.api.KeyframeData;
import me.alex_s168.tutorials.api.TutorialObjectRender;
import me.alex_s168.tutorials.api.TutorialObjectRenderKind;

import java.util.Optional;

public class EmptyRender extends EmptyKeyframeRender {
    @Override
    public @NotNull Optional<String> render(Logger logger, MatrixStack matrices, VertexConsumerProvider.Immediate immediate, TextRenderer textRenderer, KeyframeData keyframe) {
        return Optional.empty();
    }

    public static class Src implements TutorialObjectRenderKind {

        @Override
        public TutorialObjectRender parse(JsonElement json, ResourceManager resourceManager) throws Exception {
            return new EmptyRender();
        }
    }
}
