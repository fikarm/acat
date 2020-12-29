package acat.controller;

import acat.Acat;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.testfx.framework.junit.ApplicationTest;

import java.net.URL;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzeTest extends ApplicationTest {

    AcatMock acatMock = new AcatMock();
    MainMock mainMock;

    @Override
    public void start(Stage stage) throws Exception {

        acatMock.start( stage );
        mainMock = new MainMock( stage );
        acatMock.injectAnalyzeWith( mainMock );

    }

    @Test
    public void harusnyaMunculAlertKetikaKelasPreprocessException() {

        // set to null
        mainMock.outputPP = null;

        // invoke preprocess method
        clickOn( "#btnAnalyze" );

        try { // try to interact() with an error alert

            interact(() -> {

                // get front most stage which is an alert
                Stage alert = ((Stage)((lookup(".error").query())).getScene().getWindow());

                // print all the child component of the stage
                alert.getScene().getRoot().getChildrenUnmodifiable().forEach(System.out::println);

                // get the content component of the alert
                System.out.println("*** \n" + alert.getScene().lookup(".label.content").toString());

                Assert.assertEquals("Something wrong", alert.getTitle());

            });

        } catch ( Exception e ) {

            Assert.fail("Alert tidak muncul.");

        }

    }

    public void showErrorAlert(){

        Platform.runLater(() -> { // to run some codes in JavaFX thread

            mainMock.showAlert(
                    Alert.AlertType.ERROR,
                    "Coba Show Error Alert",
                    "Alhamdulillah bisa"
            );

        });

    }

}

// mock Acat class to run just Analyze View
class AcatMock extends Application {

    public Analyze analyze;

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Load main fxml
        URL mainFxml = Acat.class.getResource("/views/analyze.fxml");
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(mainFxml);
        Parent root = loader.load();

        // Analyze class reference
        analyze = loader.getController();

        primaryStage.setTitle( "Aggregated Coupling Analysis Tool v1.0" );
        primaryStage.setScene( new Scene( root ) );
        primaryStage.getIcons().add( new Image( "images/logo.png" ) );
        primaryStage.show();

    }

    public void injectAnalyzeWith(MainMock mainMock ) {

        analyze.main = mainMock;

    }

}

class MainMock extends Main {

    public Stage stage;
    public Alert alert;

    public MainMock( Stage stage ) {

        this.stage = stage;
        alert = new Alert(Alert.AlertType.NONE);
        alert.setHeaderText(null);
        alert.initStyle(StageStyle.UTILITY);

    }

    // user defined method
    public void showAlert(Alert.AlertType type, String title, String content){

        alert.setAlertType(type);
        alert.setTitle(title);
        alert.setContentText(content);

    }

    public void unbindProgressBar() {}

    public void bindProgressBar(ReadOnlyDoubleProperty rodp, ReadOnlyStringProperty rosp){}

    public void stopTimer() {}

    public void startTimer() {}

}