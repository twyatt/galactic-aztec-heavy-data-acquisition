package client.main;

import edu.sdsu.rocket.core.helpers.ValueTranslator;
import eu.hansolo.enzo.common.Section;
import eu.hansolo.enzo.gauge.Gauge;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;

import java.util.*;

public class GaugeController {

    // common
    public enum Mode {
        RAW,
        TRANSLATED,
    }
    private Mode mode = Mode.TRANSLATED;
    private final Map<Mode, GaugeSettings> settingsMap = new HashMap<>();
    private ValueTranslator translator;
    private final String label;
    private float rawValue;

    // gauge
    private Gauge gauge;

    // chart
    private double numberOfDataPoints;
    private LineChart<Number, Number> chart;
    private int x;
    private Color chartColor;

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

    public GaugeController setChart(LineChart<Number, Number> chart) {
        this.chart = chart;
        numberOfDataPoints = ((NumberAxis) chart.getXAxis()).getUpperBound();

        clearChart();
        updateChart();

        return this;
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
            update();
        }
    }

    private void update() {
        updateGauge();
        clearChart();
        updateChart();
    }

    public GaugeController setTranslator(ValueTranslator translator) {
        this.translator = translator;
        return this;
    }

    public void setValue(float value) {
        this.rawValue = value;
        updateValue();
    }

    private void updateValue() {
        float value = mode == Mode.TRANSLATED && translator != null
                ? translator.translate(rawValue)
                : rawValue;

        if (gauge != null) {
            gauge.setValue(value);
        }
        if (chart != null) {
            addChartValue(value);
        }
    }

    private void clearChart() {
        x = 0;
        chart.getData().setAll(Collections.singleton(new XYChart.Series<>()));
    }

    public void addChartValue(float value) {
        x++;

        NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        xAxis.setLowerBound(x - numberOfDataPoints + 1);
        xAxis.setUpperBound(x);
        while (chart.getData().size() >= numberOfDataPoints) {
            chart.getData().remove(0);
        }
        XYChart.Series<Number, Number> series = chart.getData().get(0);
        series.getData().add(new XYChart.Data<>(x, value));
//
//        GaugeSettings settings = getActiveSettings();
//        if (settings.sections != null) {
//            for (Section section : settings.sections) {
//                if (value > section.getStart() && value < section.getStop()) {
//                    setChartLineColor(section.getColor());
//                    return;
//                }
//            }
//        }
//        setChartLineColor(Color.BLACK);
    }

    private void setChartLineColor(Color color) {
        if (color != null || !color.equals(this.chartColor)) {
            this.chartColor = color;
            chart.applyCss();

            for (int i = 0; i < chart.getData().size(); i++) {
                Set<Node> nodes = chart.lookupAll(".series" + i);
                for (Node n : nodes) {
                    n.setStyle("-fx-stroke: " + toRgb(color) + ";");
                }
            }
        }
    }

    public static String toRgb(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return "rgb(" + r + ", " + g + ", " + b + ")";
    }

    private void updateGauge() {
        GaugeSettings settings = getActiveSettings();

        gauge.setMaxValue(settings.maxValue);
        updateValue();
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

    private void updateChart() {
        GaugeSettings settings = getActiveSettings();

        chart.setTitle(label);
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        yAxis.setLabel(settings.unit);
        yAxis.setUpperBound(settings.maxValue);
        yAxis.setTickUnit(settings.majorTickSpace);
        int majorTickCount = (int) (settings.maxValue / settings.majorTickSpace);
        int minorTickCount = (int) (settings.maxValue / majorTickCount / settings.minorTickSpace);
        yAxis.setMinorTickCount(minorTickCount);
    }

}
