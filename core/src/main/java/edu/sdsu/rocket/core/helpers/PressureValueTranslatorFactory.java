package edu.sdsu.rocket.core.helpers;

public class PressureValueTranslatorFactory {

    public static ValueTranslator getLOX() {
        return genericMSP304_01KP();
    }

    public static ValueTranslator getKerosene() {
        return genericMSP304_01KP();
    }

    public static ValueTranslator getHelium() {
        return genericMSP304_05KP();
    }

    public static ValueTranslator getMotor() {
        return genericMSP304_01KP();
    }

    public static ValueTranslator lerp(float from, float to) {
        return new ValueTranslator(to / from, 0f);
    }

    public static ValueTranslator getRcsLow() {
        return genericMSP304_01KP();
    }

    public static ValueTranslator getRcsHigh() {
        return genericMSP304_05KP();
    }

    private static ValueTranslator genericMSP304_01KP() {
        return new ValueTranslator(1f/4f, -250f);
    }

    private static ValueTranslator genericMSP304_05KP() {
        return new ValueTranslator(5f/4f, -1250f);
    }

}
