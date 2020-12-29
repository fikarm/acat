package acat.controller;

import acat.algorithm.AggregateCoupling;
import acat.algorithm.Doc2Vec;
import acat.algorithm.Preprocessor;
import acat.model.Entity;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.util.List;


public class Analyze extends IController {

    @FXML private Button                            btnAnalyze;
    @FXML private CheckBox                          cbPVHS;
    @FXML private HBox                              hbRootAnalyze;
    @FXML private FlowPane                          fpButtons;
    @FXML private Slider                            slWeight;
    @FXML private TextField                         tfPVLayer;
    @FXML private TextField                         tfPVWindow;
    @FXML private TextField                         tfPVIter;
    @FXML private TextField                         tfPVAlpha;
    @FXML private TextField                         tfPVNS;
    @FXML private TextField                         tfPVSample;
    @FXML private TreeTableView<Entity>             ttvResult;
    @FXML private TreeTableColumn<Entity, String>   ttcClass;
    @FXML private TreeTableColumn<Entity, String>   ttcInitial;


    @Override
    void load() {

        // fill the parameter value
        resetParameter();

        // populate table
        ttcClass.setCellValueFactory( param ->
                new ReadOnlyStringWrapper( param.getValue().getValue().name ));
        ttcClass.setCellFactory( ttc ->
                new TreeTableCell<Entity, String>() {

                    final ImageView pkgico  = new ImageView( getClass()
                            .getResource( "/images/ico-package.png" )
                            .toExternalForm() );
                    final ImageView fileico = new ImageView( getClass()
                            .getResource( "/images/ico-java.png" )
                            .toExternalForm() );

                    @Override
                    protected void updateItem( String item, boolean empty ) {

                        pkgico.setFitHeight( 15 );
                        pkgico.setFitWidth( 15 );
                        fileico.setFitHeight( 15 );
                        fileico.setFitWidth( 15 );

                        super.updateItem( item, empty );
                        setText( item );

                        if ( empty )

                            setGraphic( null );

                        else if ( item.endsWith( ".java" ) )

                            setGraphic( fileico );

                        else

                            setGraphic( pkgico );

                    }

                } );


        ttcInitial.setCellValueFactory( param -> new ReadOnlyStringWrapper( param.getValue().getValue().initial ) );

        ttvResult.setRoot( main.treeItem );

    }

    @Override
    Pane getRoot() { return hbRootAnalyze; }

    @Override
    String getTitle() {

        return "Analyze";

    }

    @FXML
    private void resetParameter() {

        slWeight.setValue( 0.5 );
        tfPVLayer.setText( "300" );
        tfPVWindow.setText( "5" );
        tfPVIter.setText( "100" );
        tfPVAlpha.setText( "0.05" );
        tfPVNS.setText( "20" );
        tfPVSample.setText( "1e-5" );
        cbPVHS.setSelected( true );

    }

    @FXML
    private void openVisualize() {

        main.showScreen( "visualize" );

    }

    @FXML
    private void openDetails() {

        main.showScreen( "details" );

    }

    @FXML
    private void startAnalyze() {

        //params
        final float acWeight = ( float ) slWeight.getValue();
        final float pvSubsample = Float.parseFloat( tfPVSample.getText() );
        final float pvAlpha = Float.parseFloat( tfPVAlpha.getText() );
        final int pvLayer = Integer.parseInt( tfPVLayer.getText() );
        final int pvNS = Integer.parseInt( tfPVNS.getText() );
        final int pvWindow = Integer.parseInt( tfPVWindow.getText() );
        final int pvIter = Integer.parseInt( tfPVIter.getText() );
        final boolean useHS = cbPVHS.isSelected();
        final List<Entity> entities = main.getEntities();
        final String outputPP = main.outputPP;
        final String outputPV = main.outputPV;
        final String outputAC = main.outputAC;

        // TODO: variabel argumen belum immutable
        Preprocessor prep = new Preprocessor( entities, outputPP,
                false, true, true, false,
                0, 0 );
        Doc2Vec pv = new Doc2Vec( outputPP, outputPV, pvLayer, pvWindow,
                pvIter, pvAlpha, pvSubsample, pvNS, useHS );
        // TODO: WARNING! membaca outputPV dari fail dapat mengurangi akurasi perhitungan ACE
        AggregateCoupling ace = new AggregateCoupling( main.getEntities(), outputPV, outputAC, acWeight );

        Task<Void> ukurKoplingTask = new Task<Void>() {

            @Override
            protected Void call() throws Exception {

                prep.message.addListener( ( ob, ov, nv ) -> updateMessage( nv ) );
                pv.message.addListener( ( ob, ov, nv ) -> updateMessage( nv ) );
                ace.message.addListener( ( ob, ov, nv ) -> updateMessage( nv ) );

                prep.run();
                pv.TrainModel();
                ace.run();

                return null;

            }

        };

        ukurKoplingTask.setOnCancelled( event -> {

            pv.stop();
            ace.stop();
            btnAnalyze.setDisable( false );
            btnAnalyze.setText( "Analyze" );

        });

        ukurKoplingTask.setOnSucceeded( event -> {

            // menampilkan hasil perhitungan ACE
            showResult();

            // mengubah teks dari tombol analyze menjadi reanalyze
            btnAnalyze.setText( "Reanalyze" );
            btnAnalyze.setDisable( false );

            // menampilkan tombol navigasi ke halaman Details dan Visualize
            fpButtons.setVisible( true );
            fpButtons.setDisable( false );

            // enable tombol menu details and analysis
            main.enableMenu( "details" );
            main.enableMenu( "visualize" );

            // update halaman details dan visualize karena reanalyze
            main.updateResult();

        } );

        main.assignTask( ukurKoplingTask );
        main.startTask();

        // disable tombol Analyze dan mengubah isi teksnya
        btnAnalyze.setDisable( true );
        btnAnalyze.setText( "please wait ..." );

        // menyembunyikan tombol navigasi untuk ke halaman Details dan Visualize
        fpButtons.setDisable( true );
        fpButtons.setVisible( false );

    }

    private void showResult() {

        // create new result column
        TreeTableColumn<Entity, String> ttcResult = new TreeTableColumn<>( "ACE" );
        ttcResult.setMinWidth( 150 );
        ttcResult.setSortable( false );
        ttcResult.setEditable( false );
        ttcResult.setCellValueFactory( treeItem -> {

            Entity e = treeItem.getValue().getValue();
            String val = e.isFile ? String.format( "%.5f", e.ace ) : "";
            return new ReadOnlyStringWrapper( val );

        } );

        // remove the previous result column
        ttvResult.getColumns().remove( 2 );

        // replace with the new one
        ttvResult.getColumns().add( ttcResult );

    }

}
