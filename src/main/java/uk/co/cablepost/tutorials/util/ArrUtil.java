package uk.co.cablepost.tutorials.util;

import it.unimi.dsi.fastutil.floats.Float2BooleanFunction;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import it.unimi.dsi.fastutil.ints.Int2BooleanFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class ArrUtil {
    public static <T> void zipEach(T[] a, float[] b, BiConsumer<T, Float> fn) {
        assert a.length == b.length;
        for (int i = 0; i < a.length; i++) {
            fn.accept(a[i], b[i]);
        }
    }

    public static float[] map(float[] a, Float2FloatFunction fn) {
        float[] res = new float[a.length];
        for (int i = 0; i < a.length; i ++) {
            res[i] = fn.get(a[i]);
        }
        return res;
    }

    public static float[] map2(float[] a, float[] b, BiFunction<Float, Float, Float> fn) {
        assert a.length == b.length;
        float[] res = new float[a.length];
        for (int i = 0; i < a.length; i ++) {
            res[i] = fn.apply(a[i], b[i]);
        }
        return res;
    }

    public static int[] concat(int[] src, int v) {
        var v2 = new int[src.length + 1];
        System.arraycopy(src, 0, v2, 0, src.length);
        v2[src.length] = v;
        return v2;
    }

    public static @Nullable Integer first(int[] arr, Int2BooleanFunction func) {
        for (int x : arr) {
            if (func.get(x)) {
                return x;
            }
        }
        return null;
    }

    public static boolean any(int[] arr, Int2BooleanFunction func) {
        return first(arr, func) != null;
    }

    public static @Nullable Float first(float[] arr, Float2BooleanFunction func) {
        for (float x : arr) {
            if (func.get(x)) {
                return x;
            }
        }
        return null;
    }

    public static boolean any(float[] arr, Float2BooleanFunction func) {
        return first(arr, func) != null;
    }
}
