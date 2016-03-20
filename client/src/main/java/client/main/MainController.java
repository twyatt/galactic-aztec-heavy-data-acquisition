package client.main;

import client.Launcher;
import com.badlogic.gdx.math.Vector3;
import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.*;
import edu.sdsu.rocket.core.models.Pressures;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("deprecation")
public class MainController {
	
	private static final double GAUGE_WIDTH  = 316;
	private static final double GAUGE_HEIGHT = 316;
	
	private static final double CHART_WIDTH  = GAUGE_WIDTH * 2;
	private static final double CHART_HEIGHT = 190;
	
	private static final long NANOSECONDS_PER_MILLISECOND = 1000000L;
	
	private static final boolean DEBUG_SENSORS = false;
	
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
	@FXML private Label gpsFixLabel;
	@FXML private Label gpsSatellitesLabel;
	@FXML private Label powerLabel;
	@FXML private FlowPane gaugePane;
	@FXML private BorderPane gpsBorderPane;
	
	@FXML private Label latitudeLabel;
	@FXML private Label longitudeLabel;
	@FXML private Label altitudeLabel;
	@FXML private Button zeroAltitudeButton;
	
	private GoogleMap map;
	
	private Gauge lox;
	private Gauge kerosene;
	private Gauge helium;
	private Gauge motor;
	
	private int chartIndex;
	private static final int ACCELEROMETER_DATA_POINTS = 50;
	private static final int GYROSCOPE_DATA_POINTS     = 50;
	private static final int MAGNETOMETER_DATA_POINTS  = 50;
	private static final int BAROMETER_DATA_POINTS     = 50;
	
	private NumberAxis accelerometerX;
	private Series<Number, Number> accelerometerXData = new XYChart.Series<>();
	private Series<Number, Number> accelerometerYData = new XYChart.Series<>();
	private Series<Number, Number> accelerometerZData = new XYChart.Series<>();
	
	private NumberAxis gyroscopeX;
	private Series<Number, Number> gyroscopeXData = new XYChart.Series<>();
	private Series<Number, Number> gyroscopeYData = new XYChart.Series<>();
	private Series<Number, Number> gyroscopeZData = new XYChart.Series<>();
	
	private NumberAxis magnetometerX;
	private Series<Number, Number> magnetometerXData = new XYChart.Series<>();
	private Series<Number, Number> magnetometerYData = new XYChart.Series<>();
	private Series<Number, Number> magnetometerZData = new XYChart.Series<>();
	
	private NumberAxis barometerX;
	private Series<Number, Number> barometerPressureData = new XYChart.Series<>();
	
	double altitudeZero = Double.NaN;

	private static final Format LATENCY_FORMAT = new DecimalFormat("#.#");
	private static final Format ALTITUDE_FORMAT = new DecimalFormat("#.##");
	
	private static final Vector3 tmpVec = new Vector3();
	
