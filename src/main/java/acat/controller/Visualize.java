package acat.controller;

import acat.model.Hexagon;
import acat.algorithm.SOM;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Map;

public class Visualize extends IController {

    @FXML private HBox          hbRoot;
    @FXML private Pane          hexGrid;  // hexagonal grid container

    private Hexagon[][]         hexagons; // for ease referencing
    private Map<String, Label>  labels;   // store UI labels

    @Override
    void load() {

        // this condition is required to prevent adding more hexgrid
        // to the view as recalculate is performed
        if ( hexGrid.getChildren().isEmpty() ) {

            createHexGrid( 50, 50, 700 );
            createLabel();

        }

        startSOM();

    }

    @Override
    Pane getRoot() {
        return hbRoot;
    }

    @Override
    String getTitle() {
        return "Visualization";
    }

    @FXML
    private void startSOM() {

        Task<Void> somTask = new Task<Void>() {

            @Override
            protected Void call() {

                SOM som = new SOM( extractAC(), extractTag(), 50 );


                int interval = (int) (som.maxIter - ( som.maxIter * ( 0.999 ) ) );

                som.iter.addListener( (ob, ov, nv) -> {

                    if ( nv.intValue() % interval == 0 ) {

                        updateMessage(
                                String.format( "SOM training iteration %,d / %,d", som.maxIter, nv.intValue() )
                        );

                        Platform.runLater( () -> {

                            drawUMatrix( som.getUMatrix() );
                            updateLabel( som.getBMU() );

                        });

                    }

                });

                som.run();

                updateMessage( "SOM finished." );

                return null;

            }

        };

        somTask.setOnSucceeded( event -> main.endProgress() );
        somTask.setOnFailed( event -> {main.endProgress();
            System.out.println(event.getSource().getException().getMessage());} );

        main.startProgress( somTask.messageProperty() );

        Thread thread = new Thread( somTask );
        thread.setDaemon(true);
        thread.start();

    }

    /**
     * Get aggregated coupling values of each entities
     */
    private double[][] extractAC() {

        int n = main.entities.size();
        double[][] ac = new double[n][n];

        for ( int i = 0; i < n; i++ )

            for ( int j = 0; j < n; j++ )

                ac[i][j] = main.entities.get( i ).ac.get( j );


        return ac;

    }

    /**
     * Get string arrays of entities initial names
     */
    private String[] extractTag() {

        String[] tags = new String[ main.entities.size() ];

        for ( int i = 0; i < main.entities.size(); i++ )

            tags[i] = main.entities.get( i ).initial;

        return tags;

    }

    /**
     * Create hexagonal grid inside a Pane.
     * Also populate the hexagons array.
     */
    private void createHexGrid( int cols, int rows, double totalWidth ) {

        // initialize
        hexagons = new Hexagon[cols][rows];

        // required variables
        double  width       = totalWidth / ( cols + 0.5 ),
                radius      = width / 1.732,
                vspace      = radius * 1.5,
                halfWidth   = width / 2,
                totalHeight = radius * ( rows * 1.5 + 0.5 ),
                startx;

        // create hexagonal grid
        for ( int y = 0; y < rows; y++ ) {

            // make even rows offset
            startx = ( y & 1 ) == 0 ? width : halfWidth;

            for ( int x = 0; x < cols; x++ ) {

                Hexagon hex = new Hexagon(
                        radius,
                        startx + width * x ,
                        radius + vspace * y
                );
                hexGrid.getChildren().add( hex );
                hexagons[x][y] = hex;

            }

        }

        // these properties are key points to avoid wiggle effect
        hexGrid.setMinSize( totalWidth, totalHeight );
        hexGrid.setMaxSize( totalWidth, totalHeight );
        hexGrid.setPrefSize( totalWidth, totalHeight );

    }

    /**
     * Create UI Label for each entity.
     */
    private void createLabel() {

        labels   = new HashMap<>();

        main.entities.forEach( entity -> {

            Label label = new Label( entity.initial );
            label.setTextFill( entity.color );
            label.setFont(Font.font("sans-serif", FontWeight.BOLD, 15));
            labels.put( entity.initial, label );
            hexGrid.getChildren().add(label);

        });

    }

    /**
     * Update the Label position based on BMU index
     */
    private void updateLabel( Map<String, int[]> labelBMU ) {

        labelBMU.forEach( ( name, bmu ) -> {

            Hexagon hex = hexagons[bmu[0]][bmu[1]];
            labels.get( name ).setLayoutX( hex.centerx );
            labels.get( name ).setLayoutY( hex.centery );

        });

    }

    private void drawUMatrix( double[][] umatrix ) {

        for( int x = 0; x < umatrix.length; x++ )

            for( int y = 0; y < umatrix[0].length; y++ )

                hexagons[x][y].setFill( Color.gray( umatrix[x][y] ) );


    }

}
