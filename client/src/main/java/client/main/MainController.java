package client.main;

import client.Launcher;
import edu.sdsu.rocket.core.helpers.PressureValueTranslatorFactory;
import edu.sdsu.rocket.core.models.Sensors;
import edu.sdsu.rocket.core.net.SensorClient;
import eu.hansolo.enzo.common.Section;
import eu.hansolo.enzo.gauge.Gauge;
import eu.hansolo.enzo.gauge.GaugeBuilder;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
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

    public static final long PING_INTERVAL = 1000L;
    public static final long STATUS_INTERVAL = 5000L;

    public static final Color WARNING_COLOR = Color.YELLOW;
    public static final Color DANGER_COLOR  = Color.RED;

    private static final int SENSOR_COLUMNS = 3;
    private GaugeController[] gaugeControllers;

    private static final String CONNECT    = "Connect";
    private static final String DISCONNECT = "Disconnect";

    private static final int PORT = 4444;
    private final Sensors sensors = new Sensors();
    private final SensorClient client = new SensorClient(sensors);
    private Thread pingThread;
    private Thread statusThread;

    @FXML private TextField hostTextField;
    @FXML private Button connectButton;
    @FXML private Slider frequencySlider;
    @FXML private Label frequencyLabel;
    @FXML private Label latencyLabel;
    @FXML private Label temperatureLabel;
    @FXML private Label signalLabel;
    @FXML private GridPane gaugePane;
    @FXML private GridPane chartPane;

    private static final Format LATENCY_FORMAT = new DecimalFormat("#.#");
    private static final Format TEMPERATURE_FORMAT = new DecimalFormat("#.#");

    /**
     * Constructor for the controller.
     * 
     * Called prior to the initialize() method.
     */
    public MainController() {
        client.setListener(new SensorClient.SensorClientListener() {
            @Override
            public void onPingResponse(final long latency) {
                Platform.runLater(() -> updateLatency((float) latency / NANOSECONDS_PER_MILLISECOND));
            }
            
            @Override
            public void onSensorsUpdated(byte mask) {
                Platform.runLater(() -> updateSensors(mask));
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
        frequencySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int value = newValue.intValue();
            frequencyLabel.setText(String.valueOf(value));
            client.setFrequency(value);
        });
        
        setupGauges();
        loadSettings();
    }

    private void setupGauges() {
        final GaugeSettings voltageSettingsADS1114 = new GaugeSettings()
                .setUnit("mV")
                .setMaxValue(5500)
                .setMinorTickSpace(50)
                .setMajorTickSpace(500)
                .setSections(new ArrayList<Section>() {{
                    add(new Section(5000, 5300, WARNING_COLOR));
                    add(new Section(5300, 5500, DANGER_COLOR));
                }});
        final GaugeSettings voltageSettingsADS1100 = new GaugeSettings()
                .setUnit("mV")
                .setMaxValue(5500)
                .setMinorTickSpace(50)
                .setMajorTickSpace(500)
                .setSections(new ArrayList<Section>() {{
                    add(new Section(5000, 5200, WARNING_COLOR));
                    add(new Section(5200, 5500, DANGER_COLOR));
                }});

        gaugeControllers = new GaugeController[] {
                new GaugeController("LOX")
                        .setTranslator(PressureValueTranslatorFactory.getLOX())
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1114)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(750)
                                .setMinorTickSpace(10)
                                .setMajorTickSpace(100)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(600, 700, WARNING_COLOR));
                                        add(new Section(700, 750, DANGER_COLOR));
                                }})),

                new GaugeController("Kerosene")
                        .setTranslator(PressureValueTranslatorFactory.getKerosene())
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1114)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(750)
                                .setMinorTickSpace(10)
                                .setMajorTickSpace(100)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(3300, 3600, WARNING_COLOR));
                                        add(new Section(3600, 5000, DANGER_COLOR));
                                }})),

                new GaugeController("Helium")
                        .setTranslator(PressureValueTranslatorFactory.getHelium())
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1114)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(2500)
                                .setMinorTickSpace(20)
                                .setMajorTickSpace(200)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(3300, 3600, WARNING_COLOR));
                                        add(new Section(3600, 5000, DANGER_COLOR));
                                }})),

                new GaugeController("Motor")
                        .setTranslator(PressureValueTranslatorFactory.getMotor())
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1114)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(500)
                                .setMinorTickSpace(10)
                                .setMajorTickSpace(100)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(3300, 3600, Color.YELLOW));
                                        add(new Section(3600, 5000, DANGER_COLOR));
                                }})),

                new GaugeController("RCS Low")
                        .setTranslator(PressureValueTranslatorFactory.getRcsLow())
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1100)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(800)
                                .setMinorTickSpace(10)
                                .setMajorTickSpace(100)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(3300, 3600, WARNING_COLOR));
                                        add(new Section(3600, 5000, DANGER_COLOR));
                                }})),

                new GaugeController("RCS High")
                        .setTranslator(PressureValueTranslatorFactory.getRcsHigh())
                        .putSettings(GaugeController.Mode.RAW, voltageSettingsADS1100)
                        .putSettings(GaugeController.Mode.TRANSLATED, new GaugeSettings()
                                .setUnit("PSI")
                                .setMaxValue(4000)
                                .setMinorTickSpace(50)
                                .setMajorTickSpace(500)
                                .setSections(new ArrayList<Section>() {{
                                        add(new Section(3300, 3600, WARNING_COLOR));
                                        add(new Section(3600, 5000, DANGER_COLOR));
                                }})),
        };

        for (int i = 0; i < gaugeControllers.length; i++) {
            GaugeController controller = gaugeControllers[i];

            int row = i / SENSOR_COLUMNS;
            int col = i % SENSOR_COLUMNS;
            Gauge gauge = makePressureGauge(controller.getLabel(), controller.getActiveSettings());
            controller.setGauge(gauge);

            gaugePane.add(gauge, col, row);

            LineChart<Number, Number> chart = (LineChart<Number, Number>) chartPane.getChildren().get(i);
            controller.setChart(chart);
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

    private void updateLatency(float latency) {
        latencyLabel.setText(LATENCY_FORMAT.format(latency));
    }
    
    private void updateSensors(byte mask) {
        if ((mask & Sensors.RADIO_MASK) != 0) {
            updateSignalStrength();
        }

        if ((mask & Sensors.ANALOG_MASK) != 0) {
            updateGauges();
        }

        if ((mask & Sensors.SYSTEM_MASK) != 0) {
            updateTemperature();
        }
    }

    private void updateTemperature() {
        if (sensors.system.getRawTemperature() == 0) {
            temperatureLabel.setText("?");
        } else {
            try {
                temperatureLabel.setText(TEMPERATURE_FORMAT.format(sensors.system.getTemperatureF()));
            } catch (IllegalArgumentException e) {
                System.err.println("Failed to format temperature value for display: " + e);
            }
        }
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

    private void updateGauges() {
        for (int i = 0; i < gaugeControllers.length; i++) {
            if (i >= sensors.analog.count) {
                break;
            }
            GaugeController gaugeController = gaugeControllers[i];
            gaugeController.setValue(sensors.analog.get(i));
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
        for (GaugeController gaugeController : gaugeControllers) {
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

                startPingThread();
                startStatusThread();

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
            stopPingThread();
            stopStatusThread();

            client.stop();

            latencyLabel.setText("?");
            temperatureLabel.setText("?");
            connectButton.setText(CONNECT);
        }
        
        event.consume();
    }

    private void startStatusThread() {
        stopStatusThread();

        statusThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(STATUS_INTERVAL);
                } catch (InterruptedException e) {
                    System.err.println(e);
                    return;
                }

                try {
                    client.sendStatusRequest();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        });
        statusThread.setName("Status Request");
        statusThread.start();
    }

    private void stopStatusThread() {
        if (statusThread != null) {
            statusThread.interrupt();
            try {
                statusThread.join();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
            statusThread = null;
        }
    }

    private void startPingThread() {
        stopPingThread();

        pingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(PING_INTERVAL);
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
        });
        pingThread.setName("Ping Request");
        pingThread.start();
    }

    private void stopPingThread() {
        if (pingThread != null) {
            pingThread.interrupt();
            try {
                pingThread.join();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
            pingThread = null;
        }
    }

    private void saveSettings() {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.put("host", hostTextField.getText());
        prefs.putInt("frequency", (int) frequencySlider.getValue());
    }

    private void loadSettings() {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        hostTextField.setText(prefs.get("host", "raspberrypi"));
        frequencySlider.setValue(prefs.getInt("frequency", 1));
    }

    /**
     * Determines if a shutdown (quit) process should commence.
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