	private static final String ZERO_TEXT = "Zero";
	private static final String UNZERO_TEXT = "Unzero";

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
	}

	private void createSensors() {
		if (DEBUG_SENSORS) {
			motor    = makePressureGauge("Motor",    "mV", 5000, 100);
			lox      = makePressureGauge("LOX",      "mV", 5000, 100);
			kerosene = makePressureGauge("Kerosene", "mV", 5000, 100);
			helium   = makePressureGauge("Helium",   "mV", 5000, 100);
			
			List<Section> sections = new ArrayList<Section>();
			sections.add(new Section(3300, 3600));
			sections.add(new Section(3600, 5000));
			
			Gauge[] gauges = new Gauge[] { motor, lox, kerosene, helium };
			for (Gauge gauge : gauges) {
				gauge.setSections(sections);
				gauge.setSectionFill0(Color.YELLOW);
				gauge.setSectionFill1(Color.RED);
			}
		} else {
			lox      = makePressureGauge("LOX",      "PSI", Pressures.LOX_MAX_PRESSURE,      10);
			kerosene = makePressureGauge("Kerosene", "PSI", Pressures.KEROSENE_MAX_PRESSURE, 10);
			helium   = makePressureGauge("Helium",   "PSI", Pressures.HELIUM_MAX_PRESSURE,   50);
			motor    = makePressureGauge("Motor",    "PSI", Pressures.MOTOR_MAX_PRESSURE,    10);
			
			lox.setSections(new Section(450, 550), new Section(550, 610), new Section(610, Pressures.LOX_MAX_PRESSURE));
			lox.setSectionFill0(Color.GREEN);
			lox.setSectionFill1(Color.YELLOW);
			lox.setSectionFill2(Color.RED);
			
			kerosene.setSections(new Section(425, 500), new Section(500, 610), new Section(610, Pressures.KEROSENE_MAX_PRESSURE));
			kerosene.setSectionFill0(Color.GREEN);
			kerosene.setSectionFill1(Color.YELLOW);
			kerosene.setSectionFill2(Color.RED);
		}
		
		accelerometerX = new NumberAxis();
		NumberAxis accelerometerY = new NumberAxis();
		accelerometerX.setAutoRanging(false);
		accelerometerX.setTickLabelsVisible(false);
		accelerometerY.setLabel("Acceleration (m/s^2)");
		accelerometerY.setForceZeroInRange(true);
		LineChart<Number, Number> accelerometer = makeChart("Accelerometer", accelerometerX, accelerometerY);
		accelerometerXData.setName("X");
		accelerometerYData.setName("Y");
		accelerometerZData.setName("Z");
		accelerometer.getData().add(accelerometerXData);
		accelerometer.getData().add(accelerometerYData);
		accelerometer.getData().add(accelerometerZData);
		
		gyroscopeX = new NumberAxis();
		NumberAxis gyroscopeY = new NumberAxis();
		gyroscopeX.setAutoRanging(false);
		gyroscopeX.setTickLabelsVisible(false);
		gyroscopeY.setLabel("Rotation (deg/sec)");
		gyroscopeY.setForceZeroInRange(true);
		LineChart<Number, Number> gyroscope = makeChart("Gyroscope", gyroscopeX, gyroscopeY);
		gyroscopeXData.setName("X");
		gyroscopeYData.setName("Y");
		gyroscopeZData.setName("Z");
		gyroscope.getData().add(gyroscopeXData);
		gyroscope.getData().add(gyroscopeYData);
		gyroscope.getData().add(gyroscopeZData);
		
		magnetometerX = new NumberAxis();
		NumberAxis magnetometerY = new NumberAxis();
		magnetometerX.setAutoRanging(false);
		magnetometerX.setTickLabelsVisible(false);
		magnetometerY.setLabel("Field Strength (Ga)");
		magnetometerY.setForceZeroInRange(true);
		LineChart<Number, Number> magnetometer = makeChart("Magnetometer", magnetometerX, magnetometerY);
		magnetometerXData.setName("X");
		magnetometerYData.setName("Y");
		magnetometerZData.setName("Z");
		magnetometer.getData().add(magnetometerXData);
		magnetometer.getData().add(magnetometerYData);
		magnetometer.getData().add(magnetometerZData);
		
		barometerX = new NumberAxis();
		NumberAxis barometerY = new NumberAxis();
		barometerX.setAutoRanging(false);
		barometerX.setTickLabelsVisible(false);
		barometerY.setLabel("Pressure (mbar)");
		barometerY.setForceZeroInRange(false);
		LineChart<Number, Number> barometer = makeChart("Barometer", barometerX, barometerY);
		barometerPressureData.setName("Pressure");
		barometer.getData().add(barometerPressureData);
		
		gaugePane.getChildren().add(lox);
		gaugePane.getChildren().add(kerosene);
		gaugePane.getChildren().add(helium);
		gaugePane.getChildren().add(motor);
		gaugePane.getChildren().add(accelerometer);
		gaugePane.getChildren().add(gyroscope);
		gaugePane.getChildren().add(magnetometer);
		gaugePane.getChildren().add(barometer);
	}

	private LineChart<Number, Number> makeChart(String title, NumberAxis x, NumberAxis y) {
		LineChart<Number, Number> chart = new LineChart<>(x, y);
		chart.setTitle(title);
		chart.setCreateSymbols(false);
		chart.setAnimated(false);
		chart.setHorizontalZeroLineVisible(true);
		chart.setLegendSide(Side.RIGHT);
		chart.setPrefWidth(CHART_WIDTH);
		chart.setPrefHeight(CHART_HEIGHT);
		return chart;
	}

	private Gauge makePressureGauge(String label, String unit, double maxValue, double minorTickSpace) {
		return GaugeBuilder.create()
				.prefWidth(GAUGE_WIDTH).prefHeight(GAUGE_HEIGHT)
				.styleClass("gauge")
				.title(label)
				.unit(unit)
				.minValue(0)
				.maxValue(maxValue)
				.majorTickSpace(minorTickSpace * 10)
				.minorTickSpace(minorTickSpace)
				.minMeasuredValueVisible(true)
				.maxMeasuredValueVisible(true)
				.animated(false)
				.decimals(1)
				.build();
	}
	
	protected void updateLatency(float latency) {
		latencyLabel.setText(LATENCY_FORMAT.format(latency) + " ms");
	}
	
	@FXML
	public void clearSensors() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				motor.setValue(0);
				lox.setValue(0);
				kerosene.setValue(0);
				helium.setValue(0);
				
				accelerometerXData.getData().clear();
				accelerometerYData.getData().clear();
				accelerometerZData.getData().clear();
				gyroscopeXData.getData().clear();
				gyroscopeYData.getData().clear();
				gyroscopeZData.getData().clear();
				magnetometerXData.getData().clear();
				magnetometerYData.getData().clear();
				magnetometerZData.getData().clear();
				barometerPressureData.getData().clear();
				
				signalLabel.setText("?");
				gpsFixLabel.setText("?");
				gpsSatellitesLabel.setText("?");
				powerLabel.setText("?");
				
				latitudeLabel.setText("?");
				longitudeLabel.setText("?");
				altitudeLabel.setText("?");
			}
		});
	}
	
	public void updateSensors() {
		// toolbar
		updateSignalStrength();
		updateGPSToolbar(sensors);
		updatePower(sensors);
		
		// gauges
		updatePressures(sensors);
		
		// charts
		chartIndex++;
		updateAccelerometer(sensors);
		updateGyroscope(sensors);
		updateMagnetometer(sensors);
		updateBarometer(sensors);
		
		// GPS
		updateGPS();
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

	private void updateGPSToolbar(Sensors sensors) {
		switch (sensors.gps.getFixStatus()) {
		case 1: // no fix
			gpsFixLabel.setText("No Fix");
			break;
		case 2: // 2D
			gpsFixLabel.setText("2D");
			break;
		case 3: // 3D
			gpsFixLabel.setText("3D");
			break;
		}
		gpsSatellitesLabel.setText(""+sensors.gps.getSatellites());
	}

	private void updateGPS() {
		String postfix;
		
		double altitudeMeters = sensors.gps.getAltitude();
		if (Double.isNaN(altitudeZero)) {
			postfix = "MSL";
		} else {
			altitudeMeters -= altitudeZero;
			postfix = "AGL";
		}
		double altitudeFeet = altitudeMeters / 0.3048f;
		latitudeLabel.setText(""+ sensors.gps.getLatitude());
		longitudeLabel.setText(""+ sensors.gps.getLongitude());
		altitudeLabel.setText(ALTITUDE_FORMAT.format(altitudeMeters) + " m (" + ALTITUDE_FORMAT.format(altitudeFeet) + " ft) " + postfix);
	}
	
	private void updatePower(Sensors sensors) {
		if (sensors.system.getIsPowerGood()) {
			powerLabel.setText("GOOD");
		} else {
			powerLabel.setText("BAD");
		}
	}

	private void updateBarometer(Sensors sensors) {
		float pressure = sensors.barometer.getPressure();
		barometerX.setLowerBound(chartIndex - BAROMETER_DATA_POINTS + 1);
		barometerX.setUpperBound(chartIndex);
		while (barometerPressureData.getData().size() >= BAROMETER_DATA_POINTS) {
			barometerPressureData.getData().remove(0);
		}
		barometerPressureData.getData().add(new XYChart.Data<>(chartIndex, pressure));
	}

	private void updateMagnetometer(Sensors sensors) {
		Vector3 magnetometer = tmpVec;
		sensors.magnetometer.get(magnetometer);
		magnetometerX.setLowerBound(chartIndex - MAGNETOMETER_DATA_POINTS + 1);
		magnetometerX.setUpperBound(chartIndex);
		while (magnetometerXData.getData().size() >= MAGNETOMETER_DATA_POINTS) {
			magnetometerXData.getData().remove(0);
		}
		while (magnetometerYData.getData().size() >= MAGNETOMETER_DATA_POINTS) {
			magnetometerYData.getData().remove(0);
		}
		while (magnetometerZData.getData().size() >= MAGNETOMETER_DATA_POINTS) {
			magnetometerZData.getData().remove(0);
		}
		magnetometerXData.getData().add(new XYChart.Data<>(chartIndex, magnetometer.x));
		magnetometerYData.getData().add(new XYChart.Data<>(chartIndex, magnetometer.y));
		magnetometerZData.getData().add(new XYChart.Data<>(chartIndex, magnetometer.z));
	}

	private void updateGyroscope(Sensors sensors) {
		Vector3 gyroscope = tmpVec;
		sensors.gyroscope.get(gyroscope);
		gyroscopeX.setLowerBound(chartIndex - GYROSCOPE_DATA_POINTS + 1);
		gyroscopeX.setUpperBound(chartIndex);
		while (gyroscopeXData.getData().size() >= GYROSCOPE_DATA_POINTS) {
			gyroscopeXData.getData().remove(0);
		}
		while (gyroscopeYData.getData().size() >= GYROSCOPE_DATA_POINTS) {
			gyroscopeYData.getData().remove(0);
		}
		while (gyroscopeZData.getData().size() >= GYROSCOPE_DATA_POINTS) {
			gyroscopeZData.getData().remove(0);
		}
		gyroscopeXData.getData().add(new XYChart.Data<>(chartIndex, gyroscope.x));
		gyroscopeYData.getData().add(new XYChart.Data<>(chartIndex, gyroscope.y));
		gyroscopeZData.getData().add(new XYChart.Data<>(chartIndex, gyroscope.z));
	}

	private void updateAccelerometer(Sensors sensors) {
		Vector3 accelerometer = tmpVec;
		sensors.accelerometer.get(accelerometer);
		accelerometer.scl(9.8f);
		accelerometerX.setLowerBound(chartIndex - ACCELEROMETER_DATA_POINTS + 1);
		accelerometerX.setUpperBound(chartIndex);
		while (accelerometerXData.getData().size() >= ACCELEROMETER_DATA_POINTS) {
			accelerometerXData.getData().remove(0);
		}
		while (accelerometerYData.getData().size() >= ACCELEROMETER_DATA_POINTS) {
			accelerometerYData.getData().remove(0);
		}
		while (accelerometerZData.getData().size() >= ACCELEROMETER_DATA_POINTS) {
			accelerometerZData.getData().remove(0);
		}
		accelerometerXData.getData().add(new XYChart.Data<>(chartIndex, accelerometer.x));
		accelerometerYData.getData().add(new XYChart.Data<>(chartIndex, accelerometer.y));
		accelerometerZData.getData().add(new XYChart.Data<>(chartIndex, accelerometer.z));
	}

	private void updatePressures(Sensors sensors) {
		if (DEBUG_SENSORS) {
			motor.setValue(sensors.analog.get(0));
			lox.setValue(sensors.analog.get(1));
			kerosene.setValue(sensors.analog.get(2));
			helium.setValue(sensors.analog.get(3));
		} else {
			motor.setValue(sensors.pressures.getMotor());
			lox.setValue(sensors.pressures.getLOX());
			kerosene.setValue(sensors.pressures.getKerosene());
			helium.setValue(sensors.pressures.getHelium());
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
	
	@FXML
	private void onLoadMap(ActionEvent event) {
		final double latitude = sensors.gps.getLatitude();
		final double longitude = sensors.gps.getLongitude();
		
		final GoogleMapView mapView = new GoogleMapView();
		mapView.addMapInializedListener(new MapComponentInitializedListener() {
			@Override
			public void mapInitialized() {
				LatLong center;
				if (latitude != 0 && longitude != 0) {
					center = new LatLong(latitude, longitude);
				} else {
					// Friends of Amateur Rocketry
					center = new LatLong(35.347218, -117.808392);
				}
				
		        MapOptions mapOptions = new MapOptions();
		        mapOptions.center(center)
		                .mapType(MapTypeIdEnum.SATELLITE)
		                .overviewMapControl(false)
		                .panControl(false)
		                .rotateControl(false)
		                .scaleControl(false)
		                .streetViewControl(false)
		                .zoomControl(true)
		                .zoom(16);
		        map = mapView.createMap(mapOptions);
			}
		});
		gpsBorderPane.setCenter(mapView);
	}
	
	@FXML
	private void onCopyLatitude(ActionEvent event) {
		Clipboard clipboard = Clipboard.getSystemClipboard();
		ClipboardContent content = new ClipboardContent();
		content.putString(latitudeLabel.getText());
		clipboard.setContent(content);
	}
	
	@FXML
	private void onCopyLongitude(ActionEvent event) {
		Clipboard clipboard = Clipboard.getSystemClipboard();
		ClipboardContent content = new ClipboardContent();
		content.putString(longitudeLabel.getText());
		clipboard.setContent(content);
	}
	
	@FXML
	private void onCenter(ActionEvent event) {
		if (map == null) return;
		
		try {
			double latitude = Double.valueOf(latitudeLabel.getText());
			double longitude = Double.valueOf(longitudeLabel.getText());
			
			LatLong position = new LatLong(latitude, longitude);
			map.setCenter(position);
		} catch (NumberFormatException e) {
			System.err.println(e);
		}
	}
	
	@FXML
	private void onAddMarker(ActionEvent event) {
		if (map == null) return;
		
		try {
			double latitude = Double.valueOf(latitudeLabel.getText());
			double longitude = Double.valueOf(longitudeLabel.getText());
			
			MarkerOptions options = new MarkerOptions();
			options.position(new LatLong(latitude, longitude));
			Marker marker = new Marker(options);
			map.addMarker(marker);
		} catch (NumberFormatException e) {
			System.err.println(e);
		}
	}
	
	@FXML
	private void onZeroAltitude(ActionEvent event) {
		if (ZERO_TEXT.equals(zeroAltitudeButton.getText())) {
			zeroAltitudeButton.setText(UNZERO_TEXT);
			altitudeZero = sensors.gps.getAltitude();
		} else { // UNZERO_TEXT
			zeroAltitudeButton.setText(ZERO_TEXT);
			altitudeZero = Double.NaN;
		}
		updateGPS();
	}

	/**
	 * Determines if a shutdown (quit) process should commence.
	 * 
	 * @return
	 */
	public boolean requestQuit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Launcher.NAME);
        alert.setHeaderText("Quit");
        alert.setContentText("Are you sure you want to quit?");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
	}

}
