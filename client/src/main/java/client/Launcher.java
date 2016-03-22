package client;

import client.main.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Launcher extends Application {

	public static final String NAME = "Data Acquisition Client";

	@Override
	public void start(Stage stage) throws Exception {
		// http://stackoverflow.com/a/19603055
		URL resource = getClass().getResource("/MainPane.fxml");
		FXMLLoader loader = new FXMLLoader(resource);
		Parent root = loader.load();
		
		final MainController controller = loader.getController();
		Scene scene = new Scene(root);
		
        stage.setMinWidth(640);
        stage.setMinHeight(480);
		stage.setWidth(1024);
		stage.setHeight(768);
		stage.setTitle(NAME);
		stage.setScene(scene);
		stage.show();
		
		scene.getWindow().setOnCloseRequest(event -> {
            if (controller.requestQuit()) {
                Platform.exit();
                System.exit(0);
            } else {
                event.consume();
            }
        });
	}
	
	public static void main(String[] args) {
		Application.launch(args);
	}

}
