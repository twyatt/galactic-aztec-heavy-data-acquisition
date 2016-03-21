package client.main;

import client.Launcher;
import edu.sdsu.rocket.core.models.Sensors;
import edu.sdsu.rocket.core.net.SensorClient;
import eu.hansolo.enzo.common.Section;
import eu.hansolo.enzo.gauge.Gauge;
import eu.hansolo.enzo.gauge.GaugeBuilder;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Optional;
import java.util.prefs.Preferences;

public class MainController {
    
    private static final long NANOSECONDS_PER_MILLISECOND = 1000000L;

    private static final int SENSOR_COLUMNS = 3;
    private GaugeController[] gaugeControllers;

    private static final String CONNECT    = "Connect";
    private static final String DISCONNECT = "Disconnect";

    private static final int PORT = 4444;
    private final Sensors sensors = new Sensors();
    private final SensorClient client = new SensorClient(sensors);
    private Thread pingThread;

    @FXML private TextField hostTextField;
    @FXML private Button connectButton;
    @FXML private Slider frequencySlider;
    @FXML private Label frequencyLabel;
    @FXML private Label latencyLabel;
    @FXML private Label signalLabel;
    @FXML private Label powerLabel;
    @FXML private GridPane gaugePane;

    private int chartIndex;
    private static final int BAROMETER_DATA_POINTS     = 50;
    
    private NumberAxis barometerX;
    private Series<Number, Number> barometerPressureData = new XYChart.Series<>();
    
    private static final Format LATENCY_FORMAT = new DecimalFormat("#.#");

