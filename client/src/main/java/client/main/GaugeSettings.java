package client.main;

import eu.hansolo.enzo.common.Section;

import java.util.List;

public class GaugeSettings {

    String unit;
    double maxValue;
    List<Section> sections;
    double minorTickSpace;
    double majorTickSpace;

    public GaugeSettings setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    public GaugeSettings setMaxValue(double maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    public GaugeSettings setSections(List<Section> sections) {
        this.sections = sections;
        return this;
    }

    public GaugeSettings setMinorTickSpace(double minorTickSpace) {
        this.minorTickSpace = minorTickSpace;
        return this;
    }

    public GaugeSettings setMajorTickSpace(double majorTickSpace) {
        this.majorTickSpace = majorTickSpace;
        return this;
    }

}
