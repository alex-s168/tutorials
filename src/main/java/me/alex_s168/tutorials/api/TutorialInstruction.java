package me.alex_s168.tutorials.api;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.cablepost.tutorials.util.ArrUtil;
import uk.co.cablepost.tutorials.util.JsonUtil;

import java.lang.reflect.Type;

public class TutorialInstruction {
    public TutorialInstruction(@NotNull String object_id, int time, @NotNull BaseData baseData, @Nullable JsonElement keyframeData) {
        this.object_id = object_id;
        this.time = time;
        this.baseData = baseData;
        this.keyframeData = keyframeData;
    }

    @NotNull
    public String object_id;

    public int time;

    @NotNull
    public BaseData baseData;

    @Nullable
    public JsonElement keyframeData;


    public static class BaseData {

        public boolean show = true;
        public float[] pos = new float[3];
        public float[] rot = new float[3];
        public float[] scale = new float[3];
        public float fov = 66f;

        public void from(BaseData o) {
            show = o.show;
            pos = o.pos.clone();
            rot = o.rot.clone();
            scale = o.scale.clone();
            fov = o.fov;
        }

        public void from(JsonElement json) {
            JsonObject obj = json.getAsJsonObject();

            JsonUtil.with(obj, "position", JsonUtil::asFloatArr, (pos) -> {
                assert pos.length == 3;
                this.pos = pos;
            });

            JsonUtil.with(obj, "rotation", JsonUtil::asFloatArr, (rot) -> {
                assert rot.length == 3;
                this.rot = rot;
            });

            JsonUtil.with(obj, "scale", JsonUtil::asFloatArr, (scale) -> {
                assert scale.length == 3;
                this.scale = scale;
            });

            JsonUtil.with(obj, "fov", JsonElement::getAsFloat, (fov) -> {
                this.fov = fov;
            });
        }

        public BaseData interpolate(BaseData other, Interpolator interpolator) {
            BaseData res = new BaseData();

            res.fov = interpolator.interpolate(fov, other.fov);
            res.show = interpolator.snap(show, other.show);
            res.pos = ArrUtil.map2(pos, other.pos, interpolator::interpolate);
            res.rot = ArrUtil.map2(rot, other.rot, interpolator::interpolate);
            res.scale = ArrUtil.map2(scale, other.scale, interpolator::interpolate);

            return res;
        }
    }

    public static class Keyframe {

        public Keyframe(@NotNull BaseData base, @NotNull KeyframeData other) {
            this.base = base;
            this.other = other;
        }

        @NotNull
        public BaseData base;

        @NotNull
        public KeyframeData other;

        public Keyframe interpolate(Keyframe second, Interpolator interpolator) {
            BaseData resbd = base.interpolate(second.base, interpolator);
            KeyframeData reskd = other.interpolate(second.other, interpolator);
            return new Keyframe(resbd, reskd);
        }

        public static Keyframe getDefault(KeyframeData kd) {
            return new Keyframe(new BaseData(), kd);
        }
    }

    public static class Deserializer implements JsonDeserializer<TutorialInstruction> {

        @Override
        public TutorialInstruction deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject obj = jsonElement.getAsJsonObject();

            String objId = obj.get("object").getAsString();
            int time = obj.get("time").getAsInt();
            BaseData bd = jsonDeserializationContext.deserialize(jsonElement, BaseData.class);
            JsonElement extra = obj.has("extra") ? obj.get("extra").deepCopy() : null;

            return new TutorialInstruction(objId, time, bd, extra);
        }
    }
}