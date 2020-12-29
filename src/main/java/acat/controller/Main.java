package acat.controller;

import acat.Acat;
import acat.model.Entity;
import javafx.animation.AnimationTimer;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

public class Main implements Initializable {

    //View variables
    @FXML Button        btnMenuDashboard,
                        btnMenuAnalyze,
                        btnMenuVisualize,
                        btnMenuDetails,
                        btnCancelTask;
    @FXML IController   dashboardController,
                        analyzeController,
                        visualizeController,
                        detailsController;
    @FXML Label         lbTitle,
                        lbPath,
                        lbTime,
                        lbStatus,
                        lbLog;
    @FXML ProgressBar   pbStatus;
    @FXML VBox          vbSidebar;


    //Class Variables
    List<Entity> entities;
    TreeItem<Entity> treeItem; //tree of files & folders
    String outputPP, outputPV, outputAC, outputSOM;
    public  Path output;
    private Alert alert;
    private IController active;
    private AnimationTimer timer;
    private long startTime;

    @Override
    public void initialize( URL location, ResourceBundle resources ) {

        try {

            active = dashboardController;

            // inject main controller
            dashboardController.main = this;
            analyzeController.main = this;
            visualizeController.main = this;
            detailsController.main = this;

            // hide other screens
            analyzeController.getRoot().setVisible(false);
            visualizeController.getRoot().setVisible(false);
            detailsController.getRoot().setVisible(false);

            // create an instance of alert
            alert = new Alert(Alert.AlertType.NONE);
            alert.setHeaderText(null);
            alert.initStyle(StageStyle.UTILITY);

            // set output folder
            output      = Files.createTempDirectory("acat_");
            outputPP    = output + "/corpus.txt";
            outputPV    = output + "/pv.txt";
            outputAC    = output + "/ac.txt";
            outputSOM   = output + "/som.txt";

            lbLog.setText(output.toString());
            lbPath.setText("");

            timer = new AnimationTimer() {

                @Override
                public void start() {
                    super.start();
                    startTime = System.currentTimeMillis();
                }

                @Override
                public void handle( long now ) {

                    long ms = System.currentTimeMillis() - startTime;
                    long s  = ms / 1000;
                    long m  = s / 60;

                    lbTime.setText( String.format( "%02dm %02ds %03dms", m, s % 60, ms % 1000 ) );

                }

            };

        } catch ( IOException e ) {

            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Something wrong", e.getMessage());

        }

    }

    @FXML
    public void clickMenu( ActionEvent event ) {

        try {

            Button source = (Button)event.getSource();

            showScreen( source.getAccessibleText() );

        } catch ( Exception ignored ) {

        }

    }

    @FXML
    public void closeProject() {

        Acat.reload();

    }

    @FXML
    public void exit() {

        Acat.stage.close();

    }


    public List<Entity> getEntities() {

        return entities;

    }


    public void showAlert(Alert.AlertType type, String title, String content) {

        alert.setAlertType(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();

    }

    public void updateResult() {

        // update the table on the details page when reanalyze is done.
        ( ( Details ) detailsController ).updateTable();

        // memicu SOM dijalankan ulang
        visualizeController.isLoaded = false;

    }


    public void showScreen( String name ) {

        try {

            enableMenu( name );
            active.getRoot().setVisible(false);
            active = getControllerOf( name );
            active.init();

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public void enableMenu( String name ) {

        try {

            getButtonOf( name ).setDisable(false);

        } catch ( Exception e ) {

            e.printStackTrace();

        }

    }

    private IController getControllerOf( String name ) throws Exception {

        switch ( name ) {

            case "dashboard": return dashboardController;
            case "analyze": return analyzeController;
            case "details": return detailsController;
            case "visualize": return visualizeController;
            default: throw new Exception("Controller doesn't recognized.");

        }

    }

    private Button getButtonOf( String menu ) throws Exception {

        switch ( menu ) {

            case "dashboard": return btnMenuDashboard;
            case "analyze": return btnMenuAnalyze;
            case "details": return btnMenuDetails;
            case "visualize": return btnMenuVisualize;
            default: throw new Exception("Button doesn't recognized.");

        }

    }


    /**
     * Task handling functions
     */
    //TODO: current state --> implementasi fitur cancelling task
    //TODO: task ini nanti bisa diseragamkan untuk onsucceed dan onfailednya.

    private Task<?> activeTask;

    public void assignTask( Task<?> task ) {

        // assign a task instance
        activeTask = task;

        // assign listener to the active task
        activeTask.stateProperty().addListener( ( ob, ov, nv ) ->  {

            if ( nv.equals( Worker.State.SUCCEEDED ) )  endProgress();
            else if ( nv.equals( Worker.State.CANCELLED ) ) endProgress();
            else if ( nv.equals( Worker.State.FAILED ) ) {
                endProgress();
                showAlert(Alert.AlertType.ERROR, "Something wrong", activeTask.getException().toString());
            }

        } );

    }

    public void startTask() {

        // bind to the status bar and start the timer
        startProgress( activeTask.messageProperty() );

        // create and run the task on a new thread
        Thread thread = new Thread(activeTask);
        thread.setDaemon( true );
        thread.start();

    }

    @FXML
    public void cancelTask() {

        // send cancel signal to the task
        activeTask.cancel();

        endProgress();

        lbStatus.setText( "task cancelled." );

    }

    public void startProgress( ReadOnlyStringProperty stringProperty ) {

        // release binding
        endProgress();

        // bind
        lbStatus.textProperty().bind( stringProperty );
        pbStatus.setVisible( true );
        btnCancelTask.setVisible( true );
        timer.start();

    }

    public void endProgress() {

        lbStatus.textProperty().unbind();
        pbStatus.setVisible( false );
        btnCancelTask.setVisible( false );
        timer.stop();

    }





}
