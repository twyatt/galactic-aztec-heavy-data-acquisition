package client.main;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class GraphController {

    private static final int DATA_POINTS = 50;

    private final XYChart.Series<Number, Number> xData = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> yData = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> zData = new XYChart.Series<>();

    private NumberAxis xAxis;
    private NumberAxis yAxis;

    private int chartIndex;

    public GraphController() {
        xData.setName("X");
        yData.setName("Y");
        zData.setName("Z");
    }

    public void bind(LineChart<Number, Number> graph) {
        xAxis = (NumberAxis) graph.getXAxis();
        yAxis = (NumberAxis) graph.getYAxis();

        graph.getData().add(xData);
        graph.getData().add(yData);
        graph.getData().add(zData);
    }

    public void update(float x, float y, float z) {
        chartIndex++;

        xAxis.setLowerBound(chartIndex - DATA_POINTS + 1);
        xAxis.setUpperBound(chartIndex);

        while (xData.getData().size() >= DATA_POINTS) {
            xData.getData().remove(0);
        }
        while (yData.getData().size() >= DATA_POINTS) {
            yData.getData().remove(0);
        }
        while (zData.getData().size() >= DATA_POINTS) {
            zData.getData().remove(0);
        }

        xData.getData().add(new XYChart.Data<>(chartIndex, x));
        yData.getData().add(new XYChart.Data<>(chartIndex, y));
        zData.getData().add(new XYChart.Data<>(chartIndex, z));
    }

}
