package me.alex_s168.tutorials.api;

import com.google.gson.JsonElement;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

public interface TutorialObjectRender {
    @NotNull
    KeyframeData defaultKeyframe();

    @NotNull
    KeyframeData parseKeyframe(@Nullable KeyframeData last, JsonElement json)
            throws Exception;

    @NotNull
    Optional<String> render(Logger logger, MatrixStack matrices, VertexConsumerProvider.Immediate immediate, TextRenderer textRenderer, KeyframeData keyframe);
}
