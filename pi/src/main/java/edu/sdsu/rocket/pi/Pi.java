package edu.sdsu.rocket.pi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Pi {

    // cat /sys/class/thermal/thermal_zone0/temp
    private static final String CPU_TEMPERATURE_DEVICE = "/sys/class/thermal/thermal_zone0/temp";
    
    // degrees celsius
    public static float getCpuTemperatureC() throws IOException {
        return (float) getRawCpuTemperature() / 1000f;
    }

    public static float fahrenheitFromCelsius(float celsius) throws IOException {
        return celsius * 9f / 5f + 32f;
    }
    
    public static int getRawCpuTemperature() throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(CPU_TEMPERATURE_DEVICE));
            int temperature = Integer.valueOf(in.readLine());
            return temperature;
        } catch (FileNotFoundException e) {
            System.err.println(e);
            return 0;
        } catch (NumberFormatException e) {
            System.err.println(e);
            return 0;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
    
}
