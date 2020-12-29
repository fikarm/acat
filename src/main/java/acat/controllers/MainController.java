package acat.controllers;

import com.github.acat2.models.Entity;
import com.github.acat2.models.FileType;
import com.github.acat2.models.Hexagon;
import com.github.acat2.services.ACE;
import com.github.acat2.services.FileVisitor;
import com.github.acat2.services.SOMTask;
import com.github.acat2.services.logger.LogView;
import com.github.acat2.services.logger.Logger;
import com.github.acat2.services.nlp.Doc2Vec;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MainController implements Initializable {

    @FXML private Button buttonAnalyze;
    @FXML private Button buttonCancelTask;
    @FXML private CheckBox checkBoxPVHS;
    @FXML private HBox hboxGridContainer;
    @FXML private Label labelFolderName;
    @FXML private Label labelProjectName;
    @FXML private Label labelProjectLocation;
    @FXML private Label labelLogLocation;
    @FXML private Label labelLOC;
    @FXML private Label labelClassNum;
    @FXML private Label labelPackageNum;
    @FXML private Label labelStatusText;
    @FXML private Label labelTimelapse;
    @FXML private Label labelSelectionCount;
    @FXML private Label labelClassInitial;
    @FXML private Label labelClassName;
    @FXML private Label labelClassType;
    @FXML private Label labelClassPackage;
    @FXML private Label labelClassLOC;
    @FXML private Label labelClassACE;
    @FXML private Label labelLastAnlaysis;
    @FXML private ListView<String> listViewClassRP;
    @FXML private Menu menuView;
    @FXML private ProgressBar progressBar;
    @FXML private ScrollPane scrollPaneLog;
    @FXML private SplitPane splitPaneMain;
    @FXML private TabPane tabPaneInfo;
    @FXML private TabPane tabPaneMain;
    @FXML private Tab tabProjectOverview;
    @FXML private Tab tabResult;
    @FXML private Tab tabVisualization;
    @FXML private Tab tabLogs;
    @FXML private Tab tabClassInfo;
    @FXML private Tab tabHelp;
    @FXML private ToolBar toolbar;
    @FXML private TableView<Entity> tableViewResult;
    @FXML private TableColumn<Entity, String> tableColumnEntity;
    @FXML private TableColumn<Entity, Number> tableColumnACE;
    @FXML private TableColumn<Entity, Number> tableColumnNo;
    @FXML private TextField textFieldACWeight;
    @FXML private TextField textFieldPVLayer;
    @FXML private TextField textFieldPVWindow;
    @FXML private TextField textFieldPVIter;
    @FXML private TextField textFieldPVAlpha;
    @FXML private TextField textFieldPVNS;
    @FXML private TextField textFieldPVSample;
    @FXML private TreeView<Entity> treeClassBrowser;


    private Stage primaryStage;
    // param related
    private final IntegerProperty paramsValidity = new SimpleIntegerProperty( 0b1111111 );
    // log related
    private Path logLocation;
    // alert related
    private Alert alert;
    // status bar related
    private AnimationTimer stopwatch;
    private final LongProperty timeMillis = new SimpleLongProperty();
    private Task<?> activeTask;
    // scan folder related
    private java.io.File inputFile;
    private boolean isOpen;
    private final ObservableList<CheckBoxTreeItem<Entity>> javaClassItemList = FXCollections.observableArrayList(); // digunakan sebagai model dari 'project borwser'
    private final ObservableList<Entity> selectedEntities = FXCollections.observableArrayList(); // digunakan sebagai model dari 'tabel result' + perhitungan ACE
    private final IntegerProperty selectionCount = new SimpleIntegerProperty();
    // analyze related
    private final List<String> stopwords = Arrays.asList(
            "i", "me", "my", "myself", "we", "our", "ours", "ourselves",
            "you", "you're", "you've", "you'll", "you'd", "your",
            "yours", "yourself", "yourselves", "he", "him", "his",
            "himself", "she", "she's", "her", "hers", "herself", "it",
            "it's", "its", "itself", "they", "them", "their", "theirs",
            "themselves", "what", "which", "who", "whom", "this", "that",
            "that'll", "these", "those", "am", "is", "are", "was", "were",
            "be", "been", "being", "have", "has", "had", "having", "do",
            "does", "did", "doing", "a", "an", "the", "and", "but", "if",
            "or", "because", "as", "until", "while", "of", "at", "by",
            "for", "with", "about", "against", "between", "into", "through",
            "during", "before", "after", "above", "below", "to", "from",
            "up", "down", "in", "out", "on", "off", "over", "under",
            "again", "further", "then", "once", "here", "there", "when",
            "where", "why", "how", "all", "any", "both", "each", "few",
            "more", "most", "other", "some", "such", "no", "nor", "not",
            "only", "own", "same", "so", "than", "too", "very", "s", "t",
            "can", "will", "just", "don", "don't", "should", "should've",
            "now", "d", "ll", "m", "o", "re", "ve", "y", "ain", "aren",
            "aren't", "couldn", "couldn't", "didn", "didn't", "doesn",
            "doesn't", "hadn", "hadn't", "hasn", "hasn't", "haven",
            "haven't", "isn", "isn't", "ma", "mightn", "mightn't", "mustn",
            "mustn't", "needn", "needn't", "shan", "shan't", "shouldn",
            "shouldn't", "wasn", "wasn't", "weren", "weren't", "won",
            "won't", "wouldn", "wouldn't"
    );
    // visualization related
    private Pane hexGrid;
    private Hexagon[][] hexagons; // for easy referencing
    private Map<String, Label> labels = new HashMap<>(); // store UI labels
    boolean isVisualized;
    // class details related
    private final ObservableList<String> rp = FXCollections.observableArrayList();


    @Override public void initialize( URL location, ResourceBundle resources ) {

        prepareLog();
        prepareAlert();
        prepareStopwatch();
        prepareTextFields();
        prepareHexGrid();
        prepareTreeClassBrowser();
        prepareTableViewResult();
        prepareClassInfo();

        // bind label
        labelSelectionCount.textProperty().bind( selectionCount.asString() );

        // tambahkan event listener ke tab
        tabVisualization.selectedProperty().addListener( observable -> {

            if ( !isVisualized ) {

                // hapus label dari hexgrid
                hexGrid.getChildren().removeAll( labels.values() );

                // buat ulang label
                createLabel();

                // mulai SOM
                startSOM();

                isVisualized = true;

            }

        });

    }


    // #LOGS RELATED
    private void prepareLog() {

        try {

            logLocation = Files.createTempDirectory( "acat_log-" );
            LogView logView = new LogView();
            scrollPaneLog.setContent( logView );

        } catch ( IOException ex ) {

            String message = "The system cannot create temporary directory.\n" + "Cause: " + ex.getMessage();
            Logger.error( message );
            showAlert( Alert.AlertType.ERROR, "Error", message );

        }

    }


    // #ALERT RELATED
    private void prepareAlert() {

        // create an instance of alert
        alert = new Alert( Alert.AlertType.NONE );
        alert.setHeaderText( null );
        alert.initStyle( StageStyle.UNDECORATED );

    }
    private boolean showAlert( Alert.AlertType type, String title, String content ) {

        alert.setAlertType(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
        return alert.getResult().getButtonData().isCancelButton();

    }


    // #PARAM RELATED
    private void prepareTextFields() {

        // tambahkan tooltip
        Tooltip integerTooltip = new Tooltip( "must be positive integer value" );
        Tooltip doubleTooltip = new Tooltip( "must be positive double value" );
        Tooltip rangeTooltip = new Tooltip( "must be positive double value between 0 and 1" );

        integerTooltip.setShowDelay( Duration.ZERO );
        doubleTooltip.setShowDelay( Duration.ZERO );
        rangeTooltip.setShowDelay( Duration.ZERO );

        textFieldPVLayer.setTooltip( integerTooltip );
        textFieldPVIter.setTooltip( integerTooltip );
        textFieldPVWindow.setTooltip( integerTooltip );
        textFieldPVNS.setTooltip( integerTooltip );
        textFieldPVSample.setTooltip( doubleTooltip );
        textFieldPVAlpha.setTooltip( doubleTooltip );
        textFieldACWeight.setTooltip( rangeTooltip );

        // pasang listener untuk validasi textfield ketika out of focus
        attachListener( textFieldPVLayer, 0, false );
        attachListener( textFieldPVIter,  1, false );
        attachListener( textFieldPVWindow,2, false );
        attachListener( textFieldPVSample,3, true );
        attachListener( textFieldACWeight,4, true );
        attachListener( textFieldPVAlpha, 5, true );
        attachListener( textFieldPVNS,    6, false );

        // set nilai awal dari textfields
        textFieldACWeight.setText( "0.5" );
        textFieldPVLayer.setText( "300" );
        textFieldPVIter.setText( "100" );
        textFieldPVWindow.setText( "5" );
        textFieldPVAlpha.setText( "0.05" );
        textFieldPVNS.setText( "20" );
        textFieldPVSample.setText( "1e-5" );
        checkBoxPVHS.setSelected( true );

        // disable tombol analyze jika ada textfield yang tidak valid
        buttonAnalyze.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () ->  paramsValidity.get() < 127,
                        paramsValidity
                )
        );

    }
    private void attachListener( TextField target, int position, boolean isTypeOfDouble ) {

        target.focusedProperty().addListener( observable -> {

            // validasi input setelah out of focus
            if ( !target.isFocused() ) {

                int setTrueBitmask = 0b1 << position;
                int setFalseBitmask = ~setTrueBitmask;

                try {

                    if ( isTypeOfDouble )
                        Double.parseDouble( target.getText() );
                    else
                        Integer.parseInt( target.getText() );

                    target.setStyle( null );
                    paramsValidity.set( paramsValidity.get() | setTrueBitmask ); // set true

                } catch ( NumberFormatException ex ) {

                    target.setStyle( "-fx-effect: dropshadow( three-pass-box, red , 5.0, 0.0,0.0,0.0 );" );
                    paramsValidity.set( paramsValidity.get() & setFalseBitmask ); // set false

                }

            }

        });

    }


    // #TASK RELATED
    private void prepareStopwatch() {

        stopwatch = new AnimationTimer() {

            private static final long STOPPED = -1 ;
            private long startTime = STOPPED ;

            @Override
            public void handle( long timestamp ) {

                if (startTime == STOPPED)
                    startTime = timestamp;

                long nanosecond = timestamp - startTime ;
                long millisecond = nanosecond / 1_000_000 ;
                timeMillis.set( millisecond );

            }

            @Override
            public void stop() {
                startTime = STOPPED;
                super.stop();
            }

        };

        labelTimelapse.textProperty().bind( Bindings.createStringBinding(
                () -> {
                    long ms = timeMillis.getValue();
                    long s = ms / 1000;
                    long m = s / 60;
                    return String.format( "%02dm %02ds %03dms", m, s % 60, ms % 1000 );
                },
                timeMillis
        ));

    }
    @FXML public void cancelTask() {

        Logger.info( "task cancelled" );
        activeTask.cancel();

    }
    private void startTask( String taskName ) {

        activeTask.setOnFailed(  event -> {
            endProgress();
            Logger.error( taskName + " task failed" );
            labelStatusText.setText( taskName + " task failed" );
        });

        activeTask.setOnCancelled(  event -> {
            endProgress();
            Logger.info( taskName + " task cancelled" );
            labelStatusText.setText( taskName + " task cancelled" );
        });

        // bind to the status bar and start the timer
        startProgress( activeTask.progressProperty(), activeTask.messageProperty() );
        Logger.info( "starting " + taskName + " task" );

        // create and run the task on a new thread
        Thread thread = new Thread( activeTask );
        thread.setDaemon( true );
        thread.start();

    }
    private void startProgress( ReadOnlyDoubleProperty progressProperty, ReadOnlyStringProperty messageProperty ) {

        // release binding
        endProgress();

        // bind
        progressBar.progressProperty().bind( progressProperty );
        labelStatusText.textProperty().bind( messageProperty );
        buttonCancelTask.setVisible( true );
        stopwatch.start();

    }
    private void endProgress() {

        progressBar.progressProperty().unbind();
        progressBar.setProgress( 0 );
        labelStatusText.textProperty().unbind();
        buttonCancelTask.setVisible( false );
        stopwatch.stop();

    }
    private void setActiveTask( Task<?> task ) {
        activeTask = task;
    }


    // #SCAN RELATED
    private void prepareTreeClassBrowser() {

        treeClassBrowser.setCellFactory( param -> new CheckBoxTreeCell<>() {

            @Override public void updateItem ( Entity item, boolean empty ) {

                super.updateItem( item, empty );

                if ( empty || item == null ) {

                    setStyle( null );
                    setDisable( false );

                } else if ( !item.getFileType().equals( FileType.JAVA_CLASS ) && !item.isHasJavaDescendant() ) {

                    setStyle( "-fx-text-fill: #adadad;" );
                    setDisable( true );

                } else {

                    setStyle( null );
                    setDisable( false );

                }

            }

        });

        treeClassBrowser.setShowRoot( false );

    }
    @FXML public void dragOver( DragEvent event ) {

        if ( event.getDragboard().getFiles().get( 0 ).isDirectory() )

            event.acceptTransferModes( TransferMode.ANY );

        else

            event.acceptTransferModes( TransferMode.NONE );

    }
    @FXML public void dragDropped( DragEvent event ) {

        if ( event.isAccepted() )

            scanFolder( event.getDragboard().getFiles().get( 0 ) );

        event.setDropCompleted( true );

    }
    @FXML public void chooseFolder() {

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle( "Select a Java project folder" );

        java.io.File folderInput = chooser.showDialog( primaryStage );

        if ( folderInput != null )

            scanFolder( folderInput );

    }
    private void scanFolder( File folderInput ) {

        // Validasi 1: jika sudah ada folder yang dibuka,
        // minta persetujuan user untuk mengganti dengan folder yang baru
        if ( isOpen ) {

            if ( showAlert(
                    Alert.AlertType.CONFIRMATION,
                    "Confirmation",
                    "There is a folder already opened. Do you want to replace with the selected one?"
            ) )
                return;

        }

        activeTask = new Task<FileVisitor>() {

            @Override protected FileVisitor call() throws Exception {

                // mulai scan folder
                FileVisitor visitor = new FileVisitor();

                // bind status dari file visitor
                visitor.messageProperty().addListener( (o, l, n) -> updateMessage( n ) );
                visitor.isCancelledProperty().bind( new ReadOnlyBooleanWrapper( isCancelled() ) );

                Files.walkFileTree( folderInput.toPath(), visitor );

                updateMessage( "scan folder finished" );

                return visitor;

            }

        };

        activeTask.setOnSucceeded( event -> scanTaskOnSuccess( folderInput ) );

        startTask( "scan folder" );

    }
    private void scanTaskOnSuccess( File folderInput ) {

        Logger.info( "scan folder finished" );
        endProgress();

        FileVisitor visitor = ( FileVisitor ) activeTask.getValue();

        // bersihkan daftar dari folder sebelumnya
        javaClassItemList.clear();

        // tambahkan daftar tree item java file dari visitor
        javaClassItemList.addAll( visitor.getJavaClassItemList() );

        // assign selection listener ke setiap checkbox item
        for ( CheckBoxTreeItem<Entity> item : javaClassItemList )

            if ( item.isLeaf() )

                attachSelectionListener( item );

            else

                item.getChildren().forEach(
                        childItem -> attachSelectionListener( ( ( CheckBoxTreeItem<Entity> ) childItem ) )
                );


        // Validasi 2: harus mengandung file java minimal 2
        if ( javaClassItemList.size() < 2 ) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Warning",
                    "The folder contain less than two java class. Please choose different folder."
            );

        } else {

            // set state jadi opened
            isOpen = true;

            // simpan file input ke dalam variabel
            inputFile =  folderInput;

            // tampilkan hasil scan ke tree view
            showScanResult( visitor.getRoot() );

            loadTabProjectOverview();

        }

    }
    private void attachSelectionListener( CheckBoxTreeItem<Entity> target ) {

        target.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {

            Entity entity = target.getValue();

            // jika checkbox item dicentang
            if ( newValue ) {

                // increment count
                selectionCount.set( selectionCount.get() + 1 );

                // tambahkan ke list
                if ( !selectedEntities.contains( entity ) )

                    selectedEntities.add( entity );

            // jika checkbox tidak dicentang
            } else {

                // decrement count
                selectionCount.set( selectionCount.get() - 1 );

                // hapus dari list
                selectedEntities.remove( entity );

            }

        });

    }
    private void showScanResult( TreeItem<Entity> rootItem ) {

        Platform.runLater( () -> treeClassBrowser.setRoot( rootItem ) );

        showLeftPane();

    }
    private void loadTabProjectOverview() {

        tabPaneMain.setVisible( true );
        toolbar.setDisable( false );

        String projectName = inputFile.getName().toLowerCase().replaceAll( "[^\\d\\w.]", " " );
        String capitalizeFirstLetter =  projectName.substring( 0, 1 ).toUpperCase() + projectName.substring( 1 );
        int numberOfLOC = 0;
        int numberOfClasses = 0;
        Set<String> numberOfPackages = new HashSet<>();

        for ( CheckBoxTreeItem<Entity> treeItem : javaClassItemList) {

            if ( treeItem.isLeaf() ) {

                numberOfLOC += treeItem.getValue().getLoc();
                numberOfPackages.add(treeItem.getValue().getPackageName());
                numberOfClasses++;

            } else

                for ( TreeItem<Entity> childItem : treeItem.getChildren() ) {

                    numberOfLOC += childItem.getValue().getLoc();
                    numberOfPackages.add( childItem.getValue().getPackageName() );
                    numberOfClasses++;

                }

        }

        labelFolderName.setText( capitalizeFirstLetter );
        labelProjectName.setText( labelFolderName.getText() );
        labelProjectLocation.setText( inputFile.getPath() );
        labelLogLocation.setText( logLocation.toString() );
        labelLOC.setText( "" + numberOfLOC );
        labelPackageNum.setText( "" + ( numberOfPackages.size() + 1 ) );
        labelClassNum.setText( "" + numberOfClasses );

    }


    // #ANALYZE RELATED
    private void prepareTableViewResult() {

        tableColumnNo.setCellFactory( param -> new TableCell<>() {
            @Override
            protected void updateItem( Number item, boolean empty ) {
                super.updateItem( item, empty );
                if ( !empty && getTableRow() != null )
                    setText( ( getTableRow().getIndex() + 1 ) + "" );
                else
                    setText( null );
            }
        });

        // set cell value dan cel factory dari table result
        tableColumnEntity.setCellValueFactory(
                tableColumn -> new ReadOnlyStringWrapper(
                        tableColumn.getValue() + " (" + tableColumn.getValue().getInitial() + ")"
                )
        );
        tableColumnACE.setCellValueFactory( tableColumn -> new ReadOnlyDoubleWrapper( tableColumn.getValue().getAce() ) );
        tableColumnACE.setCellFactory(tableCell -> getTableCellFormatDouble() );

        // ketika row tabel result di klik, maka tampilkan class info
        tableViewResult.setRowFactory( tableView -> {

            final TableRow<Entity> row = new TableRow<>();

            row.setOnMouseClicked( event -> {

                final Entity entity = row.getItem();

                if ( entity != null )

                    showClassInfo( entity );

            });

            return row;

        });

    }
    private TableCell<Entity, Number> getTableCellFormatDouble(){

        // format tableCell dengan tiga angka dibelakang koma
        DecimalFormat decimalFormat = new DecimalFormat( "0.000" );

        return new TableCell<>() {

            @Override protected void updateItem( Number item, boolean empty ) {

                super.updateItem( item, empty );

                if ( empty )

                    setText( null );

                else

                    setText( decimalFormat.format( item ) );

            }

        };

    }
    @FXML public void startAnalyze() {

        Logger.info( "request for analyze coupling" );

        // 1. daftar entitas yang terseleksi sudah otomatis berubah menyesuaikan dengan javaFileTreeItem

        // 2. Verifikasi 1: jika user memilih kurang dari 2 entitas
        if ( selectedEntities.size() < 2 ) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Warning",
                    "You have to select 2 or more classes in order to do aggregate coupling analysis."
            );

            return;

        }


        // 3. validasi 2: jika input textField tidak valid, sudah menggunakan listener

        // 4. get relevant properties, wes neng file visitor

        activeTask = new Task<>() {

            @Override
            protected Void call() throws IOException {

                // 5. preproses
                Logger.info( "create and clean corpus" );
                createAndCleanCorpus();

                //params
                final float paramWeight = Float.parseFloat( textFieldACWeight.getText() );
                final float paramSubsample = Float.parseFloat( textFieldPVSample.getText() );
                final float paramAlpha = Float.parseFloat( textFieldACWeight.getText() );
                final int paramLayer = Integer.parseInt( textFieldPVLayer.getText() );
                final int paramNS = Integer.parseInt( textFieldPVNS.getText() );
                final int paramWindow = Integer.parseInt( textFieldPVWindow.getText() );
                final int paramIter = Integer.parseInt( textFieldPVIter.getText() );
                final boolean paramHS = checkBoxPVHS.isSelected();

                Doc2Vec pv = new Doc2Vec(
                        logLocation + "/corpus.txt",
//                        "/media/share/data/kuliah_s1/Semester_8/skripsi/leap3/samples/acat_6383532752563486308/corpus.txt",
                        logLocation.toString(),
                        paramLayer,
                        paramWindow,
                        paramIter,
                        paramAlpha,
                        paramSubsample,
                        paramNS,
                        paramHS
                );
                Logger.info( "start train doc2vec" );
                pv.isCancelledProperty().bind( new ReadOnlyBooleanWrapper( isCancelled() ) );
//                pv.messageProperty().addListener( (o, l, n) -> updateMessage( n ) );
                updateMessage("training doc2vec using " + logLocation + "/corpus.txt" );
                pv.TrainModel();
                updateMessage("doc2vec finished");
                double[][] docvecs = pv.getVector();

                // calc sc, cc, ac, ace
                ACE ace = new ACE( selectedEntities, docvecs, paramWeight, logLocation.toString() );
                ace.isCancelledProperty().bind( new ReadOnlyBooleanWrapper( isCancelled() ) );
                updateMessage( "calculating aggregated coupling metrics" );
                ace.run();

                updateMessage( "analyze finished" );

                return null;

            }

        };

        activeTask.setOnSucceeded( event -> {

            isVisualized = false;

            endProgress();
            Logger.info( "analyze finished" );

            labelLastAnlaysis.setText( "Last analysis at " +
                    DateTimeFormatter.ofPattern("MMMM dd HH:mm:ss").format( LocalDateTime.now() ) );

            menuView.getItems().forEach( menuItem -> menuItem.setDisable( false ) );
            tabResult.setDisable( false );
            tabVisualization.setDisable( false );
            loadTabResult( );

        });

        startTask( "analyze" );

    }
    private void createAndCleanCorpus() throws IOException {

        String corpus = selectedEntities
                .stream()
                .map( entity -> entity.getSourceCode().replaceAll( "[\n\r]", " " ) )
                //--clean symbols and number--
                .map( s -> s
                        .toLowerCase()
                        .replaceAll( "(<[^>\\[\\],]*>)([^</]*)(</[^>\\[\\],]*>)", "$2" ) // hapus html tag
                        .replaceAll( "@\\w+", " " ) // hapus java annotation
                        .replaceAll( "[^\\w ]", " " ) // hapus selain huruf
                        .replaceAll( "\\d", " " ) // hapus angka
                        .replaceAll( "_", "" ) // hapus underscore
                        .replaceAll( "\\b\\w\\b", "" ) // hapus huruf tunggal
                        .replaceAll( "\\s+", " " ) // ringkas spasi
                        .trim()
                )
                //--clean stopwords--
                .map( s -> Arrays.stream( s.split( " +" ) )
                        .filter( token -> !stopwords.contains( token ) )
                        .collect( Collectors.joining( " " ) ) )
                .collect( Collectors.joining( "\n" ) ) + "\n";

        Logger.info( "write corpus to file" );
        Files.write( Paths.get( logLocation + "/corpus.txt" ), corpus.getBytes() );

    }
    private void loadTabResult( ) {

        // gunakan model ini
        ObservableList<Entity> tableItems = FXCollections.observableList( selectedEntities );

        // hapus dulu kolom sc, cc, ace
        tableViewResult.getColumns().remove( 3, tableViewResult.getColumns().size() );

        //buat lagi kolom SC,CC,AC setiap entitas yang terseleksi
        for ( int index = 0; index < tableItems.size(); index++ ) {

            TableColumn<Entity, Number> tableColumnSC = new TableColumn<>( "SC" );
            TableColumn<Entity, Number> tableColumnCC = new TableColumn<>( "CC" );
            TableColumn<Entity, Number> tableColumnAC = new TableColumn<>( "AC" );

            int finalIndex = index;
            tableColumnSC.setCellValueFactory(
                    tableItem -> new ReadOnlyDoubleWrapper( tableItem.getValue().getSc().get( finalIndex ) ) );
            tableColumnAC.setCellValueFactory(
                    tableItem -> new ReadOnlyDoubleWrapper( tableItem.getValue().getCc().get( finalIndex ) ) );
            tableColumnCC.setCellValueFactory(
                    tableItem -> new ReadOnlyDoubleWrapper( tableItem.getValue().getAc().get( finalIndex ) ) );

            tableColumnSC.setCellFactory( tableCell -> getTableCellFormatDouble() );
            tableColumnAC.setCellFactory( tableCell -> getTableCellFormatDouble() );
            tableColumnCC.setCellFactory( tableCell -> getTableCellFormatDouble() );

            TableColumn<Entity, String> tableColumnGroup = new TableColumn<>();
            tableColumnGroup.setText( tableItems.get( index ).getInitial() );
            tableColumnGroup.getColumns().add( tableColumnSC );
            tableColumnGroup.getColumns().add( tableColumnCC );
            tableColumnGroup.getColumns().add( tableColumnAC );

            tableColumnSC.setSortable( false );
            tableColumnCC.setSortable( false );
            tableColumnAC.setSortable( false );
            tableColumnGroup.setSortable( false );

            tableViewResult.getColumns().add( tableColumnGroup );

        }

        Platform.runLater( () -> tableViewResult.setItems( tableItems ) );

        // buka tab Result
        tabPaneMain.getSelectionModel().select( tabResult );

    }


    // #VISUALIZATION RELATED
    private void prepareHexGrid() {

        int cols = 50;
        int rows = 50;
        double totalWidth = 700;

        // initialize
        hexGrid = new Pane( );  // hexagonal grid container
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
        hboxGridContainer.getChildren().add( hexGrid );

    }
    private void createLabel() {

        // membuat label dari item tableview dan menambahkannya ke hexgrid
        labels = tableViewResult.getItems().stream()
                .collect( Collectors.toMap(
                        Entity::getInitial,
                        entity -> {
                            Label label = new Label( entity.getInitial() );
                            label.setOnMouseClicked( event -> showClassInfo( entity ) );
                            return label;
                        }
                ));

        // ubah text label jadi simbol x jika label lebih dari dimensi hexgrid
        if ( labels.size() > hexagons.length )
            labels.values().forEach( label -> label.setText( "X" ) );

        hexGrid.getChildren().addAll( labels.values() );

        Map<String, List<String>> packageToName = new HashMap<>();
        tableViewResult.getItems().forEach( entity -> {
            String packageName = entity.getPackageName();
            List<String> names = packageToName.computeIfAbsent( packageName, k -> new ArrayList<>());
            names.add( entity.getInitial() );
        });

        // kalkulasi dan pemberian warna pada label
        Random rand = new Random( 0 );
        int step =  360 / packageToName.keySet().size();
        Iterator<List<String>> values = packageToName.values().iterator();
        for( int hue = 0; hue <= 360 && values.hasNext(); hue += step ) {

            double saturation = (90 + rand.nextDouble() * 10) / 100;
            double brightness = (90 + rand.nextDouble() * 10) / 100;
            Color color = Color.hsb( hue, saturation, brightness );
            values.next().forEach(
                name -> labels.get( name ).setStyle( String.format(
                        "-fx-text-fill: #%02x%02x%02x;",
                        (int)( color.getRed() * 255 ),
                        (int)( color.getGreen() * 255 ),
                        (int)( color.getBlue() * 255 )
                )
            ));

        }

    }
    private void startSOM() {

        final AtomicInteger readyState = new AtomicInteger( -1 );
        SOMTask somTask = new SOMTask( extractAC(), extractTag(), 50);
        somTask.setOnSucceeded( event -> endProgress() );
        somTask.intervalProperty().addListener( ( o, l, n ) -> {

            if ( readyState.getAndSet( n.intValue() ) == -1 )

                Platform.runLater( () -> {

                    drawUMatrix( somTask.getUMatrix() );
                    drawLabel( somTask.getBMU() );
                    readyState.set( -1 );

                });

        });

        setActiveTask( somTask );

        startTask( "SOM" );

    }
    private double[][] extractAC() {

        int n = selectedEntities.size();
        double[][] ac = new double[n][n];

        for ( int i = 0; i < n; i++ )

            for ( int j = 0; j < n; j++ ) {

                ac[i][j] = selectedEntities.get( i ).getAc().get( j );
            }

        return ac;

    }
    private String[] extractTag() {

        String[] tags = new String[ selectedEntities.size() ];

        for ( int i = 0; i < selectedEntities.size(); i++ ) {

            tags[i] = selectedEntities.get( i ).getInitial();
        }
        return tags;

    }
    private void drawLabel( Map<String, int[]> labelBMU ) {

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


    // #CLASS INFO RELATED
    private void prepareClassInfo() {

        final Image methodIcon = new Image( "/images/icon-method.png" );
        final Image fieldIcon = new Image( "/images/icon-field.png" );
        final Image classIcon = new Image( "/images/icon-java-class.png" );

        listViewClassRP.setItems( rp );
        listViewClassRP.setCellFactory( param -> new ListCell<>(){

            @Override
            public void updateItem( String item, boolean empty ) {

                super.updateItem(item, empty);

                if ( item == null ) {

                    setGraphic( null );
                    setText( null );

                } else {

                    ImageView iconImage = new ImageView();
                    iconImage.setFitWidth( 20 );
                    iconImage.setFitHeight( 20 );
                    String itemName;

                    // jika var
                    if ( item.startsWith( "var_" ) ) {
                        iconImage.setImage( fieldIcon );
                        itemName = item.substring( 4 );
                        // jika method
                    } else if ( item.endsWith("()") ) {
                        iconImage.setImage( methodIcon );
                        itemName = item.replace( "()", "" );
                    } else {
                        iconImage.setImage( classIcon );
                        itemName = item;
                    }

                    setGraphic( iconImage );
                    setText( itemName );

                }

            }

        });

    }
    private void showClassInfo( Entity entity ) {

        labelClassName.setText( entity.toString() );
        labelClassInitial.setText( entity.getInitial() );
        labelClassType.setText( entity.getClassType() );
        labelClassPackage.setText( entity.getPackageName() );
        labelClassLOC.setText( entity.getLoc() + "" );
        labelClassACE.setText( entity.getAce() + "" );
        listViewClassRP.getItems( ).clear( );
        listViewClassRP.getItems( ).addAll( entity.getRp()  );

        showRightPane();
        showTabClassInfo();

    }


    // #MENUVIEW RELATED
    @FXML public void exitApp() {

        primaryStage.fireEvent(
                new WindowEvent(
                        primaryStage,
                        WindowEvent.WINDOW_CLOSE_REQUEST
                )
        );

    }
    @FXML public void showTabProjectOverview() {

        tabPaneMain.getSelectionModel().select( tabProjectOverview );

    }
    @FXML public void showTabResult() {

        tabPaneMain.getSelectionModel().select( tabResult );

    }
    @FXML public void showTabVisualization() {

        tabPaneMain.getSelectionModel().select( tabVisualization );

    }
    @FXML public void showTabLog() {

        tabPaneMain.getSelectionModel().select( tabLogs );

    }
    @FXML public void showTabClassInfo() {

        tabPaneInfo.getSelectionModel().select( tabClassInfo );

    }
    @FXML public void showTabHelp() {

        showRightPane();
        tabPaneInfo.getSelectionModel().select( tabHelp );

    }
    @FXML public void toggleLeftPane() {

        double pos = splitPaneMain.getDividerPositions()[0];
        if ( pos > 0.05 )
            hideLeftPane();
        else
            showLeftPane();

    }
    @FXML public void toggleRightPane() {
        double pos = splitPaneMain.getDividerPositions()[1];

        if ( pos < 0.9 )
            hideRightPane();
        else
            showRightPane();

    }
    private void showLeftPane() {
        splitPaneMain.setDividerPosition( 0, 0.25 );
    }
    private void hideLeftPane() {
        splitPaneMain.setDividerPosition( 0, 0 );
    }
    private void showRightPane() {
        splitPaneMain.setDividerPosition( 1, 0.75 );
    }
    private void hideRightPane() {
        splitPaneMain.setDividerPosition( 1, 1 );
    }


    public void setStage( Stage stage ) {

        primaryStage = stage;

    }


    public void dispose() {

        // delete temporary folder recursively on exit
        try {

            Files.walk( logLocation )
                    .sorted( Comparator.reverseOrder() )
                    .map( Path::toFile)
                    .forEach( java.io.File::delete );

        } catch ( IOException ex ) {

            Logger.error( ex.getMessage() );

        }

    }

}
