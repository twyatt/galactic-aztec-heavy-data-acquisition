package client.main;

import eu.hansolo.enzo.gauge.Gauge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GaugeController {

    private final Map<Mode, GaugeSettings> settingsMap = new HashMap<>();

    public enum Mode {
        RAW,
        TRANSLATED,
    }
    private Mode mode = Mode.TRANSLATED;

    private final String label;
    private Gauge gauge;

    public GaugeController(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public GaugeController setGauge(Gauge gauge) {
        this.gauge = gauge;
        updateGauge();
        return this;
    }

    public Gauge getGauge() {
        return gauge;
    }

    public GaugeController putSettings(Mode mode, GaugeSettings settings) {
        settingsMap.put(mode, settings);
        return this;
    }

    public GaugeSettings getActiveSettings() {
        return settingsMap.get(mode);
    }

    public void setMode(Mode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            updateGauge();
        }
    }

    private void updateGauge() {
        GaugeSettings settings = getActiveSettings();

        double ratio = gauge.getValue() / gauge.getMaxValue();

        gauge.setMaxValue(settings.maxValue);
        double value = gauge.getValue() * ratio;
        gauge.setValue(value);

        gauge.setMajorTickSpace(settings.majorTickSpace);
        gauge.setMinorTickSpace(settings.minorTickSpace);
        if (settings.sections == null) {
            gauge.setSections(new ArrayList<>());
        } else {
            gauge.setSections(settings.sections);
        }
        gauge.setUnit(settings.unit);

        // hack to force redraw of unit text
        boolean interactive = gauge.isInteractive();
        gauge.setInteractive(!interactive);
        gauge.setInteractive(interactive);

        gauge.resetMinAndMaxMeasuredValue();
    }

}
