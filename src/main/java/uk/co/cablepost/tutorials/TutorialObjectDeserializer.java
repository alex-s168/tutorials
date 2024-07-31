package uk.co.cablepost.tutorials;

import com.google.gson.*;
import net.minecraft.util.Identifier;
import uk.co.cablepost.tutorials.client.TutorialsClient;

import java.lang.reflect.Type;

public class TutorialObjectDeserializer implements JsonDeserializer<TutorialObject> {
    @Override
    public TutorialObject deserialize(JsonElement jsonElement,
                                      Type type,
                                      JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        JsonObject obj = jsonElement.getAsJsonObject();

        Identifier rendKindName = new Identifier(obj.get("kind").getAsString());
        TutorialObjectRenderKind rendKind = TutorialsClient.tutorialRenderers.get(rendKindName);
        if (rendKind == null)
            throw new JsonParseException("Renderer " + rendKindName + " not registered!");

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
