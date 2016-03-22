package edu.sdsu.rocket.core.helpers;

public class PressureValueTranslatorFactory {

    public static ValueTranslator getLOX() {
        // P51-500-A-A-I36-5V-000-000
        // calibrated transducer #2 on Nov 11, 2014
        return new ValueTranslator(0.20688f, -143.273f);
    }

    public static ValueTranslator getKerosene() {
        // P51-500-A-A-I36-5V-000-000
        // calibrated transducer #3 on Nov 11, 2014
        return new ValueTranslator(0.212968f, -147.109f);
    }

    public static ValueTranslator getHelium() {
        // MSP-300-2K5-P-4-N-1
        // calibrated transducer #4 on Nov 13, 2014
        return new ValueTranslator(1.060797f, -653.691f);
    }

    public static ValueTranslator getMotor() {
        // P51-500-A-A-I36-5V-000-000
        // calibrated transducer #1 on Nov 13, 2014
        return new ValueTranslator(0.210439f, -150.502f);
    }

    public static ValueTranslator lerp(float from, float to) {
        return new ValueTranslator(to / from, 0f);
    }

    public static ValueTranslator getRcsLow() {
        return lerp(5000f, 700f);
    }

    public static ValueTranslator getRcsHigh() {
        return lerp(5000f, 3000f);
    }

}
