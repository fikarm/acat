package acat.services.logger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Duration;

import java.text.SimpleDateFormat;

public class LogView extends ListView<LogRecord> {

    private static final int MAX_ENTRIES = 10_000;
    private final static PseudoClass debug = PseudoClass.getPseudoClass( "debug" );
    private final static PseudoClass info  = PseudoClass.getPseudoClass( "info" );
    private final static PseudoClass warn  = PseudoClass.getPseudoClass( "warn" );
    private final static PseudoClass error = PseudoClass.getPseudoClass( "error" );
    private final static SimpleDateFormat timestampFormatter = new SimpleDateFormat( "HH:mm:ss.SSS" );
    private final ObservableList<LogRecord> logItems = FXCollections.observableArrayList();

    public LogView() {

        getStyleClass().add( "log-view" );

        Timeline logTransfer = new Timeline(
                new KeyFrame(
                        Duration.seconds( 1 ),
                        event -> {
                            Logger.getLog().drainTo( logItems );

                            if ( logItems.size() > MAX_ENTRIES )
                                logItems.remove( 0, logItems.size() - MAX_ENTRIES );

                            //scrollTo( logItems.size() ); // uncomment untuk tail
                        }
                )
        );

        logTransfer.setCycleCount( Timeline.INDEFINITE );
        logTransfer.setRate( 60 );
        logTransfer.play();
        setItems( logItems );
        setCellFactory( param -> new ListCell<>() {

            @Override
            protected void updateItem( LogRecord item, boolean empty ) {

                super.updateItem(item, empty);

                pseudoClassStateChanged( debug, false );
                pseudoClassStateChanged( info, false );
                pseudoClassStateChanged( warn, false );
                pseudoClassStateChanged( error, false );

                if ( item == null || empty ) {
                    setText(null);
                    return;
                }

                setText( (item.getTimestamp() == null) ?
                        "" :
                        timestampFormatter.format( item.getTimestamp() ) + " "
                        + item.getLevel().name() + " "
                        + item.getMessage()
                );

                switch (item.getLevel()) {
                    case DEBUG: pseudoClassStateChanged(debug, true); break;
                    case INFO: pseudoClassStateChanged(info, true); break;
                    case WARN: pseudoClassStateChanged(warn, true); break;
                    case ERROR: pseudoClassStateChanged(error, true); break;
                }
            }
        });

    }

}
