package uk.co.cablepost.tutorials.rend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.resource.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import uk.co.cablepost.tutorials.KeyframeData;
import uk.co.cablepost.tutorials.TutorialObjectRender;
import uk.co.cablepost.tutorials.TutorialObjectRenderKind;

import java.util.Optional;

public class BlockTutorialObjectRender extends EmptyKeyframeRender {
    public BlockTutorialObjectRender(@NotNull BlockState blockState) {
        this.blockState = blockState;
    }

    @NotNull
    BlockState blockState;

    @Override
    public @NotNull Optional<String> render(Logger logger, MatrixStack matrices, VertexConsumerProvider.Immediate immediate, TextRenderer textRenderer, KeyframeData keyframe) {
        matrices.push();
        matrices.translate(-0.5f, 0, -0.5f);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                blockState, matrices, immediate, 255, OverlayTexture.DEFAULT_UV);
        matrices.pop();

        return Optional.empty();
    }

    public static class Src implements TutorialObjectRenderKind {
        @Override
        public TutorialObjectRender parse(JsonElement json, ResourceManager resourceManager) throws Exception {
            JsonObject obj = json.getAsJsonObject();

            String block = obj.get("block").getAsString();
            String state = "";
            if (obj.has("state")) {
                state = obj.get("state").getAsString();
            }

            String blockStateString = block + "[" + state + "]";
            BlockArgumentParser blockArgumentParser = new BlockArgumentParser(new StringReader(blockStateString), true);
            blockArgumentParser.parseBlockId();
            blockArgumentParser.parseBlockProperties();

            BlockState blockState = blockArgumentParser.getBlockState();

            if (blockState == null)
                throw new Exception("Error parsing block / block state!");

            return new BlockTutorialObjectRender(blockState);
        }
    }
}
