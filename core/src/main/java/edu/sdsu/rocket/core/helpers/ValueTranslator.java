package edu.sdsu.rocket.core.helpers;

public class ValueTranslator {

    private final float slope;
    private final float bias;

    public ValueTranslator(float slope, float bias) {
        this.slope = slope;
        this.bias = bias;
    }

    public float translate(float value) {
        return slope * value + bias;
    }

}
