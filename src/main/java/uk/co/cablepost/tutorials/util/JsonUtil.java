package uk.co.cablepost.tutorials.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public class JsonUtil {
    public static <T> List<T> asList(JsonElement e, Function<JsonElement, T> fn) {
        JsonArray arr = e.getAsJsonArray();
        List<T> res = new ArrayList<>();
        for (int i = 0; i < arr.size(); i ++) {
            res.add(fn.apply(arr.get(i)));
        }
        return res;
    }

    public static <T> T[] asArr(JsonElement e, IntFunction<T[]> construct, Function<JsonElement, T> fn) {
        return asList(e, fn).toArray(construct);
    }

    public static int[] asIntArr(JsonElement e, Function<JsonElement, Integer> fn) {
        JsonArray arr = e.getAsJsonArray();
        int[] res = new int[arr.size()];
        for (int i = 0; i < arr.size(); i ++) {
            res[i] = fn.apply(arr.get(i));
        }
        return res;
    }

    public static int[] asIntArr(JsonElement e) {
        JsonArray arr = e.getAsJsonArray();
        int[] res = new int[arr.size()];
        for (int i = 0; i < arr.size(); i ++) {
            res[i] = arr.get(i).getAsInt();
        }
        return res;
    }

    public static float[] asFloatArr(JsonElement e, Function<JsonElement, Integer> fn) {
        JsonArray arr = e.getAsJsonArray();
        float[] res = new float[arr.size()];
        for (int i = 0; i < arr.size(); i ++) {
            res[i] = fn.apply(arr.get(i));
        }
        return res;
    }

    public static float[] asFloatArr(JsonElement e) {
        JsonArray arr = e.getAsJsonArray();
        float[] res = new float[arr.size()];
        for (int i = 0; i < arr.size(); i ++) {
            res[i] = arr.get(i).getAsInt();
        }
        return res;
    }

    public static JsonObject with(JsonObject obj, String key, Consumer<JsonElement> maybeOnce) {
        if (obj.has(key)) {
            maybeOnce.accept(obj.get(key));
        }
        return obj;
    }

    public static <T> JsonObject with(JsonObject obj, String key, Function<JsonElement, T> maybeOnce1, Consumer<T> maybeOnce2) {
        if (obj.has(key)) {
            maybeOnce2.accept(maybeOnce1.apply(obj.get(key)));
        }
        return obj;
    }

    public static JsonElement empty() {
        return new JsonElement() {
            @Override
            public JsonElement deepCopy() {
                return this;
            }
        };
    }
}
