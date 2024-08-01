package uk.co.cablepost.tutorials;

import com.google.gson.*;
import me.alex_s168.tutorials.api.Interpolator;
import me.alex_s168.tutorials.api.TutorialManager;
import me.alex_s168.tutorials.api.TutorialObject;
import me.alex_s168.tutorials.api.TutorialObjectRender;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;

public class TutorialObjectDeserializer implements JsonDeserializer<TutorialObject> {
    @Override
    public TutorialObject deserialize(JsonElement jsonElement,
                                      Type type,
                                      JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        if (jsonElement == null) return null;
        JsonObject obj = jsonElement.getAsJsonObject();
        if (obj == null) return null;

        Identifier rendKindName = new Identifier(obj.get("kind").getAsString());
        var rendKind = TutorialManager.getRender(rendKindName)
                .orElseThrow(() -> new JsonParseException("Renderer " + rendKindName + " not registered!"));

        TutorialObjectRender rend;

        var resMan = TutorialsClient.lastResourceManager;
        assert resMan != null;

        JsonElement cfg = obj.get("config");
        try {
            rend = rendKind.parse(cfg, resMan);
        } catch (Exception e) {
            throw new JsonParseException(e);
        }

        TutorialObject res = new TutorialObject();
        res.renderer = rend;

        var interp = obj.has("interpolate") ? obj.get("interpolate").getAsString() : "snap";
        res.interpolator = Interpolator.byName(interp);

        return res;

    }
}
