package uk.co.cablepost.tutorials.util;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;

public class Resources {
    public static String[] resourceLocation(String v)
            throws Exception {
        String[] parts = v.split(":");

        if (parts.length == 1) {
            return new String[] { "minecraft", parts[0] };
        }

        if (parts.length == 2) {
            return parts;
        }

        throw new Exception("Invalid resource location \"" + v + "\"! Example resource location: \"modname:a/b/c\"");
    }

    public static Resource loadAlternatives(Identifier[] alternatives, ResourceManager mngr)
    throws Exception {
        Resource resource = null;
        for (Identifier id : alternatives) {
            try {
                resource = mngr.getResource(id);
                if (resource == null) continue;
                break;
            } catch (IOException ignored) {}
        }
        if (resource == null)
            throw new Exception("None of the possible texture identifiers existed: " + Arrays.toString(alternatives));
        return resource;
    }

    public static Identifier selectAlternatives(Identifier[] alternatives, ResourceManager mngr) {
        for (Identifier id : alternatives) {
            if (mngr.containsResource(id)) {
                return id;
            }
        }
        return null;
    }

    public static Identifier[] addVariation(Identifier[] alternatives, Function<Identifier, Identifier> map) {
        Identifier[] result = new Identifier[alternatives.length << 1];
        for (int i = 0; i < alternatives.length; i ++) {
            int i2 = i << 1;
            result[i2] = alternatives[i];
            result[i2 + 1] = map.apply(alternatives[i]);
        }
        return result;
    }

    public static Identifier mapPath(Identifier id, Function<String, String> map) {
        return new Identifier(id.getNamespace(), map.apply(id.getPath()));
    }

    public static Identifier[] getTextureAlternatives(Identifier id) {
        String namespace = id.getNamespace();
        String path = id.getPath();

        Identifier[] alt = new Identifier[] {
            id,
            new Identifier(namespace, "textures/" + path),
            new Identifier(namespace, "textures/tutorial_components/" + path),
            new Identifier(namespace, "textures/tutorials/" + path),
        };

        alt = addVariation(alt, (x) ->
            mapPath(x, (p) ->
                p + ".png"));

        return alt;
    }

    public static String removeSuffix(String str, String suffix) {
        if (suffix.length() > str.length())
            return str;

        int cmpBegin = str.length() - suffix.length();

        for (int i = 0; i < suffix.length(); i ++) {
            char a = str.charAt(cmpBegin + i);
            char b = suffix.charAt(i);
            if (a != b) return str;
        }

        if (cmpBegin == 0) return "";

        return str.substring(str.length(), cmpBegin - 1);
    }
}
