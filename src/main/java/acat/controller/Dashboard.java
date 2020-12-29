package acat.controller;

import acat.Acat;
import acat.algorithm.ExplorerTask;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;


public class Dashboard extends IController{

    @FXML   VBox    vbRootHome;
    private boolean isAccepted;
    private File    input;

    // Event handler
    @FXML
    public void dragEntered(DragEvent event) throws MalformedURLException {

        input = new File( new URL(event.getDragboard().getString()).getPath() );

        if (input.isDirectory()) {

            isAccepted = true;
            event.acceptTransferModes(TransferMode.ANY);
            getRoot().setStyle("-fx-background-color: #B3D4FF;");

        } else {

            isAccepted = false;
            event.acceptTransferModes(TransferMode.NONE);
            getRoot().setStyle("-fx-background-color: #FFBDAD;");

        }

        event.consume();
    }

    @FXML
    public void dragOver(DragEvent event) {

        if(isAccepted)

            event.acceptTransferModes(TransferMode.ANY);

        event.consume();

    }

    @FXML
    public void dragDropped(DragEvent event) {

        if (isAccepted)

            firstCheck(input.toPath());

        event.setDropCompleted(true);
        event.consume();

    }

    @FXML
    public void dragExited(DragEvent event) {

        getRoot().setStyle("-fx-background-color: transparent;");

        event.consume();

    }

    @FXML
    public void openFolder() {

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Java Project Directory");

        input = chooser.showDialog( Acat.stage );

        if (input != null) firstCheck( input.toPath() );

    }


    // User defined method
    private void firstCheck(Path path) {

        if (isLoaded)

            main.showAlert(
                    Alert.AlertType.WARNING,
                    "Hold on",
                    "You have to close the opened project first. " +
                            "Button to close the project is on the bottom left menu.");

        else

            secondCheck(path);

    }

    private void secondCheck(Path path) {

        try {

            if (Files.walk(path).anyMatch(p->p.toString().endsWith(".java"))) {

                main.lbPath.setText(path.toString());
                scanFolder(path);

            } else {

                main.showAlert(
                        Alert.AlertType.WARNING,
                        "Oops",
                        "Seems the folder has no java files in it. " +
                                "Please try again with appropriate folders.");
            }

        } catch (IOException e) {

            e.printStackTrace();
            main.showAlert(Alert.AlertType.WARNING, "Something wrong", e.getMessage());

        }

    }

    private void scanFolder(Path path) {

        getRoot().setDisable(true);

        ExplorerTask task = new ExplorerTask(path);

        task.setOnSucceeded(event -> {

            main.endProgress();

            main.treeItem = task.getValue();
            main.entities = task.getJavaFiles();

            main.showScreen("analyze");

            getRoot().setDisable(false);

        });

        task.setOnFailed(event -> {

            main.endProgress();

            main.showAlert(
                    Alert.AlertType.ERROR,
                    "Something wrong",
                    event.getSource().getException().getMessage());

            getRoot().setDisable(false);

        });

        main.startProgress( task.messageProperty() );

        // Start the Task.
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

    }


    // IController's abstracts methods
    @Override
    void load() {}

    @Override
    Pane getRoot() { return vbRootHome; }

    @Override
    String getTitle() {
        return "Dashboard";
    }

}