    /**
     * Constructor for the controller.
     * 
     * Called prior to the initialize() method.
     */
    public MainController() {
        client.setListener(new SensorClient.SensorClientListener() {
            @Override
            public void onPingResponse(final long latency) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        updateLatency((float) latency / NANOSECONDS_PER_MILLISECOND);
                    }
                });
            }
            
            @Override
            public void onSensorsUpdated() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        updateSensors();
                    }
                });
            }
        });
    }
    
    /**
     * Initialize the controller.
     * 
     * Automatically called after the FXML view has been loaded.
     * 
     * Configures the child Life Line controller/view.
     */
    @FXML
    private void initialize() {
        frequencySlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                int value = newValue.intValue();
                frequencyLabel.setText(value + " Hz");
                client.setFrequency(value);
            }
        });
        
        createSensors();
        loadSettings();
    }

    private void createSensors() {
        final GaugeSettings voltageSettingsADS1114 = new GaugeSettings()
                .setUnit("mV")
                .setMaxValue(5500)
                .setMinorTickSpace(50)
                .setMajorTickSpace(500)
                .setSections(new ArrayList<Section>() {{
                    add(new Section(5000, 5300, Color.YELLOW));
                    add(new Section(5300, 5500, Color.RED));
                }});
        final GaugeSettings voltageSettingsADS1100 = new GaugeSettings()
                .setUnit("mV")
                .setMaxValue(5500)
                .setMinorTickSpace(50)
                .setMajorTickSpace(500)
                .setSections(new ArrayList<Section>() {{
                    add(new Section(5000, 5200, Color.YELLOW));
                    add(new Section(5200, 5500, Color.RED));
                }});

        gaugeControllers = new GaugeController[] {
                new GaugeController("LOX")
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1114)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(750)
                                .setMinorTickSpace(10)
                                .setMajorTickSpace(100)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(600, 700, Color.YELLOW));
                                        add(new Section(700, 750, Color.RED));
                                }})),

                new GaugeController("Kerosene")
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1114)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(750)
                                .setMinorTickSpace(10)
                                .setMajorTickSpace(100)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(3300, 3600, Color.YELLOW));
                                        add(new Section(3600, 5000, Color.RED));
                                }})),

                new GaugeController("Helium")
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1114)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(2500)
                                .setMinorTickSpace(20)
                                .setMajorTickSpace(200)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(3300, 3600, Color.YELLOW));
                                        add(new Section(3600, 5000, Color.RED));
                                }})),

                new GaugeController("Motor")
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1114)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(500)
                                .setMinorTickSpace(10)
                                .setMajorTickSpace(100)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(3300, 3600, Color.YELLOW));
                                        add(new Section(3600, 5000, Color.RED));
                                }})),

                new GaugeController("RCS Low")
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1100)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(800)
                                .setMinorTickSpace(10)
                                .setMajorTickSpace(100)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(3300, 3600, Color.YELLOW));
                                        add(new Section(3600, 5000, Color.RED));
                                }})),

                new GaugeController("RCS High")
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1100)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(4000)
                                .setMinorTickSpace(50)
                                .setMajorTickSpace(500)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(3300, 3600, Color.YELLOW));
                                        add(new Section(3600, 5000, Color.RED));
                                }})),
        };

        for (int i = 0; i < gaugeControllers.length; i++) {
            GaugeController controller = gaugeControllers[i];

            int row = i / SENSOR_COLUMNS;
            int col = i % SENSOR_COLUMNS;
            Gauge gauge = makePressureGauge(controller.getLabel(), controller.getActiveSettings());
            controller.setGauge(gauge);

            gaugePane.add(gauge, col, row);
        }
    }

    private Gauge makePressureGauge(String label, GaugeSettings settings) {
        return GaugeBuilder.create()
                .styleClass("gauge")
                .title(label)
                .unit(settings.unit)
                .minValue(0)
                .maxValue(settings.maxValue)
                .majorTickSpace(settings.majorTickSpace)
                .minorTickSpace(settings.minorTickSpace)
                .minMeasuredValueVisible(true)
                .maxMeasuredValueVisible(true)
                .animated(false)
                .decimals(1)
                .build();
    }

    private LineChart<Number, Number> makeChart(String title, NumberAxis x, NumberAxis y) {
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setTitle(title);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setHorizontalZeroLineVisible(true);
        chart.setLegendSide(Side.RIGHT);
        return chart;

//        NumberAxis axisX = new NumberAxis();
//        axisX.setAutoRanging(false);
//        axisX.setTickLabelsVisible(false);
//
//        NumberAxis axisY = new NumberAxis();
//        axisY.setLabel("Pressure (PSI)");
//        axisY.setForceZeroInRange(true);
//        LineChart<Number, Number> chart = makeChart(label, axisX, axisY);
    }

    protected void updateLatency(float latency) {
        latencyLabel.setText(LATENCY_FORMAT.format(latency) + " ms");
    }
    
    @FXML
    public void clearSensors() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                barometerPressureData.getData().clear();
                
                signalLabel.setText("?");
                powerLabel.setText("?");
            }
        });
    }
    
    public void updateSensors() {
        // toolbar
        updateSignalStrength();
        updatePower(sensors);
        
        // gauges
        updatePressures(sensors);
        
        // charts
        chartIndex++;
        updateBarometer(sensors);
    }

    private void updateSignalStrength() {
        if (sensors.radio.getSignalStrength() == 0) {
            signalLabel.setText("?");
        } else {
            try {
                signalLabel.setText("-" + sensors.radio.getSignalStrength());
            } catch (IllegalArgumentException e) {
                System.err.println("Failed to format signal strength value for display: " + e);
            }
        }
    }

    private void updatePower(Sensors sensors) {
        if (sensors.system.getIsPowerGood()) {
            powerLabel.setText("GOOD");
        } else {
            powerLabel.setText("BAD");
        }
    }

    private void updateBarometer(Sensors sensors) {
//        float pressure = sensors.barometer.getPressure();
//        barometerX.setLowerBound(chartIndex - BAROMETER_DATA_POINTS + 1);
//        barometerX.setUpperBound(chartIndex);
//        while (barometerPressureData.getData().size() >= BAROMETER_DATA_POINTS) {
//            barometerPressureData.getData().remove(0);
//        }
//        barometerPressureData.getData().add(new XYChart.Data<>(chartIndex, pressure));
    }

    private void updatePressures(Sensors sensors) {
        for (int i = 0; i < gaugeControllers.length; i++) {
            GaugeController gaugeController = gaugeControllers[i];
            Gauge gauge = gaugeController.getGauge();
            if (i >= sensors.analog.count) {
                break;
            }
            gauge.setValue(sensors.analog.get(i));
        }
    }

    @FXML
    private void onDisplayPSI(ActionEvent event) {
        setMode(GaugeController.Mode.TRANSLATED);
        event.consume();
    }

    @FXML
    private void onDisplayMillivolts(ActionEvent event) {
        setMode(GaugeController.Mode.RAW);
        event.consume();
    }

    private void setMode(GaugeController.Mode mode) {
        for (int i = 0; i < gaugeControllers.length; i++) {
            GaugeController gaugeController = gaugeControllers[i];
            gaugeController.setMode(mode);
        }
    }

    @FXML
    private void onConnect(ActionEvent event) {
        if (CONNECT.equals(connectButton.getText())) {
            try {
                InetAddress addr = InetAddress.getByName(hostTextField.getText());
                client.setFrequency((float) frequencySlider.getValue());
                client.start(addr, PORT);

                pingThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!Thread.currentThread().isInterrupted()) {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                System.err.println(e);
                                return;
                            }

                            try {
                                client.sendPingRequest();
                            } catch (IOException e) {
                                System.err.println(e);
                            }
                        }
                    }
                });
                pingThread.start();

                connectButton.setText(DISCONNECT);
            } catch (IOException e) {
                e.printStackTrace();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(Launcher.NAME);
                alert.setHeaderText("Failed to connect.");
                alert.setContentText(e.toString());
                alert.showAndWait();
            }
        } else {
            pingThread.interrupt();
            try {
                pingThread.join();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
            pingThread = null;

            client.stop();

            latencyLabel.setText("?");
            connectButton.setText(CONNECT);
        }
        
        event.consume();
    }

    private void saveSettings() {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.put("host", hostTextField.getText());
    }

    private void loadSettings() {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        hostTextField.setText(prefs.get("host", "raspberrypi"));
    }

    /**
     * Determines if a shutdown (quit) process should commence.
     * 
     * @return
     */
    public boolean requestQuit() {
        saveSettings();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Launcher.NAME);
        alert.setHeaderText("Quit");
        alert.setContentText("Are you sure you want to quit?");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

}
