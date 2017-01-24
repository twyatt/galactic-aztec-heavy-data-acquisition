package client.main;

import edu.sdsu.rocket.core.helpers.ValueTranslator;
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
    private double rawValue;

    // gauge
    private Gauge gauge;

    // chart
    private int numberOfDataPoints;
    private LineChart<Number, Number> chart;
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
        numberOfDataPoints = (int) ((NumberAxis) chart.getXAxis()).getUpperBound();

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

    public void setValue(double value) {
        this.rawValue = value;
        updateValue();
    }

    private void updateValue() {
        float value = mode == Mode.TRANSLATED && translator != null
                ? translator.translate((float) rawValue)
                : (float) rawValue;

        if (gauge != null) {
            gauge.setValue(value);
        }
        if (chart != null) {
            addChartValue(value);
        }
    }

    private void clearChart() {
        chart.getData().setAll(Collections.singleton(new XYChart.Series<>()));
    }

    public void addChartValue(float value) {
        XYChart.Series<Number, Number> series = chart.getData().get(0);
        while (series.getData().size() > numberOfDataPoints) {
            series.getData().remove(0);
        }

        reindexData();
        series.getData().add(new XYChart.Data<>(series.getData().size(), value));
    }

    /**
     * Sets the X values to match their indices.
     */
    private void reindexData() {
        XYChart.Series<Number, Number> series = chart.getData().get(0);
        for (int i = 0; i < series.getData().size(); i++) {
            series.getData().get(i).setXValue(i);
        }
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

        gauge.setMinValue(settings.minValue);
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
        double valueRange = settings.maxValue - settings.minValue;

        chart.setTitle(label);
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        yAxis.setLabel(settings.unit);
        yAxis.setLowerBound(settings.minValue);
        yAxis.setUpperBound(settings.maxValue);
        if (settings.majorTickSpace > valueRange) {
            yAxis.setTickUnit(settings.majorTickSpace / 10);
        } else {
            yAxis.setTickUnit(settings.majorTickSpace);
        }
        double majorTickCount = valueRange / settings.majorTickSpace;
        int minorTickCount = (int) (valueRange / majorTickCount / settings.minorTickSpace);
        yAxis.setMinorTickCount(minorTickCount);
    }

}
