package edu.sdsu.rocket.server.devices;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

/**
 * Self-Calibrating, 16-Bit Analog-to-Digital Converter
 */
public class ADS1100 {

    /**
     *        I2C Address | Package Marking
     * 0x48 |   1001 000  |       AD0
     * 0x49 |   1001 001  |       AD1
     * 0x4A |   1001 010  |       AD2
     * 0x4B |   1001 011  |       AD3
     * 0x4C |   1001 100  |       AD4
     * 0x4D |   1001 101  |       AD5
     * 0x4E |   1001 110  |       AD6
     * 0x4F |   1001 111  |       AD7
     */
    public enum Address {
        AD0(0b1001_000),
        AD1(0b1001_001),
        AD2(0b1001_010),
        AD3(0b1001_011),
        AD4(0b1001_100),
        AD5(0b1001_101),
        AD6(0b1001_110),
        AD7(0b1001_111),
        ;
        int config;
        Address(int config) {
            this.config = config;
        }
    }

    /**
     * CONFIGURATION REGISTER
     *
     * You can use the 8-bit configuration register to control the ADS1100’s operating mode, data rate, and PGA
     * settings. The default setting is 0x8C (1000 1100).
     *
     * The configuration register’s format is shown in the following Table:
     *
     *  BIT |    7   | 6 | 5 |  4 |  3  |  2  |   1  |   0
     * NAME | ST/BSY | 0 | 0 | SC | DR1 | DR0 | PGA1 | PGA0
     */
    public static final int CONFIG_DEFAULT = (int) 0b1000_1100;

    /**
     * Bits 1-0: PGA
     * Bits 1 and 0 control the ADS1100’s gain setting, as shown in the following Table:
     *
     * PGA1 | PGA0 | GAIN
     *   0  |   0  |   1 (default)
     *   0  |   1  |   2
     *   1  |   0  |   4
     *   1  |   1  |   8
     */
    public enum Gain {
        PGA_1(0b00, 1), // default
        PGA_2(0b01, 2),
        PGA_4(0b10, 4),
        PGA_8(0b11, 8),
        ;
        int config;
        int value;
        public int getValue() { return value; }
        Gain(int config, int value) {
            this.config = config;
            this.value = value;
        }
    }

    /**
     * Bits 3-2: DR
     * Bits 3 and 2 control the ADS1100’s data rate, as shown in following Table:
     *
     * DR1 | DR0 | DATA RATE
     *  0  |  0  | 128 SPS
     *  0  |  1  |  32 SPS
     *  1  |  0  |  16 SPS
     *  1  |  1  |   8 SPS (default)
     */
    public enum Rate {
        SPS_8  (0b1100,   8, 16, -32768, 32767), // default
        SPS_16 (0b1000,  16, 15, -16483, 16482),
        SPS_32 (0b0100,  32, 14,  -8192,  8191),
        SPS_128(0b0000, 128, 12,  -2048,  2047),
        ;
        int config;
        int sps;
        int nob;
        int min;
        int max;
        public int getSamplesPerSecond() { return sps; }
        public int getNumberOfBits() { return nob; }
        public int getMinimumCode() { return min; }
        public int getMaximumCode() { return max; }
        Rate(int config, int samplesPerSeconds, int numberOfBits, int minimumCode, int maximumCode) {
            this.config = config;
            this.sps = samplesPerSeconds;
            this.nob = numberOfBits;
            this.min = minimumCode;
            this.max = maximumCode;
        }
    }

    /**
     * Bit 4: SC
     * SC controls whether the ADS1100 is in continuous conversion or single conversion mode. When SC is 1, the ADS1100
     * is in single conversion mode; when SC is 0, the ADS1100 is in continuous conversion mode. The default is 0.
     */
    public enum Mode {
        CONTINUOUS(0b00000), // default
        SINGLE    (0b10000),
        ;
        int config;
        Mode(int config) {
            this.config = config;
        }
    }

    /**
     * Bits 6-5: Reserved
     * Bits 6 and 5 must be set to zero.
     */

    /**
     * Bit 7: ST/BSY
     * The meaning of the ST/BSY bit depends on whether it is being written to or read from.
     * In single conversion mode, writing a 1 to the ST/BSY bit causes a conversion to start, and writing a 0 has no
     * effect. In continuous conversion mode, the ADS1100 ignores the value written to ST/BSY.
     */
    public static final int CONVERSION = 0b1000_0000;

    /**
     * Address of I2C device.
     */
    private final Address address;

    private Gain gain = Gain.PGA_1;
    private Rate rate = Rate.SPS_8;
    private Mode mode = Mode.CONTINUOUS;

    /**
     * I2C bus number to use to access device.
     */
    private final int i2cBus;

    /**
     * Abstraction of I2C device.
     */
    private I2CDevice i2c;

    /**
     * Device supply voltage.
     */
    private float Vdd = 5f;

    /**
     * Read/write buffer.
     */
    private final byte[] BUFFER = new byte[3];

    public ADS1100(Address address) {
        this(I2CBus.BUS_1, address);
    }

    /**
     * Specific address constructor.
     *
     * @param bus
     * @param address I2C address
     */
    public ADS1100(int bus, Address address) {
        this.i2cBus = bus;
        this.address = address;
    }
    
    /**
     * Setup the sensor for general usage.
     * 
     * @throws IOException 
     */
    public ADS1100 setup() throws IOException {
        // http://pi4j.com/example/control.html
        i2c = I2CFactory.getInstance(i2cBus).getDevice(address.config);
        return this;
    }

    public ADS1100 setSupplyVoltage(float Vdd) {
        this.Vdd = Vdd;
        return this;
    }

    public float getSupplyVoltage() {
        return Vdd;
    }

    public ADS1100 setGain(Gain gain) {
        this.gain = gain;
        return this;
    }
    
    public Gain getGain() {
        return gain;
    }

    public ADS1100 setRate(Rate rate) {
        this.rate = rate;
        return this;
    }

    public Rate getRate() {
        return rate;
    }
    
    public ADS1100 setMode(Mode mode) {
        this.mode = mode;
        return this;
    }
    
    public Mode getMode() {
        return mode;
    }

    public int getConfig() {
        return gain.config
                | rate.config
                | mode.config;
    }
    
    public ADS1100 writeConfig() throws IOException {
        i2c.write((byte) getConfig());
        return this;
    }

    public int readConfig() throws IOException {
        final int offset = 0;
        final int size = 3;
        i2c.read(BUFFER, offset, size);
        return BUFFER[2];
    }

    public void startConversion() throws IOException {
        int config = CONVERSION | getConfig();
        i2c.write((byte) config);
    }

    public boolean isPerformingConversion() throws IOException {
        return (readConfig() & CONVERSION) != 0;
    }

    private int readOutputRegister() throws IOException {
        int offset = 0;
        int size = 2;
        i2c.read(BUFFER, offset, size);
        return (BUFFER[0] << 8) | (BUFFER[1] & 0xFF);
    }

    /**
     * Read the current voltage reading.
     *
     * @throws IOException
     */
    public float readVoltage() throws IOException {
        final float conversion = -1 * rate.min * gain.value;
        return readOutputRegister() / conversion * Vdd;
    }

    @Override
    public String toString() {
        return super.toString()
                + ": Vdd=" + Vdd
                + ", gain=" + gain
                + ", rate=" + rate
                + ", mode=" + mode;
    }
}
