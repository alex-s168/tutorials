package uk.co.cablepost.tutorials.rend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import uk.co.cablepost.tutorials.Interpolator;
import uk.co.cablepost.tutorials.KeyframeData;
import uk.co.cablepost.tutorials.TutorialObjectRender;
import uk.co.cablepost.tutorials.TutorialObjectRenderKind;
import uk.co.cablepost.tutorials.util.Resources;

import java.util.Optional;

public class TextureTutorialObjectRender implements TutorialObjectRender {
    public TextureTutorialObjectRender(@NotNull Identifier texture) {
        this.texture = texture;
    }

    @NotNull
    Identifier texture;

    static class Keyframe implements KeyframeData {
        int width = 0;
        int height = 0;

        int crop_x = 0;
        int crop_y = 0;

        int u = 0;
        int v = 0;

        public void from(Keyframe other) {
            width = other.width;
            height = other.height;

            crop_x = other.crop_x;
            crop_y = other.crop_y;

            u = other.u;
            v = other.v;
        }

        @Override
        public KeyframeData interpolate(KeyframeData other, Interpolator i) {
            Keyframe o = (Keyframe) other;

            Keyframe r = new Keyframe();

            r.width = i.interpolate(width, o.width);
            r.height = i.interpolate(height, o.height);

            r.crop_x = i.interpolate(crop_x, o.crop_x);
            r.crop_y = i.interpolate(crop_y, o.crop_y);

            r.u = i.interpolate(u, o.u);
            r.v = i.interpolate(v, o.v);

            return r;
        }
    }

    @Override
    public @NotNull KeyframeData defaultKeyframe() {
        return new Keyframe();
    }

    @Override
    public @NotNull KeyframeData parseKeyframe(@Nullable KeyframeData last, JsonElement json) throws Exception {
        Keyframe res = new Keyframe();

        if (last != null) {
            res.from((Keyframe) last);
        }

        JsonObject obj = json.getAsJsonObject();

        if (obj.has("width"))
            res.width = obj.get("width").getAsInt();
        if (obj.has("height"))
            res.height = obj.get("height").getAsInt();

        if (obj.has("crop_x"))
            res.crop_x = obj.get("crop_x").getAsInt();
        if (obj.has("crop_y"))
            res.crop_y = obj.get("crop_y").getAsInt();

        if (obj.has("u"))
            res.u = obj.get("u").getAsInt();
        if (obj.has("v"))
            res.v = obj.get("v").getAsInt();

        return res;
    }

    @Override
    public @NotNull Optional<String> render(Logger logger, MatrixStack matrices, VertexConsumerProvider.Immediate immediate, TextRenderer textRenderer, KeyframeData keyframe) {
        Keyframe k = (Keyframe) keyframe;

        RenderSystem.setShaderTexture(0, texture);

        matrices.push();

        matrices.scale(0.25f, 0.25f, 0.25f);

        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180));

        matrices.translate(k.crop_x / 2f, -k.crop_y / 2f, 0f);

        matrices.translate(-k.crop_x, 0f, 0f);
        DrawableHelper.drawTexture(
                matrices,
                0,//x
                0,//y
                0,//z
                k.u,//u
                k.v,//v
                k.crop_x,//w
                k.crop_y,//h
                k.width,//tw
                k.height//th
        );
        matrices.translate(k.crop_x, 0f, 0f);
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180f));
        DrawableHelper.drawTexture(
                matrices,
                0,//x
                0,//y
                0,//z
                k.u,//u
                k.v,//v
                k.crop_x,//w
                k.crop_y,//h
                k.width,//tw
                k.height//th
        );

        matrices.pop();

        return Optional.empty();
    }

    public static class Src implements TutorialObjectRenderKind {

        @Override
        public TutorialObjectRender parse(JsonElement json, ResourceManager resourceManager) throws Exception {
            JsonObject obj = json.getAsJsonObject();

            String idStr = obj.get("texture").getAsString();
            Identifier id = new Identifier(idStr);
            id = Resources.selectAlternatives(
                    Resources.getTextureAlternatives(id),
                    resourceManager);

            if (id == null) {
                throw new Exception("Texture \"" + idStr + "\" not found!");
            }

            return new TextureTutorialObjectRender(id);
        }
    }
}
