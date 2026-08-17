package com.jprecise;

import org.junit.Test;
import static org.junit.Assert.*;

public class VariableStepVolumeCurveTest {

    private static final float EPSILON = 0.001f;

    @Test
    public void testFullAscendingWalk() {
        float[] expectedAscending = new float[]{
                0.05f, 0.10f, 0.15f, 0.20f, 0.25f, 0.30f,
                0.40f, 0.50f, 0.60f, 0.70f, 0.80f, 0.90f,
                1.90f, 2.90f, 3.90f, 4.90f, 5.90f, 6.90f,
                7.90f, 8.90f, 9.90f, 10.90f, 11.90f, 12.90f,
                13.90f, 14.90f, 15.00f
        };

        float current = 0.00f;
        for (float expected : expectedAscending) {
            current = VariableStepVolumeCurve.calculateStepUp(current);
            assertEquals("Stepping up to " + expected, expected, current, EPSILON);
        }

        // Cap at MAX_VOLUME
        assertEquals(15.00f, VariableStepVolumeCurve.calculateStepUp(15.00f), EPSILON);
    }

    @Test
    public void testFullDescendingWalk() {
        float[] expectedDescending = new float[]{
                14.90f, 13.90f, 12.90f, 11.90f, 10.90f, 9.90f,
                8.90f, 7.90f, 6.90f, 5.90f, 4.90f, 3.90f,
                2.90f, 1.90f, 0.90f, 0.80f, 0.70f, 0.60f,
                0.50f, 0.40f, 0.30f, 0.25f, 0.20f, 0.15f,
                0.10f, 0.05f, 0.00f
        };

        float current = 15.00f;
        for (float expected : expectedDescending) {
            current = VariableStepVolumeCurve.calculateStepDown(current);
            assertEquals("Stepping down to " + expected, expected, current, EPSILON);
        }

        // Cap at MIN_VOLUME
        assertEquals(0.00f, VariableStepVolumeCurve.calculateStepDown(0.00f), EPSILON);
    }

    @Test
    public void testBoundaryTransitions() {
        // Low to Mid boundary
        assertEquals(0.30f, VariableStepVolumeCurve.calculateStepUp(0.25f), EPSILON);
        assertEquals(0.40f, VariableStepVolumeCurve.calculateStepUp(0.30f), EPSILON);
        assertEquals(0.25f, VariableStepVolumeCurve.calculateStepDown(0.30f), EPSILON);
        assertEquals(0.30f, VariableStepVolumeCurve.calculateStepDown(0.40f), EPSILON);

        // Mid to High boundary
        assertEquals(0.90f, VariableStepVolumeCurve.calculateStepUp(0.80f), EPSILON);
        assertEquals(1.90f, VariableStepVolumeCurve.calculateStepUp(0.90f), EPSILON);
        assertEquals(0.80f, VariableStepVolumeCurve.calculateStepDown(0.90f), EPSILON);
        assertEquals(0.90f, VariableStepVolumeCurve.calculateStepDown(1.90f), EPSILON);

        // Zero / Mute boundary
        assertEquals(0.05f, VariableStepVolumeCurve.calculateStepUp(0.00f), EPSILON);
        assertEquals(0.00f, VariableStepVolumeCurve.calculateStepDown(0.05f), EPSILON);
    }

    @Test
    public void testArbitraryIntermediatePositions() {
        // In high range
        assertEquals(6.00f, VariableStepVolumeCurve.calculateStepUp(5.00f), EPSILON);
        assertEquals(4.00f, VariableStepVolumeCurve.calculateStepDown(5.00f), EPSILON);

        // In mid range
        assertEquals(0.65f, VariableStepVolumeCurve.calculateStepUp(0.55f), EPSILON);
        assertEquals(0.45f, VariableStepVolumeCurve.calculateStepDown(0.55f), EPSILON);

        // In low range
        assertEquals(0.18f, VariableStepVolumeCurve.calculateStepUp(0.13f), EPSILON);
        assertEquals(0.08f, VariableStepVolumeCurve.calculateStepDown(0.13f), EPSILON);
    }

    @Test
    public void testGetStepSizeAt() {
        assertEquals(0.05f, VariableStepVolumeCurve.getStepSizeAt(0.00f), EPSILON);
        assertEquals(0.05f, VariableStepVolumeCurve.getStepSizeAt(0.15f), EPSILON);
        assertEquals(0.05f, VariableStepVolumeCurve.getStepSizeAt(0.29f), EPSILON);

        assertEquals(0.10f, VariableStepVolumeCurve.getStepSizeAt(0.30f), EPSILON);
        assertEquals(0.10f, VariableStepVolumeCurve.getStepSizeAt(0.60f), EPSILON);
        assertEquals(0.10f, VariableStepVolumeCurve.getStepSizeAt(0.89f), EPSILON);

        assertEquals(1.00f, VariableStepVolumeCurve.getStepSizeAt(0.90f), EPSILON);
        assertEquals(1.00f, VariableStepVolumeCurve.getStepSizeAt(5.00f), EPSILON);
        assertEquals(1.00f, VariableStepVolumeCurve.getStepSizeAt(15.00f), EPSILON);
    }
}
