package me.alex_s168.tutorials.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface KeyframeData {
    KeyframeData interpolate(KeyframeData other, Interpolator interpolator);

    class Empty implements KeyframeData {

        @Override
        public KeyframeData interpolate(KeyframeData other, Interpolator interpolator) {
            return this;
        }
    }

    class Color implements KeyframeData {

        public int a = 0;
        public int r = 0;
        public int g = 0;
        public int b = 0;

        private int packed = 0;
        public void update() {
            packed = 0;

            packed |= a;
            packed <<= 8;

            packed |= r;
            packed <<= 8;

            packed |= g;
            packed <<= 8;

            packed |= b;
        }

        public int getPackedARGB() {
            return packed;
        }

        public int getPackedRGB() {
            return packed & 0xFF_FF_FF;
        }

        public char[] bytesARGB() {
            return new char[]{(char) a, (char) r, (char) g, (char) b};
        }

        public char[] bytesRGB() {
            return new char[]{(char) r, (char) g, (char) b};
        }

        public void fromRGB(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = 0xFF;
            update();
        }

        public void fromARGB(int a, int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            update();
        }

        public void fromPacked(int packed) {
            this.packed = packed;
            a = packed >> 24 & 0xFF;
            r = packed >> 16 & 0xFF;
            g = packed >> 8  & 0xFF;
            b = packed       & 0xFF;
        }

        public void setAlpha(int alpha) {
            a = alpha;
            packed |= (a << 24);
        }

        public void parse(JsonElement json) throws Exception {
            JsonObject obj = json.getAsJsonObject();

            a = 0xFF;

            boolean hasColor = false;
            if (obj.has("hex")) {
                hasColor = true;
                String hex = obj.get("hex").getAsString();
                fromPacked(Integer.parseInt(hex, 16));
            }

            if (obj.has("a")) {
                a = obj.get("a").getAsInt();
            }
            else if (obj.has("alpha")) {
                a = obj.get("alpha").getAsInt();
            }

            if (hasColor) {
                setAlpha(a);
                return;
            }

            r = (obj.has("r") ? obj.get("r") : obj.get("red")).getAsInt();
            g = (obj.has("g") ? obj.get("g") : obj.get("green")).getAsInt();
            b = (obj.has("b") ? obj.get("b") : obj.get("blue")).getAsInt();

            update();
        }

        @Override
        public KeyframeData interpolate(KeyframeData other, Interpolator interpolator) {
            Color cother = (Color) other;
            Color res = new Color();

            res.a = interpolator.interpolate(a, cother.a);
            res.r = interpolator.interpolate(r, cother.r);
            res.g = interpolator.interpolate(g, cother.g);
            res.b = interpolator.interpolate(b, cother.b);

            return res;
        }
    }
}
