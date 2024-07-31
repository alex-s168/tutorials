package uk.co.cablepost.tutorials;

import org.jetbrains.annotations.Nullable;

public sealed interface Interpolator {
    void update(float t);

    double interpolate(double a, double b);
    long interpolate(long a, long b);
    String interpolate(String a, String b);
    <T> T snap(T a, T b);

    default int interpolate(int a, int b) {
        return (int) interpolate((long) a, (long) b);
    }

    default float interpolate(float a, float b) {
        return (float) interpolate((double) a, (double) b);
    }


    final class Lerp implements Interpolator {
        float t = 0.0f;
        boolean second = false;

        @Override
        public void update(float t) {
            this.t = t;
            this.second = (t >= 0.92f);
        }

        @Override
        public double interpolate(double a, double b) {
            return a + ((double) t) * (b - a);
        }

        @Override
        public long interpolate(long a, long b) {
            return (long) (a + t * (b - a));
        }

        @Override
        public String interpolate(String a, String b) {
            return null;
        }

        @Override
        public <T> T snap(T a, T b) {
            return second ? b : a;
        }
    }

    final class Snap implements Interpolator {
        boolean second = false;

        @Override
        public void update(float t) {
            this.second = (t >= 0.92f);
        }

        @Override
        public double interpolate(double a, double b) {
            return second ? b : a;
        }

        @Override
        public long interpolate(long a, long b) {
            return second ? b : a;
        }

        @Override
        public String interpolate(String a, String b) {
            return second ? b : a;
        }

        @Override
        public <T> T snap(T a, T b) {
            return second ? b : a;
        }
    }

    final class Combined implements Interpolator {
        public Combined(Interpolator num, Interpolator str) {
            this.num = num;
            this.str = str;
        }

        Interpolator num;
        Interpolator str;
        boolean second = false;

        @Override
        public void update(float t) {
            num.update(t);
            str.update(t);
            this.second = (t >= 0.92f);
        }

        @Override
        public double interpolate(double a, double b) {
            return num.interpolate(a, b);
        }

        @Override
        public long interpolate(long a, long b) {
            return num.interpolate(a, b);
        }

        @Override
        public String interpolate(String a, String b) {
            return str.interpolate(a, b);
        }

        @Override
        public <T> T snap(T a, T b) {
            return second ? b : a;
        }
    }

    static @Nullable Interpolator byName(@Nullable String name) {
        if (name == null) {
            return null;
        }

        Interpolator snap = new Snap();

        if (name.equalsIgnoreCase("snap")) {
            return snap;
        }
        else if (name.equalsIgnoreCase("linear")) {
            return new Combined(new Lerp(), snap);
        }
        return null;
    }
}
