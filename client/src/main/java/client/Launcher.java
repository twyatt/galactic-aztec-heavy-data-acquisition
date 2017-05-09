package client;

import client.main.MainController;
import edu.sdsu.rocket.core.BuildConfig;
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
        controller.setStage(stage);
        Scene scene = new Scene(root);

        // 16:9 aspect ratios
        // https://en.wikipedia.org/wiki/16:9
        stage.setMinWidth(720);
        stage.setMinHeight(405);
        stage.setWidth(1280);
        stage.setHeight(720);

        stage.setTitle(NAME + " " + BuildConfig.VERSION);
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
