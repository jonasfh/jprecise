package com.jprecise;

import org.junit.Test;
import static org.junit.Assert.*;

public class SubStepVolumeControllerTest {

    private static final float EPSILON = 0.001f;

    @Test
    public void testCalculateBaseStreamIndex() {
        assertEquals(0, SubStepVolumeController.calculateBaseStreamIndex(0.0f));
        assertEquals(1, SubStepVolumeController.calculateBaseStreamIndex(0.05f));
        assertEquals(1, SubStepVolumeController.calculateBaseStreamIndex(0.1f));
        assertEquals(1, SubStepVolumeController.calculateBaseStreamIndex(0.5f));
        assertEquals(1, SubStepVolumeController.calculateBaseStreamIndex(1.0f));
        assertEquals(1, SubStepVolumeController.calculateBaseStreamIndex(1.9f));
        assertEquals(2, SubStepVolumeController.calculateBaseStreamIndex(2.0f));
        assertEquals(5, SubStepVolumeController.calculateBaseStreamIndex(5.5f));
        assertEquals(15, SubStepVolumeController.calculateBaseStreamIndex(15.0f));
        assertEquals(15, SubStepVolumeController.calculateBaseStreamIndex(20.0f));
    }

    @Test
    public void testCalculateAttenuationDb() {
        // At 0.0 (muted)
        assertEquals(-96.0f, SubStepVolumeController.calculateAttenuationDb(0.0f), EPSILON);

        // At 1.0 (base level 1 full output, 0 dB attenuation)
        assertEquals(0.0f, SubStepVolumeController.calculateAttenuationDb(1.0f), EPSILON);

        // At 0.5 (half level: -12 dB)
        assertEquals(-12.0f, SubStepVolumeController.calculateAttenuationDb(0.5f), EPSILON);

        // At 0.1 (-21.6 dB)
        assertEquals(-21.6f, SubStepVolumeController.calculateAttenuationDb(0.1f), EPSILON);

        // For levels > 1.0
        assertEquals(0.0f, SubStepVolumeController.calculateAttenuationDb(2.0f), EPSILON);
    }

    @Test
    public void testCalculateFloatGain() {
        assertEquals(0.0f, SubStepVolumeController.calculateFloatGain(0.0f), EPSILON);
        assertEquals(0.1f, SubStepVolumeController.calculateFloatGain(0.1f), EPSILON);
        assertEquals(0.5f, SubStepVolumeController.calculateFloatGain(0.5f), EPSILON);
        assertEquals(1.0f, SubStepVolumeController.calculateFloatGain(1.0f), EPSILON);
        assertEquals(1.0f, SubStepVolumeController.calculateFloatGain(5.0f), EPSILON);
    }

    @Test
    public void testSubStepResolutionStrictMonotonicity() {
        // Test that 10 consecutive sub-steps from 0.1 to 1.0 strictly increase in gain and dB
        float prevGain = -1.0f;
        float prevDb = -100.0f;

        for (int i = 1; i <= 10; i++) {
            float level = i / 10.0f;
            float gain = SubStepVolumeController.calculateFloatGain(level);
            float db = SubStepVolumeController.calculateAttenuationDb(level);

            assertTrue("Gain at level " + level + " must be strictly greater than previous", gain > prevGain);
            assertTrue("dB at level " + level + " must be strictly greater than previous", db > prevDb);

            prevGain = gain;
            prevDb = db;
        }
    }
}
