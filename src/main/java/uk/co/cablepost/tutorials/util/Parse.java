package uk.co.cablepost.tutorials.util;

public class Parse {
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
}