package edu.sdsu.rocket.core.models;

import edu.sdsu.rocket.core.helpers.MathHelper;

public class Pressures {

    public static final double MOTOR_MAX_PRESSURE    = 500;
    public static final double LOX_MAX_PRESSURE      = 750;
    public static final double KEROSENE_MAX_PRESSURE = 750;
    public static final double HELIUM_MAX_PRESSURE   = 2500;
    public static final double RCS_LOW_MAX_PRESSURE  = 800;
    public static final double RCS_HIGH_MAX_PRESSURE = 5000;

    private final Analog analog;

    public Pressures(Analog analog) {
        this.analog = analog;
    }
    
    public float getMotor() {
        float volts = analog.get(0) / 1000f;
        return MathHelper.translate(volts, 0f, 5f, 0f, (float) MOTOR_MAX_PRESSURE);
        
        // P51-500-A-A-I36-5V-000-000
        // calibrated transducer #1 on Nov 13, 2014
//        return 0.210439f * analog.get(0) - 150.502f;
    }
    
    public float getLOX() {
        float volts = analog.get(1) / 1000f;
        return MathHelper.translate(volts, 0f, 5f, 0f, (float) LOX_MAX_PRESSURE);
        
        // P51-500-A-A-I36-5V-000-000
        // calibrated transducer #2 on Nov 11, 2014
//        return 0.20688f * analog.get(1) - 143.273f;
    }
    
    public float getKerosene() {
        float volts = analog.get(2) / 1000f;
        return MathHelper.translate(volts, 0f, 5f, 0f, (float) KEROSENE_MAX_PRESSURE);
        
        // P51-500-A-A-I36-5V-000-000
        // calibrated transducer #3 on Nov 11, 2014
//        return 0.212968f * analog.get(2) - 147.109f;
    }
    
    public float getHelium() {
        float volts = analog.get(3) / 1000f;
        return MathHelper.translate(volts, 0f, 5f, 0f, (float) HELIUM_MAX_PRESSURE);
        
        // MSP-300-2K5-P-4-N-1
        // calibrated transducer #4 on Nov 13, 2014
//        return 1.060797f * analog.get(3) - 653.691f;
    }
    
    public float getRcsLow() {
        float volts = analog.get(4) / 1000f;
        return MathHelper.translate(volts, 0f, 5f, 0f, (float) RCS_LOW_MAX_PRESSURE);
    }

    public float getRcsHigh() {
        float volts = analog.get(5) / 1000f;
        return MathHelper.translate(volts, 0f, 5f, 0f, (float) RCS_HIGH_MAX_PRESSURE);
    }
    
}
