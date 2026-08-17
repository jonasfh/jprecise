package com.jprecise;

public final class VariableStepVolumeCurve {

    public static final float MIN_VOLUME = 0.0f;
    public static final float MIN_NON_ZERO_VOLUME = 0.05f;
    public static final float MAX_VOLUME = 15.0f;

    public static final float LOW_RANGE_THRESHOLD = 0.30f;
    public static final float MID_RANGE_THRESHOLD = 0.90f;

    public static final float LOW_STEP_SIZE = 0.05f;
    public static final float MID_STEP_SIZE = 0.10f;
    public static final float HIGH_STEP_SIZE = 1.00f;

    private static final float EPSILON = 0.001f;

    private VariableStepVolumeCurve() {
        // Utility class
    }

    public static float calculateStepUp(float currentLevel) {
        float rounded = round2(currentLevel);

        if (rounded < MIN_NON_ZERO_VOLUME - EPSILON) {
            return MIN_NON_ZERO_VOLUME;
        }
        if (rounded < LOW_RANGE_THRESHOLD - EPSILON) {
            return round2(rounded + LOW_STEP_SIZE);
        }
        if (rounded < MID_RANGE_THRESHOLD - EPSILON) {
            return round2(rounded + MID_STEP_SIZE);
        }
        return round2(Math.min(MAX_VOLUME, rounded + HIGH_STEP_SIZE));
    }

    public static float calculateStepDown(float currentLevel) {
        float rounded = round2(currentLevel);

        if (rounded <= MIN_NON_ZERO_VOLUME + EPSILON) {
            return MIN_VOLUME;
        }
        if (rounded <= LOW_RANGE_THRESHOLD + EPSILON) {
            return round2(rounded - LOW_STEP_SIZE);
        }
        if (rounded <= MID_RANGE_THRESHOLD + EPSILON) {
            return round2(rounded - MID_STEP_SIZE);
        }
        if (rounded >= MAX_VOLUME - EPSILON) {
            return round2(MAX_VOLUME - 0.10f); // 15.00 -> 14.90
        }

        float candidate = rounded - HIGH_STEP_SIZE;
        if (candidate < MID_RANGE_THRESHOLD) {
            return MID_RANGE_THRESHOLD; // Snap to 0.90 when crossing threshold downwards
        }
        return round2(candidate);
    }

    public static float getStepSizeAt(float currentLevel) {
        float rounded = round2(currentLevel);
        if (rounded < LOW_RANGE_THRESHOLD - EPSILON) {
            return LOW_STEP_SIZE;
        }
        if (rounded < MID_RANGE_THRESHOLD - EPSILON) {
            return MID_STEP_SIZE;
        }
        return HIGH_STEP_SIZE;
    }

    public static float round2(float value) {
        return Math.round(value * 100.0f) / 100.0f;
    }
}
