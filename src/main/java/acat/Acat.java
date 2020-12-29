package acat;

import acat.controller.Main;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class Acat extends Application {

    private static Main main;
    public static Stage stage;


    @Override
    public void start(Stage primaryStage) {

        // set reference to primaryStage
        stage = primaryStage;

        // Stage
        primaryStage.setTitle("Aggregated Coupling Analysis Tool v1.0");
        primaryStage.setScene(new Scene(load()));
        primaryStage.getIcons().add(new Image("images/logo.png"));
        primaryStage.show();

    }

    @Override
    public void stop() throws Exception {

        super.stop();

        clearTemp();

    }


    public  static void main(String[] args) {
        launch(args);
    }

    public  static void reload() {

        clearTemp();

        stage.getScene().setRoot(load());

    }

    private static Parent load() {

        try {

            // Load main fxml
            URL mainFxml = Acat.class.getResource("/views/main.fxml");
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(mainFxml);
            Parent root = loader.load();

            // get reference to the Main controller
            main = loader.getController();


            return root;

        } catch (Exception e) {

            e.printStackTrace();

            return null;

        }

    }

    private static void clearTemp() {

        try {

            // delete temporary folder recursively
            Files.walk(main.output)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

}