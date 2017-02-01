package client.main;

import client.Launcher;
import edu.sdsu.rocket.core.helpers.*;
import edu.sdsu.rocket.core.io.devices.PhidgetBridgeOutputStream;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class MainController {

    private static final long NANOSECONDS_PER_MILLISECOND = 1000000L;

    public static final long PING_INTERVAL = 1000L; // ms
    public static final long STATUS_INTERVAL = 5000L; // ms

    public static final Color WARNING_COLOR = Color.YELLOW;
    public static final Color DANGER_COLOR  = Color.RED;

    private GaugeController[] gaugeControllers;

    private static final String CONNECT    = "Connect";
    private static final String DISCONNECT = "Disconnect";
    private static final String RECORD     = "Record";
    private static final String STOP       = "Stop";

    private static final int PORT = 4445;
    private final Sensors sensors = new Sensors();
    private final SensorClient client = new SensorClient(sensors);
    private Thread pingThread;
    private Thread statusThread;

    private volatile boolean isRecording;
    private Logger logger;
    private PhidgetBridgeOutputStream log;

    private Stage stage;
    @FXML private TextField hostTextField;
    @FXML private Button connectButton;
    @FXML private Button recordButton;
    @FXML private Slider frequencySlider;
    @FXML private Label frequencyLabel;
    @FXML private Label latencyLabel;
    @FXML private Label temperatureLabel;
    @FXML private Label signalLabel;
    @FXML private GridPane gridPane;

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

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void setupGauges() {
        final GaugeSettings settingsMillivoltsPerVolt = new GaugeSettings()
                .setUnit("mV/V")
                .setMinValue(-1)
                .setMaxValue(8)
                .setMinorTickSpace(1)
                .setMajorTickSpace(10)
                .setSections(new ArrayList<Section>() {{
                    add(new Section(7, 7.8125, WARNING_COLOR));
                    add(new Section(7.8125, 8, DANGER_COLOR));
                }});

        /* Futek LCF450 http://www.futek.com/product.aspx?stock=FSH00130
         *
         * Rated Output = 2 mV/V (i.e. output @ capacity)
         * Capacity = 2000 lb
         *
         * Calibration Data:
         * Output (mV/V)   Load (lb)
         *     0.0              0
         *     0.4627         400
         *     0.9262         800
         *     1.3901        1200
         *     1.8542        1600
         *     2.3176        2000
         *
         * Linear curve fit: y = 862.791 * x + 0.485
         * https://docs.google.com/spreadsheets/d/1XMN06cI92ah1l_Vc1dmJLuunMsgse7e8xNSH-bHrNUc/edit?usp=sharing
         */
        final ValueTranslator forceTranslator = new ValueTranslator(862.791f, 0.485f);
        final GaugeSettings settingsForce = new GaugeSettings()
                .setUnit("lb")
                .setMinValue(-100)
                .setMaxValue(3100)
                .setMinorTickSpace(100)
                .setMajorTickSpace(500)
                .setSections(new ArrayList<Section>() {{
                    add(new Section(2000, 3000, WARNING_COLOR));
                    add(new Section(3000, 3100, DANGER_COLOR));
                }});

        // for debugging (using 5kg load cell, 1.0946 mV/V)
//        final ValueTranslator forceTranslator = new ValueTranslator(25000f / 5473f, 0f);
//        final GaugeSettings settingsForce = new GaugeSettings()
//                .setUnit("kg")
//                .setMaxValue(8)
//                .setMinorTickSpace(1)
//                .setMajorTickSpace(10)
//                .setSections(new ArrayList<Section>() {{
//                    add(new Section(5, 7.5, WARNING_COLOR));
//                    add(new Section(7.5, 8, DANGER_COLOR));
//                }});

        gaugeControllers = new GaugeController[] {
                new GaugeController("Channel 0")
                        .setTranslator(forceTranslator)
                        .putSettings(GaugeController.Mode.RAW, settingsMillivoltsPerVolt)
                        .putSettings(GaugeController.Mode.TRANSLATED, settingsForce),
        };

        GaugeController controller = gaugeControllers[0];

        LineChart<Number, Number> chart = (LineChart<Number, Number>) gridPane.getChildren().get(0);
        controller.setChart(chart);

        Gauge gauge = makePressureGauge(controller.getLabel(), controller.getActiveSettings());
        controller.setGauge(gauge);
        gridPane.add(gauge, 0, 0);
    }

    private Gauge makePressureGauge(String label, GaugeSettings settings) {
        return GaugeBuilder.create()
                .styleClass("gauge")
                .title(label)
                .unit(settings.unit)
                .minValue(settings.minValue)
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

        if ((mask & Sensors.PHIDGETS_MASK) != 0) {
            onPhidgetsSensorsUpdated();
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

    private void onPhidgetsSensorsUpdated() {
        for (int i = 0; i < sensors.analog.length; i++) {
            AtomicIntDouble.IntDoubleValuePair pair = sensors.phidgets.get();
            double value = pair.doubleValue;

            if (i < gaugeControllers.length) {
                GaugeController gaugeController = gaugeControllers[i];
                gaugeController.setValue(value);
            }

            if (isRecording) {
                long timestamp = TimeUnit.MILLISECONDS.toNanos(pair.intValue);
                writeSensor(timestamp, value);
            }
        }
    }

    private void writeSensor(long timestamp, double value) {
        try {
            log.writeValue(timestamp, value);
        } catch (IOException e) {
            e.printStackTrace();

            stopRecording();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(Launcher.NAME);
            alert.setHeaderText("Recording failed.");
            alert.setContentText(e.getMessage());
            alert.show();
        }
    }

    @FXML
    private void onDisplayLb(ActionEvent event) {
        setMode(GaugeController.Mode.TRANSLATED);
        event.consume();
    }

    @FXML
    private void onDisplayMillivoltsPerVolt(ActionEvent event) {
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

    @FXML
    private void onRecord(ActionEvent event) {
        if (RECORD.equals(recordButton.getText())) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        final Preferences prefs = Preferences.userRoot().node(this.getClass().getName());

        String logDir = prefs.get("logDir", null);
        if (logDir == null) {
            logDir = System.getProperty("user.home");
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(logDir));
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            String path = selectedDirectory.getAbsolutePath();

            File dir = new File(path);
            List<File> dirs = Collections.singletonList(dir);
            logger = new Logger(dirs);

            try {
                log = new PhidgetBridgeOutputStream(logger.create("loadcell.log"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();

                logger.close();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(Launcher.NAME);
                alert.setHeaderText("Unable to start recording.");
                alert.setContentText(e.getMessage());
                alert.show();

                return;
            }

            prefs.put("logDir", path);

            recordButton.setText(STOP);
            isRecording = true;
        }
    }

    private void stopRecording() {
        isRecording = false;

        recordButton.setText(RECORD);

        if (logger != null) {
            logger.close();
            logger = null;
        }
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
