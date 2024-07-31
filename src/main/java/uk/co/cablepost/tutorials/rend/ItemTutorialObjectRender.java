package uk.co.cablepost.tutorials.rend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.resource.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import me.alex_s168.tutorials.api.KeyframeData;
import me.alex_s168.tutorials.api.TutorialObjectRender;
import me.alex_s168.tutorials.api.TutorialObjectRenderKind;

import java.util.Optional;

public class ItemTutorialObjectRender extends EmptyKeyframeRender {
    public ItemTutorialObjectRender(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @NotNull
    ItemStack itemStack;

    @Override
    public @NotNull Optional<String> render(Logger logger, MatrixStack matrices, VertexConsumerProvider.Immediate immediate, TextRenderer textRenderer, KeyframeData keyframe) {
        MinecraftClient.getInstance().getItemRenderer().renderItem(
                itemStack,
                ModelTransformation.Mode.NONE,
                255,
                OverlayTexture.DEFAULT_UV,
                matrices,
                immediate,
                0
        );

        return Optional.empty();
    }

    public static class Src implements TutorialObjectRenderKind {

        @Override
        public TutorialObjectRender parse(JsonElement json, ResourceManager resourceManager) throws Exception {
            JsonObject obj = json.getAsJsonObject();

            String itemStr = obj.get("item").getAsString();

            ItemStringReader reader = new ItemStringReader(new StringReader(itemStr), false);
            Item item = reader.getItem();
            NbtCompound nbt = reader.getNbt();
            ItemStack stack = new ItemStack(item);
            if (nbt != null) {
                stack.setNbt(nbt);
            }

            return new ItemTutorialObjectRender(stack);
        }
    }
}
