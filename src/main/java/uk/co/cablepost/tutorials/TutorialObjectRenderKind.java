package uk.co.cablepost.tutorials;

import com.google.gson.JsonElement;
import net.minecraft.resource.ResourceManager;

public interface TutorialObjectRenderKind {
    TutorialObjectRender parse(JsonElement json, ResourceManager resourceManager)
            throws Exception;
}
